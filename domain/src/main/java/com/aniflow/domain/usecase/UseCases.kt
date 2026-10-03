package com.aniflow.domain.usecase

import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.domain.command.DownloadCommand
import com.aniflow.domain.identity.DownloadPlanId
import com.aniflow.domain.identity.DownloadTaskId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.model.aggregate.download.DownloadPlan
import com.aniflow.domain.model.aggregate.download.DownloadPlanValidation
import com.aniflow.domain.model.aggregate.download.DownloadSelection
import com.aniflow.domain.model.aggregate.download.DownloadTask
import com.aniflow.domain.model.aggregate.download.GroupingMode
import com.aniflow.domain.model.aggregate.media.Anime
import com.aniflow.domain.model.aggregate.media.Episode
import com.aniflow.domain.model.aggregate.media.Season
import com.aniflow.domain.model.aggregate.organization.DownloadProfile
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.repository.DownloadRepository
import com.aniflow.domain.repository.ProfileRepository
import com.aniflow.domain.repository.ReleaseRepository
import com.aniflow.domain.repository.RuleRepository
import com.aniflow.domain.service.CoverageComparisonService
import com.aniflow.domain.service.DownloadPlanningService
import com.aniflow.domain.service.DuplicateDetectionService
import com.aniflow.domain.service.EpisodeCoverageService
import com.aniflow.domain.service.FileNamingService
import com.aniflow.domain.service.GroupingResult
import com.aniflow.domain.service.ReleaseGroupingService
import com.aniflow.domain.service.ReleaseNormalizationService
import com.aniflow.domain.service.SelectionEvaluationResult
import com.aniflow.domain.service.SelectionService
import com.aniflow.domain.state.DownloadState
import com.aniflow.domain.state.DownloadStateMachine
import com.aniflow.domain.valueobject.CoverageComparison
import com.aniflow.domain.valueobject.DuplicateMatch
import com.aniflow.domain.valueobject.EpisodeCoverage
import com.aniflow.domain.valueobject.EpisodeNumber
import com.aniflow.domain.valueobject.PageResult
import com.aniflow.domain.valueobject.SearchQuery
import com.aniflow.domain.valueobject.StorageTarget
import kotlinx.coroutines.flow.Flow


/**
 * UseCase: Groups discovered releases according to the specified grouping strategy.
 */
class GroupReleasesUseCase {
    operator fun invoke(
        releases: List<Release>,
        mode: GroupingMode = GroupingMode.AnimeSeason
    ): GroupingResult = ReleaseGroupingService.group(releases, mode)
}

/**
 * UseCase: Computes coverage of expected vs available and downloaded episodes.
 */
class CalculateCoverageUseCase {
    operator fun invoke(
        expectedEpisodes: Set<EpisodeNumber>,
        availableReleases: List<Release>,
        downloadedEpisodes: Set<EpisodeNumber> = emptySet(),
        queuedEpisodes: Set<EpisodeNumber> = emptySet(),
        downloadingEpisodes: Set<EpisodeNumber> = emptySet(),
        failedEpisodes: Set<EpisodeNumber> = emptySet()
    ): EpisodeCoverage = EpisodeCoverageService.calculateCoverage(
        expectedEpisodes = expectedEpisodes,
        availableReleases = availableReleases,
        downloadedEpisodes = downloadedEpisodes,
        queuedEpisodes = queuedEpisodes,
        downloadingEpisodes = downloadingEpisodes,
        failedEpisodes = failedEpisodes
    )
}

/**
 * UseCase: Compares batch releases against single episodes and existing library items.
 */
class CompareCoverageUseCase {
    operator fun invoke(
        batchRelease: Release,
        individualReleases: List<Release>,
        downloadedEpisodes: Set<EpisodeNumber> = emptySet()
    ): CoverageComparison = CoverageComparisonService.compare(
        batchRelease = batchRelease,
        individualReleases = individualReleases,
        downloadedEpisodes = downloadedEpisodes
    )
}

/**
 * UseCase: Evaluates and scores a release with explainable transparent criteria.
 */
class EvaluateReleaseSelectionUseCase(
    private val profileRepository: ProfileRepository,
    private val ruleRepository: RuleRepository
) {
    suspend operator fun invoke(
        release: Release,
        profileOverride: DownloadProfile? = null,
        isDuplicate: Boolean = false,
        isEpisodeMissing: Boolean = false
    ): SelectionEvaluationResult {
        val profile = profileOverride ?: profileRepository.getDefaultProfile()
        val rules = ruleRepository.getAll()
        return SelectionService.evaluateRelease(
            release = release,
            profile = profile,
            rules = rules,
            isDuplicate = isDuplicate,
            isEpisodeMissing = isEpisodeMissing
        )
    }
}

/**
 * UseCase: Validates and prepares an executable DownloadPlan.
 */
class BuildDownloadPlanUseCase {
    operator fun invoke(
        planId: DownloadPlanId,
        selections: List<DownloadSelection>,
        releasesById: Map<String, Release>,
        existingDuplicates: Map<String, DuplicateMatch> = emptyMap(),
        storageTarget: StorageTarget = StorageTarget.DEFAULT
    ): DownloadPlanValidation = DownloadPlanningService.validatePlan(
        selections = selections,
        releasesById = releasesById,
        existingDuplicates = existingDuplicates,
        storageTarget = storageTarget
    )
}

/**
 * UseCase: Transitions download task state using the strict state machine.
 */
class TransitionDownloadStateUseCase(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(
        taskId: DownloadTaskId,
        targetState: DownloadState
    ): DownloadTask? {
        val currentTask = downloadRepository.getTaskById(taskId) ?: return null
        val nextState = DownloadStateMachine.transition(currentTask.state, targetState)
        val updated = currentTask.copy(state = nextState)
        downloadRepository.saveTask(updated)
        return updated
    }
}

/**
 * UseCase: Checks if a candidate release duplicates any existing library or active items.
 */
class CheckDuplicateReleaseUseCase {
    operator fun invoke(
        candidate: Release,
        existingReleases: List<Release>
    ): DuplicateMatch? = DuplicateDetectionService.checkDuplicate(candidate, existingReleases)
}

/**
 * UseCase: Resolves storage destination paths and file names from templates.
 */
class ResolveStoragePathUseCase {
    operator fun invoke(
        template: String = FileNamingService.DEFAULT_TEMPLATE,
        anime: Anime,
        season: Season?,
        episode: Episode?,
        release: Release? = null
    ): String = FileNamingService.resolvePath(
        template = template,
        anime = anime,
        season = season,
        episode = episode,
        release = release
    )
}
