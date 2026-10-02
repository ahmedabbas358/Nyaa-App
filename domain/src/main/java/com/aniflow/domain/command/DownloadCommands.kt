package com.aniflow.domain.command

import com.aniflow.domain.identity.DownloadTaskId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.model.aggregate.download.DownloadPriority
import com.aniflow.domain.valueobject.SearchQuery
import com.aniflow.domain.valueobject.StorageTarget

/**
 * Commands dispatched to the download orchestrator and business services (Section 61 & 86).
 * Explicitly separated from Domain Events.
 */
sealed interface DownloadCommand {
    val taskId: DownloadTaskId

    data class Queue(override val taskId: DownloadTaskId) : DownloadCommand
    data class Start(override val taskId: DownloadTaskId) : DownloadCommand
    data class Pause(override val taskId: DownloadTaskId) : DownloadCommand
    data class Resume(override val taskId: DownloadTaskId) : DownloadCommand
    data class Retry(override val taskId: DownloadTaskId) : DownloadCommand
    data class Cancel(override val taskId: DownloadTaskId) : DownloadCommand
    data class Remove(override val taskId: DownloadTaskId, val deleteFiles: Boolean = false) : DownloadCommand
    data class Recheck(override val taskId: DownloadTaskId) : DownloadCommand
    data class Move(override val taskId: DownloadTaskId, val target: StorageTarget) : DownloadCommand
    data class ChangePriority(override val taskId: DownloadTaskId, val priority: DownloadPriority) : DownloadCommand
}

/**
 * Commands dispatched for search queries and pagination.
 */
sealed interface SearchCommand {
    data class Execute(val query: SearchQuery) : SearchCommand
    data class Refresh(val query: SearchQuery) : SearchCommand
    data class LoadMore(val query: SearchQuery) : SearchCommand
    data object Cancel : SearchCommand
}

/**
 * Commands dispatched for selection management.
 */
sealed interface SelectionCommand {
    val sessionId: String

    data class SelectRelease(override val sessionId: String, val releaseId: ReleaseId) : SelectionCommand
    data class DeselectRelease(override val sessionId: String, val releaseId: ReleaseId) : SelectionCommand
    data class OverrideEpisode(
        override val sessionId: String,
        val episodeId: EpisodeId,
        val releaseId: ReleaseId
    ) : SelectionCommand
    data class Clear(override val sessionId: String) : SelectionCommand
}
