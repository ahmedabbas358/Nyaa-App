package com.aniflow.domain.event

import com.aniflow.domain.identity.DownloadTaskId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.LibraryItemId
import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.model.aggregate.release.ReleaseEpisodeRelationType
import com.aniflow.domain.state.ProviderHealthState
import com.aniflow.domain.valueobject.ParseInfo
import com.aniflow.domain.valueobject.StorageTarget
import java.time.Instant

/**
 * Domain Events emitted when business milestones occur (Section 85 & 86).
 * Independent of Android BroadcastReceivers or notification frameworks.
 */
sealed interface DomainEvent {
    val timestamp: Instant

    data class ReleaseDiscovered(
        val release: Release,
        override val timestamp: Instant = Instant.now()
    ) : DomainEvent

    data class ReleaseParsed(
        val releaseId: ReleaseId,
        val parseInfo: ParseInfo,
        override val timestamp: Instant = Instant.now()
    ) : DomainEvent

    data class ReleaseGrouped(
        val groupKey: String,
        val releaseCount: Int,
        override val timestamp: Instant = Instant.now()
    ) : DomainEvent

    data class EpisodeMatched(
        val releaseId: ReleaseId,
        val episodeId: EpisodeId,
        val relationType: ReleaseEpisodeRelationType,
        override val timestamp: Instant = Instant.now()
    ) : DomainEvent

    data class DownloadQueued(
        val taskId: DownloadTaskId,
        override val timestamp: Instant = Instant.now()
    ) : DomainEvent

    data class DownloadStarted(
        val taskId: DownloadTaskId,
        override val timestamp: Instant = Instant.now()
    ) : DomainEvent

    data class DownloadPaused(
        val taskId: DownloadTaskId,
        override val timestamp: Instant = Instant.now()
    ) : DomainEvent

    data class DownloadCompleted(
        val taskId: DownloadTaskId,
        val destination: StorageTarget,
        override val timestamp: Instant = Instant.now()
    ) : DomainEvent

    data class DownloadFailed(
        val taskId: DownloadTaskId,
        val errorMessage: String,
        override val timestamp: Instant = Instant.now()
    ) : DomainEvent

    data class LibraryIndexed(
        val libraryItemId: LibraryItemId,
        override val timestamp: Instant = Instant.now()
    ) : DomainEvent

    data class ProviderHealthChanged(
        val providerId: ProviderId,
        val oldState: ProviderHealthState,
        val newState: ProviderHealthState,
        override val timestamp: Instant = Instant.now()
    ) : DomainEvent
}
