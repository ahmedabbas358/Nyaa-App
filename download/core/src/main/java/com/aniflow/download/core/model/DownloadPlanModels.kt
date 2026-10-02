package com.aniflow.download.core.model

import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.model.aggregate.download.DownloadPolicy
import com.aniflow.domain.model.aggregate.download.NetworkPolicy
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.InfoHash
import com.aniflow.domain.valueobject.UrlValue
import java.time.Instant

@JvmInline
value class DownloadPlanId(val value: String) {
    override fun toString(): String = value
}

@JvmInline
value class DownloadTaskId(val value: String) {
    override fun toString(): String = value
}

@JvmInline
value class DownloadPlanItemId(val value: String) {
    override fun toString(): String = value
}

@JvmInline
value class PlannedFileId(val value: String) {
    override fun toString(): String = value
}

/**
 * DownloadSource (Section 3).
 * Strongly typed source specification decoupled from UI and provider parsers.
 */
sealed interface DownloadSource {
    data class Http(
        val url: UrlValue,
        val headers: Map<String, String> = emptyMap(),
        val expectedSize: Long? = null
    ) : DownloadSource

    data class Magnet(
        val uri: UrlValue.MagnetUri,
        val infoHash: InfoHash? = null,
        val displayName: String? = null
    ) : DownloadSource

    data class TorrentFile(
        val uri: UrlValue,
        val infoHash: InfoHash? = null
    ) : DownloadSource

    // Direct string representations for backward compatibility
    data class HttpSource(
        val url: String,
        val headers: Map<String, String> = emptyMap(),
        val expectedSize: Long? = null
    ) : DownloadSource

    data class MagnetSource(
        val uri: String,
        val infoHash: String? = null,
        val displayName: String? = null
    ) : DownloadSource

    data class TorrentFileSource(
        val torrentFilePath: String,
        val infoHash: String? = null
    ) : DownloadSource

    data class DirectFileSource(
        val localPath: String
    ) : DownloadSource
}

enum class DownloadEngineType {
    Http,
    Torrent,
    DirectFile
}

enum class DownloadPriority(val value: Int) {
    Highest(4),
    High(3),
    Normal(2),
    Low(1),
    Lowest(0);

    companion object {
        fun fromValue(value: Int): DownloadPriority =
            entries.firstOrNull { it.value == value } ?: Normal
    }
}

/**
 * Plan decision outcome for each item (Section 7).
 */
enum class PlanDecision {
    Ready,
    Selected,
    Blocked,
    RequiresConfirmation,
    NeedsConfirmation,
    Duplicate,
    SkippedDuplicate,
    InsufficientStorage,
    BlockedByStorage,
    SourceUnavailable,
    SkippedUnavailable,
    InvalidDestination,
    SkippedInvalid,
    PolicyBlocked,
    BlockedByPolicy
}

/**
 * Multi-layer duplicate detection result (Section 10).
 */
sealed interface DuplicateResult {
    data object None : DuplicateResult
    data class ExistingTask(val taskId: DownloadTaskId, val state: DownloadTaskState = DownloadTaskState.Downloading) : DuplicateResult
    data class ExistingFile(val filePath: String, val fileId: String? = null) : DuplicateResult
    data class ExistingPlan(val planId: DownloadPlanId) : DuplicateResult
    data class SameTorrent(val infoHash: String) : DuplicateResult
    data class SameRelease(val releaseId: ReleaseId) : DuplicateResult
}

/**
 * Policy governing duplicates (Section 11).
 */
enum class DuplicatePolicy {
    Skip,
    Ask,
    ReuseExisting,
    Compare,
    KeepBoth,
    Replace,
    Upgrade
}

/**
 * Storage context for pre-flight reservation (Section 13).
 */
data class StorageContext(
    val availableBytes: Long,
    val reservedBytes: Long,
    val requiredBytes: Long,
    val safetyMarginBytes: Long
) {
    val usableAvailableBytes: Long
        get() = (availableBytes - reservedBytes - safetyMarginBytes).coerceAtLeast(0L)

    val isSufficient: Boolean
        get() = usableAvailableBytes >= requiredBytes
}

