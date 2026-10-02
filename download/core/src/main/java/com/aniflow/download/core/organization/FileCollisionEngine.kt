package com.aniflow.download.core.organization

import com.aniflow.platform.storage.model.StorageFile

enum class CollisionPolicy {
    Skip,
    Overwrite,
    KeepBoth,
    Compare,
    Ask
}

data class FileMediaAttributes(
    val fileName: String,
    val sizeBytes: Long,
    val modifiedEpochMillis: Long,
    val resolution: String? = null,
    val codec: String? = null,
    val durationSeconds: Long? = null,
    val hash: String? = null
)

data class CollisionComparisonResult(
    val existing: FileMediaAttributes,
    val incoming: FileMediaAttributes,
    val sizeDifferenceBytes: Long,
    val hasHigherResolution: Boolean,
    val hasBetterCodec: Boolean,
    val recommendedAction: CollisionPolicy
)

data class CollisionResolution(
    val policy: CollisionPolicy,
    val resolvedFileName: String,
    val proceedWithTransfer: Boolean,
    val requiresUserConfirmation: Boolean = false,
    val comparison: CollisionComparisonResult? = null
)

/**
 * FileCollisionEngine (Sections 83, 84, 85).
 * Detects filename collisions and evaluates policies before overwriting or moving files.
 */
class FileCollisionEngine {

    fun resolveCollision(
        incomingName: String,
        existingFile: StorageFile?,
        existingAttributes: FileMediaAttributes?,
        incomingAttributes: FileMediaAttributes,
        preferredPolicy: CollisionPolicy = CollisionPolicy.KeepBoth
    ): CollisionResolution {
        if (existingFile == null) {
            // No collision!
            return CollisionResolution(
                policy = CollisionPolicy.Overwrite,
                resolvedFileName = incomingName,
                proceedWithTransfer = true
            )
        }

        return when (preferredPolicy) {
            CollisionPolicy.Skip -> {
                CollisionResolution(
                    policy = CollisionPolicy.Skip,
                    resolvedFileName = existingFile.name,
                    proceedWithTransfer = false
                )
            }
            CollisionPolicy.Overwrite -> {
                CollisionResolution(
                    policy = CollisionPolicy.Overwrite,
                    resolvedFileName = existingFile.name,
                    proceedWithTransfer = true
                )
            }
            CollisionPolicy.KeepBoth -> {
                val newName = generateUniqueNumberedName(incomingName)
                CollisionResolution(
                    policy = CollisionPolicy.KeepBoth,
                    resolvedFileName = newName,
                    proceedWithTransfer = true
                )
            }
            CollisionPolicy.Compare, CollisionPolicy.Ask -> {
                val comparison = if (existingAttributes != null) {
                    compareAttributes(existingAttributes, incomingAttributes)
                } else null

                CollisionResolution(
                    policy = preferredPolicy,
                    resolvedFileName = incomingName,
                    proceedWithTransfer = false,
                    requiresUserConfirmation = true,
                    comparison = comparison
                )
            }
        }
    }

    fun compareAttributes(
        existing: FileMediaAttributes,
        incoming: FileMediaAttributes
    ): CollisionComparisonResult {
        val sizeDiff = incoming.sizeBytes - existing.sizeBytes

        val resOrder = listOf("480p", "720p", "1080p", "2160p", "4k")
        val existingResIndex = resOrder.indexOf(existing.resolution?.lowercase())
        val incomingResIndex = resOrder.indexOf(incoming.resolution?.lowercase())
        val hasHigherRes = incomingResIndex > existingResIndex && incomingResIndex != -1

        val codecOrder = listOf("xvid", "h264", "avc", "hevc", "h265", "av1")
        val existingCodecIndex = codecOrder.indexOf(existing.codec?.lowercase())
        val incomingCodecIndex = codecOrder.indexOf(incoming.codec?.lowercase())
        val hasBetterCodec = incomingCodecIndex > existingCodecIndex && incomingCodecIndex != -1

        val recommendation = when {
            hasHigherRes || hasBetterCodec -> CollisionPolicy.Overwrite
            sizeDiff < 0 && existing.durationSeconds == incoming.durationSeconds -> CollisionPolicy.Skip
            else -> CollisionPolicy.KeepBoth
        }

        return CollisionComparisonResult(
            existing = existing,
            incoming = incoming,
            sizeDifferenceBytes = sizeDiff,
            hasHigherResolution = hasHigherRes,
            hasBetterCodec = hasBetterCodec,
            recommendedAction = recommendation
        )
    }

    fun generateUniqueNumberedName(fileName: String, existingNames: Set<String> = emptySet()): String {
        val base = fileName.substringBeforeLast('.')
        val ext = fileName.substringAfterLast('.', "")
        val dotExt = if (ext.isNotEmpty()) ".$ext" else ""

        var counter = 1
        var candidate = "$base ($counter)$dotExt"
        while (existingNames.contains(candidate)) {
            counter++
            candidate = "$base ($counter)$dotExt"
        }
        return candidate
    }
}
