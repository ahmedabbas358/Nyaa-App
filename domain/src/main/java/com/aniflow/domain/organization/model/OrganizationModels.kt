package com.aniflow.domain.organization.model

import com.aniflow.domain.identity.LibraryFileId
import com.aniflow.domain.identity.OrganizationPlanId
import com.aniflow.domain.storage.model.StorageLocation
import java.time.Instant

/**
 * Naming template definition (Section 55, 56, 57).
 * Tokens: {anime}, {season}, {seasonNumber}, {episode}, {absoluteEpisode}, {episodeTitle},
 * {group}, {uploader}, {resolution}, {codec}, {audio}, {subtitles}, {source}, {year}, {filename}, {ext}
 */
data class NamingTemplate(
    val rawTemplate: String = "{anime}/Season {seasonNumber}/{episode} - {anime} [{resolution} {codec}].{ext}"
)

/**
 * Metadata context for rendering naming templates (Section 55, 59).
 */
data class NamingContext(
    val anime: String?,
    val season: String? = null,
    val seasonNumber: Int? = null,
    val episode: String? = null,
    val absoluteEpisode: Int? = null,
    val episodeTitle: String? = null,
    val group: String? = null,
    val uploader: String? = null,
    val resolution: String? = null,
    val codec: String? = null,
    val audio: String? = null,
    val subtitles: String? = null,
    val source: String? = null,
    val year: Int? = null,
    val originalFilename: String,
    val extension: String
)

/**
 * File collision policy (Section 64).
 * Default rule: NEVER overwrite silently!
 */
enum class CollisionPolicy {
    Skip,
    Ask,
    Overwrite,
    KeepBoth,
    Compare,
    Upgrade
}

enum class OrganizationOperation {
    SameVolumeAtomicMove,
    CrossVolumeCopyVerifyDelete,
    RenameInPlace
}

/**
 * Execution plan for organization / rename / move (Section 109, 110).
 */
data class OrganizationPlan(
    val id: OrganizationPlanId,
    val items: List<OrganizationPlanItem>,
    val collisionPolicy: CollisionPolicy = CollisionPolicy.KeepBoth,
    val createdAt: Instant = Instant.now()
)

data class OrganizationPlanItem(
    val fileId: LibraryFileId,
    val currentPath: StorageLocation,
    val targetPath: StorageLocation,
    val operation: OrganizationOperation,
    val expectedSizeBytes: Long,
    val hasCollision: Boolean = false,
    val collisionFileId: LibraryFileId? = null
)

sealed interface OrganizationExecutionResult {
    data object Success : OrganizationExecutionResult
    data class PartialSuccess(
        val completedCount: Int,
        val failedCount: Int,
        val errors: List<String>
    ) : OrganizationExecutionResult
    data class Failed(val error: String) : OrganizationExecutionResult
}