data class PlanWarning(
    val code: String,
    val message: String,
    val isCritical: Boolean = false
)

data class StorageTarget(
    val locationId: String,
    val relativePath: String,
    val absoluteBasePath: String
) {
    val fullPath: String
        get() = if (absoluteBasePath.endsWith("/") || relativePath.startsWith("/")) {
            "$absoluteBasePath$relativePath"
        } else {
            "$absoluteBasePath/$relativePath"
        }
}

data class PlannedDestination(
    val storageTarget: StorageTarget,
    val filename: String,
    val tempDirectory: String,
    val finalDirectory: String
) {
    val tempFilePath: String
        get() = "$tempDirectory/$filename.aniflow.part"

    val finalFilePath: String
        get() = "$finalDirectory/$filename"
}

data class PlannedFile(
    val id: PlannedFileId,
    val relativePath: String,
    val filename: String,
    val expectedSize: Long?,
    val isSelected: Boolean = true
)

data class FilenamePolicy(
    val pattern: String = "{anime} - S{season:02d}E{episode:02d} - {title}",
    val sanitizeForbiddenChars: Boolean = true,
    val collisionPolicy: CollisionPolicy = CollisionPolicy.Rename
) {
    companion object {
        val DEFAULT = FilenamePolicy()
    }
}

data class DownloadPolicySnapshot(
    val networkPolicy: NetworkPolicy = NetworkPolicy.AnyNetwork,
    val collisionPolicy: CollisionPolicy = CollisionPolicy.Rename,
    val duplicatePolicy: DuplicatePolicy = DuplicatePolicy.ReuseExisting,
    val maxActiveHttp: Int = 3,
    val maxActiveTorrent: Int = 1,
    val maxGlobalActive: Int = 4,
    val seedingPolicy: String = "StopAfterCompletion",
    val autoVerify: Boolean = true,
    val autoOrganize: Boolean = true
)

/**
 * DownloadPlanItem (Section 5).
 * Represents an actionable planned decision for a single release or episode item.
 */
data class DownloadPlanItem(
    val id: DownloadPlanItemId = DownloadPlanItemId("item-${java.util.UUID.randomUUID()}"),
    val releaseId: ReleaseId,
    val episodeId: EpisodeId? = null,
    val source: DownloadSource,
    val files: List<PlannedFile> = emptyList(),
    val destination: PlannedDestination,
    val filenamePolicy: FilenamePolicy = FilenamePolicy.DEFAULT,
    val priority: DownloadPriority = DownloadPriority.Normal,
    val decision: PlanDecision = PlanDecision.Ready,
    val estimatedBytes: Long? = null,
    val warning: PlanWarning? = null
)

/**
 * DownloadPlan (Section 4).
 * Immutable snapshot blueprint before tasks are instantiated and queued.
 */
data class DownloadPlan(
    val id: DownloadPlanId,
    val items: List<DownloadPlanItem>,
    val totalItems: Int = items.size,
    val estimatedBytes: Long? = null,
    val totalSize: ByteSize? = estimatedBytes?.let { ByteSize.fromBytes(it) },
    val destination: StorageTarget,
    val policy: DownloadPolicy = DownloadPolicy(),
    val policySnapshot: DownloadPolicySnapshot = DownloadPolicySnapshot(
        networkPolicy = policy.networkPolicy
    ),
    val warnings: List<PlanWarning> = emptyList(),
    val createdAt: Instant = Instant.now()
) {
    val selectedItems: List<DownloadPlanItem>
        get() = items.filter { it.decision == PlanDecision.Ready || it.decision == PlanDecision.Selected }

    val skippedItems: List<DownloadPlanItem>
        get() = items.filter { it.decision != PlanDecision.Ready && it.decision != PlanDecision.Selected }
}
