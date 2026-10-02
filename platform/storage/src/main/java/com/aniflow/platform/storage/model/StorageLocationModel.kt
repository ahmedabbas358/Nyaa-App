package com.aniflow.platform.storage.model

import java.time.Instant

@JvmInline
value class StorageLocationId(val value: String) {
    override fun toString(): String = value
}

@JvmInline
value class StorageProfileId(val value: String) {
    override fun toString(): String = value
}

enum class StorageLocationType {
    InternalApp,
    ExternalAppSpecific,
    UserSelectedTree,     // Storage Access Framework (SAF)
    MediaStoreDownloads,
    MediaStoreVideo,
    MediaStoreAudio,
    Unknown
}

enum class StorageAccessState {
    Granted,
    Missing,
    Revoked,
    ReadOnly,
    Unavailable,
    NotSupported
}

/**
 * Physical or virtual storage location (Section 8, 9).
 * Decoupled from raw file system paths.
 */
data class StorageLocation(
    val id: StorageLocationId,
    val type: StorageLocationType,
    val displayName: String,
    val persistentReference: String, // String Uri or absolute base path
    val availableBytes: Long? = null,
    val totalBytes: Long? = null,
    val isWritable: Boolean = true,
    val isRemovable: Boolean = false,
    val isMounted: Boolean = true,
    val accessState: StorageAccessState = StorageAccessState.Granted
) {
    val isAccessible: Boolean
        get() = isMounted && (accessState == StorageAccessState.Granted || accessState == StorageAccessState.ReadOnly)
}

/**
 * Domain representation of a storage target (Section 10, 20).
 * Never stores Android Uri or hardware paths directly in Domain.
 */
data class StorageTarget(
    val locationId: StorageLocationId,
    val relativePath: String
)

data class StorageFile(
    val name: String,
    val relativePath: String,
    val persistentUri: String,
    val sizeBytes: Long,
    val lastModified: Instant = Instant.now(),
    val mimeType: String? = null
)

data class StorageDirectory(
    val name: String,
    val relativePath: String,
    val persistentUri: String
)

enum class StorageMediaType {
    Anime,
    Movie,
    Download,
    Other
}

data class StorageTargetRule(
    val mediaType: StorageMediaType,
    val targetLocationId: StorageLocationId,
    val subfolderTemplate: String = "{mediaType}"
)

/**
 * Policy routing media types to specific storage locations (Section 15, 16, 17).
 * Example: Anime -> SD Card, Movies -> Internal, Other -> Downloads.
 */
data class StorageRoutingPolicy(
    val rules: List<StorageTargetRule> = listOf(
        StorageTargetRule(StorageMediaType.Anime, StorageLocationId("loc-sd"), "Anime"),
        StorageTargetRule(StorageMediaType.Movie, StorageLocationId("loc-internal"), "Movies"),
        StorageTargetRule(StorageMediaType.Download, StorageLocationId("loc-downloads"), "AniFlow")
    )
) {
    fun resolveLocationFor(mediaType: StorageMediaType): StorageLocationId {
        return rules.firstOrNull { it.mediaType == mediaType }?.targetLocationId
            ?: StorageLocationId("default")
    }
}

data class StorageProfile(
    val id: StorageProfileId,
    val name: String,
    val routingPolicy: StorageRoutingPolicy = StorageRoutingPolicy(),
    val availableSpaceThresholdBytes: Long = 1024 * 1024 * 500L // 500 MB
)
