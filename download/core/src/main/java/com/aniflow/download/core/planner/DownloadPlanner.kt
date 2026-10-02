package com.aniflow.download.core.planner

import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.intelligence.model.NormalizedRelease
import com.aniflow.domain.model.aggregate.download.DownloadPolicy
import com.aniflow.domain.model.aggregate.download.NetworkPolicy
import com.aniflow.domain.selection.model.DownloadProfile
import com.aniflow.domain.selection.model.MultiEpisodeSelectionResult
import com.aniflow.domain.selection.model.SelectionResult
import com.aniflow.domain.valueobject.InfoHash
import com.aniflow.domain.valueobject.UrlValue
import com.aniflow.download.core.model.DownloadEngineSelector
import com.aniflow.download.core.model.DownloadPlan
import com.aniflow.download.core.model.DownloadPlanId
import com.aniflow.download.core.model.DownloadPlanItem
import com.aniflow.download.core.model.DownloadPlanItemId
import com.aniflow.download.core.model.DownloadPolicySnapshot
import com.aniflow.download.core.model.DownloadPriority
import com.aniflow.download.core.model.DownloadSource
import com.aniflow.download.core.model.DownloadTask
import com.aniflow.download.core.model.DuplicateDecision
import com.aniflow.download.core.model.DuplicatePolicy
import com.aniflow.download.core.model.DuplicateResult
import com.aniflow.download.core.model.FilenamePolicy
import com.aniflow.download.core.model.PlanDecision
import com.aniflow.download.core.model.PlanWarning
import com.aniflow.download.core.model.PlannedDestination
import com.aniflow.download.core.model.PlannedFile
import com.aniflow.download.core.model.PlannedFileId
import com.aniflow.download.core.model.RuntimeCapabilities
import com.aniflow.download.core.model.StorageContext
import com.aniflow.download.core.model.StorageEstimate
import com.aniflow.download.core.model.StorageTarget
import com.aniflow.download.core.model.TaskBlockReason
import com.aniflow.download.core.storage.StorageReservationManager
import java.util.UUID

/**
 * Result of download planning evaluating validation, duplicate detection, storage, and policies (Section 150).
 */
sealed interface DownloadPlanResult {
    data class Ready(val plan: DownloadPlan) : DownloadPlanResult
    data class RequiresConfirmation(
        val plan: DownloadPlan,
        val reason: String,
        val warnings: List<PlanWarning>
    ) : DownloadPlanResult
    data class Blocked(val reason: TaskBlockReason, val message: String) : DownloadPlanResult
    data class Duplicate(val duplicateResult: DuplicateResult, val explanation: String) : DownloadPlanResult

    val isReady: Boolean get() = this is Ready
}

/**
 * DownloadPlanner contract adhering to Step 22 Sections 6, 7, 8, 149, 150.
 */
interface IDownloadPlanner {
    suspend fun createPlan(
        selection: SelectionResult,
        profile: DownloadProfile? = null,
        policy: DownloadPolicy = DownloadPolicy(),
        destination: StorageTarget = StorageTarget("default", "Anime", "/storage/emulated/0/Anime"),
        capabilities: RuntimeCapabilities = RuntimeCapabilities(),
        existingTasks: List<DownloadTask> = emptyList(),
        existingPlans: List<DownloadPlan> = emptyList()
    ): DownloadPlanResult

    suspend fun createBatchPlan(
        multiResult: MultiEpisodeSelectionResult,
        profile: DownloadProfile? = null,
        policy: DownloadPolicy = DownloadPolicy(),
        destination: StorageTarget = StorageTarget("default", "Anime", "/storage/emulated/0/Anime"),
        capabilities: RuntimeCapabilities = RuntimeCapabilities(),
        existingTasks: List<DownloadTask> = emptyList(),
        existingPlans: List<DownloadPlan> = emptyList()
    ): DownloadPlanResult
}

/**
 * Transforms SelectionResult into a concrete, validated, and executable DownloadPlan (Section 5, 6, 7, 8, 9, 10).
 * Strictly decoupled from download execution, networking, and queues.
 */
class DownloadPlanner(
    private val duplicateDetector: DuplicateDetector = DuplicateDetector(),
    private val pathSanitizer: PathSanitizer = PathSanitizer(),
    private val engineSelector: DownloadEngineSelector = DownloadEngineSelector(),
    private val storageReservationManager: StorageReservationManager? = null
) : IDownloadPlanner {

    override suspend fun createPlan(
        selection: SelectionResult,
        profile: DownloadProfile?,
        policy: DownloadPolicy,
        destination: StorageTarget,
        capabilities: RuntimeCapabilities,
        existingTasks: List<DownloadTask>,
        existingPlans: List<DownloadPlan>
    ): DownloadPlanResult {
        val candidate = selection.selected
            ?: return DownloadPlanResult.Blocked(
                TaskBlockReason.ProviderUnavailable,
                "No candidate release was selected"
            )

        val selections = mapOf(
            (candidate.release.animeCandidate?.let { EpisodeId("ep-1") } ?: EpisodeId("ep-1")) to selection
        )

        return evaluatePlanInternal(
            selections = selections,
            profile = profile,
            policy = policy,
            destination = destination,
            capabilities = capabilities,
            existingTasks = existingTasks,
            existingPlans = existingPlans
        )
    }

    override suspend fun createBatchPlan(
        multiResult: MultiEpisodeSelectionResult,
        profile: DownloadProfile?,
        policy: DownloadPolicy,
        destination: StorageTarget,
        capabilities: RuntimeCapabilities,
        existingTasks: List<DownloadTask>,
        existingPlans: List<DownloadPlan>
    ): DownloadPlanResult {
        return evaluatePlanInternal(
            selections = multiResult.selections,
            profile = profile,
            policy = policy,
            destination = destination,
            capabilities = capabilities,
            existingTasks = existingTasks,
            existingPlans = existingPlans
        )
    }

    /**
     * Compatibility overload returning plain DownloadPlan for existing callers.
     */
    fun createPlan(
        selections: Map<EpisodeId, SelectionResult>,
        profile: DownloadProfile?,
        policy: DownloadPolicy,
        destination: StorageTarget,
        capabilities: RuntimeCapabilities,
        existingTasks: List<DownloadTask> = emptyList()
    ): DownloadPlan {
        val planId = DownloadPlanId("plan-${UUID.randomUUID()}")
        val planItems = mutableListOf<DownloadPlanItem>()
        val planWarnings = mutableListOf<PlanWarning>()

        var totalEstimatedBytes = 0L

        selections.forEach { (episodeId, result) ->
            val candidate = result.selected ?: return@forEach
            val release = candidate.release
            val source = resolveDownloadSource(release)
            val estimatedBytes = release.rawMetadata["sizeBytes"]?.toLongOrNull() ?: 0L

            if (source == null) {
                planItems += DownloadPlanItem(
                    id = DownloadPlanItemId("item-${UUID.randomUUID()}"),
                    releaseId = release.id,
                    episodeId = episodeId,
                    source = DownloadSource.DirectFileSource(""),
                    destination = resolveDestination(release, destination),
                    priority = DownloadPriority.Normal,
                    decision = PlanDecision.SourceUnavailable,
                    warning = PlanWarning("NO_SOURCE", "No downloadable magnet, torrent, or HTTP URL found")
                )
                return@forEach
            }

            val canHandle = try {
                engineSelector.selectEngine(source, capabilities)
                true
            } catch (e: Exception) {
                false
            }

            if (!canHandle) {
                planItems += DownloadPlanItem(
                    id = DownloadPlanItemId("item-${UUID.randomUUID()}"),
                    releaseId = release.id,
                    episodeId = episodeId,
                    source = source,
                    destination = resolveDestination(release, destination),
                    priority = DownloadPriority.Normal,
                    decision = PlanDecision.PolicyBlocked,
                    warning = PlanWarning("UNSUPPORTED_ENGINE", "Runtime cannot execute source format")
                )
                return@forEach
            }

            val plannedDestination = resolveDestination(release, destination)
            val dupDecision = duplicateDetector.checkDuplicate(
                releaseId = release.id,
                providerReleaseId = release.providerReleaseId,
                source = source,
                targetFilePath = plannedDestination.finalFilePath,
                existingTasks = existingTasks
            )

            val decision = when (dupDecision) {
                DuplicateDecision.AlreadyDownloaded, DuplicateDecision.AlreadyDownloading, DuplicateDecision.AlreadyQueued ->
                    PlanDecision.SkippedDuplicate
                DuplicateDecision.PossibleDuplicate ->
                    PlanDecision.NeedsConfirmation
                DuplicateDecision.NotDuplicate ->
                    PlanDecision.Selected
            }

            if (decision == PlanDecision.Selected || decision == PlanDecision.Ready) {
                totalEstimatedBytes += estimatedBytes
            }

            planItems += DownloadPlanItem(
                id = DownloadPlanItemId("item-${UUID.randomUUID()}"),
                releaseId = release.id,
                episodeId = episodeId,
                source = source,
                destination = plannedDestination,
                priority = DownloadPriority.Normal,
                decision = decision,
                estimatedBytes = if (estimatedBytes > 0) estimatedBytes else null,
                warning = if (decision == PlanDecision.NeedsConfirmation || decision == PlanDecision.RequiresConfirmation) {
                    PlanWarning("POSSIBLE_DUPLICATE", "A similar download already exists for ${release.normalizedTitle}")
                } else null
            )
        }

        val storageEstimate = StorageEstimate.calculate(
            contentBytes = totalEstimatedBytes,
            availableBytes = capabilities.availableStorageBytes,
            safetyMarginMb = 500L
        )

        val finalItems = if (!storageEstimate.isSufficient) {
            planWarnings += PlanWarning(
                code = "INSUFFICIENT_STORAGE",
                message = "Required storage exceeds available space",
                isCritical = true
            )
            planItems.map { item ->
                if (item.decision == PlanDecision.Ready || item.decision == PlanDecision.Selected) {
                    item.copy(decision = PlanDecision.BlockedByStorage)
                } else item
            }
        } else {
            planItems
        }

        return DownloadPlan(
            id = planId,
            items = finalItems,
            totalItems = finalItems.size,
            estimatedBytes = totalEstimatedBytes,
            destination = destination,
            policy = policy,
            warnings = planWarnings
        )
    }

    private suspend fun evaluatePlanInternal(
        selections: Map<EpisodeId, SelectionResult>,
        profile: DownloadProfile?,
        policy: DownloadPolicy,
        destination: StorageTarget,
        capabilities: RuntimeCapabilities,
        existingTasks: List<DownloadTask>,
        existingPlans: List<DownloadPlan>
    ): DownloadPlanResult {
        // Pre-flight: Network policy evaluation (Section 33)
        if (!capabilities.isNetworkAvailable) {
            return DownloadPlanResult.Blocked(
                TaskBlockReason.NetworkRestricted,
                "No active network connection is available"
            )
        }

        if (policy.networkPolicy == NetworkPolicy.WifiOnly && capabilities.isMetered) {
            return DownloadPlanResult.Blocked(
                TaskBlockReason.NetworkRestricted,
                "Download blocked: Wi-Fi only policy active on metered network"
            )
        }

        val planId = DownloadPlanId("plan-${UUID.randomUUID()}")
        val planItems = mutableListOf<DownloadPlanItem>()
        val planWarnings = mutableListOf<PlanWarning>()

        var totalEstimatedBytes = 0L
        var duplicateResultFound: DuplicateResult? = null

        for ((episodeId, result) in selections) {
            val candidate = result.selected ?: continue
            val release = candidate.release
            val source = resolveDownloadSource(release)

            if (source == null) {
                planItems += DownloadPlanItem(
                    id = DownloadPlanItemId("item-${UUID.randomUUID()}"),
                    releaseId = release.id,
                    episodeId = episodeId,
                    source = DownloadSource.DirectFileSource(""),
                    destination = resolveDestination(release, destination),
                    priority = DownloadPriority.Normal,
                    decision = PlanDecision.SourceUnavailable,
                    warning = PlanWarning("NO_SOURCE", "No downloadable source found for ${release.normalizedTitle}")
                )
                continue
            }

            val plannedDestination = resolveDestination(release, destination)

            // Multi-layer duplicate detection (Section 9, 10, 11, 12)
            val dupResult = duplicateDetector.detectDuplicate(
                releaseId = release.id,
                providerReleaseId = release.providerReleaseId,
                source = source,
                targetFilePath = plannedDestination.finalFilePath,
                existingTasks = existingTasks,
                existingPlans = existingPlans
            )

            val estimatedBytes = release.rawMetadata["sizeBytes"]?.toLongOrNull() ?: 0L

            when (dupResult) {
                is DuplicateResult.ExistingTask -> {
                    duplicateResultFound = dupResult
                    planItems += DownloadPlanItem(
                        id = DownloadPlanItemId("item-${UUID.randomUUID()}"),
                        releaseId = release.id,
                        episodeId = episodeId,
                        source = source,
                        destination = plannedDestination,
                        priority = DownloadPriority.Normal,
                        decision = PlanDecision.Duplicate,
                        estimatedBytes = estimatedBytes,
                        warning = PlanWarning("EXISTING_TASK", "Task already exists in state: ${dupResult.state}")
                    )
                    continue
                }
                is DuplicateResult.ExistingFile -> {
                    planItems += DownloadPlanItem(
                        id = DownloadPlanItemId("item-${UUID.randomUUID()}"),
                        releaseId = release.id,
                        episodeId = episodeId,
                        source = source,
                        destination = plannedDestination,
                        priority = DownloadPriority.Normal,
                        decision = PlanDecision.RequiresConfirmation,
                        estimatedBytes = estimatedBytes,
                        warning = PlanWarning("FILE_EXISTS", "Physical file already exists: ${dupResult.filePath}")
                    )
                    continue
                }
                is DuplicateResult.ExistingPlan -> {
                    duplicateResultFound = dupResult
                    planItems += DownloadPlanItem(
                        id = DownloadPlanItemId("item-${UUID.randomUUID()}"),
                        releaseId = release.id,
                        episodeId = episodeId,
                        source = source,
                        destination = plannedDestination,
                        priority = DownloadPriority.Normal,
                        decision = PlanDecision.Duplicate,
                        estimatedBytes = estimatedBytes,
                        warning = PlanWarning("EXISTING_PLAN", "A plan already includes this release: ${dupResult.planId}")
                    )
                    continue
                }
                is DuplicateResult.SameTorrent, is DuplicateResult.SameRelease -> {
                    duplicateResultFound = dupResult
                    planItems += DownloadPlanItem(
                        id = DownloadPlanItemId("item-${UUID.randomUUID()}"),
                        releaseId = release.id,
                        episodeId = episodeId,
                        source = source,
                        destination = plannedDestination,
                        priority = DownloadPriority.Normal,
                        decision = PlanDecision.Duplicate,
                        estimatedBytes = estimatedBytes,
                        warning = PlanWarning("DUPLICATE_SOURCE", "Same torrent or release is already tracked")
                    )
                    continue
                }
                DuplicateResult.None -> {
                    // Ready
                    totalEstimatedBytes += estimatedBytes
                    planItems += DownloadPlanItem(
                        id = DownloadPlanItemId("item-${UUID.randomUUID()}"),
                        releaseId = release.id,
                        episodeId = episodeId,
                        source = source,
                        destination = plannedDestination,
                        priority = DownloadPriority.Normal,
                        decision = PlanDecision.Ready,
                        estimatedBytes = estimatedBytes
                    )
                }
            }
        }

        // Storage Check & Reservation (Section 13, 14, 15, 18)
        val storageContext = storageReservationManager?.getStorageContext(totalEstimatedBytes)
            ?: StorageContext(
                availableBytes = capabilities.availableStorageBytes,
                reservedBytes = 0L,
                requiredBytes = totalEstimatedBytes,
                safetyMarginBytes = 500L * 1024 * 1024
            )

        if (!storageContext.isSufficient) {
            return DownloadPlanResult.Blocked(
                TaskBlockReason.InsufficientStorage,
                "Insufficient storage: Required ${storageContext.requiredBytes / (1024 * 1024)} MB exceeds usable ${storageContext.usableAvailableBytes / (1024 * 1024)} MB"
            )
        }

        val plan = DownloadPlan(
            id = planId,
            items = planItems,
            totalItems = planItems.size,
            estimatedBytes = totalEstimatedBytes,
            destination = destination,
            policy = policy,
            policySnapshot = DownloadPolicySnapshot(
                networkPolicy = policy.networkPolicy
            ),
            warnings = planWarnings
        )

        // If duplicate detected and no ready items, return Duplicate
        if (duplicateResultFound != null && planItems.none { it.decision == PlanDecision.Ready }) {
            return DownloadPlanResult.Duplicate(
                duplicateResult = duplicateResultFound,
                explanation = "A duplicate download task or file already exists"
            )
        }

        // If any item requires confirmation
        if (planItems.any { it.decision == PlanDecision.RequiresConfirmation }) {
            return DownloadPlanResult.RequiresConfirmation(
                plan = plan,
                reason = "Existing destination file collision detected",
                warnings = planItems.mapNotNull { it.warning }
            )
        }

        return DownloadPlanResult.Ready(plan)
    }

    private fun resolveDownloadSource(release: NormalizedRelease): DownloadSource? {
        val magnetUriStr = release.rawMetadata["magnetUri"] ?: release.rawMetadata["magnet"]
        if (!magnetUriStr.isNullOrBlank()) {
            val parsedMagnet = UrlValue.parse(magnetUriStr) as? UrlValue.MagnetUri
            val infoHash = release.rawMetadata["infoHash"]?.let { InfoHash(it) }
            return if (parsedMagnet != null) {
                DownloadSource.Magnet(
                    uri = parsedMagnet,
                    infoHash = infoHash,
                    displayName = release.normalizedTitle
                )
            } else {
                DownloadSource.MagnetSource(
                    uri = magnetUriStr,
                    infoHash = release.rawMetadata["infoHash"],
                    displayName = release.normalizedTitle
                )
            }
        }

        val torrentUrlStr = release.rawMetadata["torrentUrl"] ?: release.rawMetadata["torrent"]
        if (!torrentUrlStr.isNullOrBlank()) {
            val parsedUrl = UrlValue.parse(torrentUrlStr)
            val infoHash = release.rawMetadata["infoHash"]?.let { InfoHash(it) }
            return DownloadSource.TorrentFile(
                uri = parsedUrl,
                infoHash = infoHash
            )
        }

        val directUrlStr = release.rawMetadata["downloadUrl"] ?: release.rawMetadata["directUrl"]
        if (!directUrlStr.isNullOrBlank()) {
            val parsedUrl = UrlValue.parse(directUrlStr)
            return DownloadSource.Http(
                url = parsedUrl,
                expectedSize = release.rawMetadata["sizeBytes"]?.toLongOrNull()
            )
        }

        return null
    }

    private fun resolveDestination(
        release: NormalizedRelease,
        storageTarget: StorageTarget
    ): PlannedDestination {
        val animeName = release.animeCandidate ?: "Anime"
        val seasonNum = release.seasonCandidate ?: 1
        val safeAnime = pathSanitizer.sanitizeFilename(animeName, "")
        val safeSeason = "Season ${String.format("%02d", seasonNum)}"

        val safeFilename = pathSanitizer.sanitizeFilename("${release.normalizedTitle}.mkv")

        val finalDir = "${storageTarget.fullPath}/$safeAnime/$safeSeason"
        val tempDir = "${storageTarget.fullPath}/.aniflow/temp"

        return PlannedDestination(
            storageTarget = storageTarget,
            filename = safeFilename,
            tempDirectory = tempDir,
            finalDirectory = finalDir
        )
    }
}
