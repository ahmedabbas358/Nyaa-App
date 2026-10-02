package com.aniflow.domain.organization.engine

import com.aniflow.domain.identity.LibraryFileId
import com.aniflow.domain.identity.OrganizationPlanId
import com.aniflow.domain.library.model.LibraryFile
import com.aniflow.domain.organization.model.CollisionPolicy
import com.aniflow.domain.organization.model.NamingContext
import com.aniflow.domain.organization.model.NamingTemplate
import com.aniflow.domain.organization.model.OrganizationExecutionResult
import com.aniflow.domain.organization.model.OrganizationOperation
import com.aniflow.domain.organization.model.OrganizationPlan
import com.aniflow.domain.organization.model.OrganizationPlanItem
import com.aniflow.domain.organization.naming.NamingTemplateEngine
import com.aniflow.domain.storage.model.StorageLocation
import com.aniflow.domain.storage.model.StorageRoot
import java.util.UUID

/**
 * OrganizationEngine (Section 54, 61, 62, 63, 64, 65, 66, 107, 108, 109, 110, 111, 112).
 * Plans, previews, and executes file moves/renames with:
 * - Atomic same-volume moves
 * - Cross-volume copy -> verify -> delete source
 * - Strict collision policies (Default: Never overwrite silently!)
 * - Safe upgrade workflows: keeps old version until new is verified and in place.
 */
class OrganizationEngine(
    private val templateEngine: NamingTemplateEngine = NamingTemplateEngine()
) {

    /**
     * Builds an OrganizationPlan without modifying physical storage (Section 109, 110).
     */
    fun createPlan(
        files: List<Pair<LibraryFile, NamingContext>>,
        targetRoot: StorageRoot,
        template: NamingTemplate,
        collisionPolicy: CollisionPolicy = CollisionPolicy.KeepBoth,
        existingTargetPaths: Set<String> = emptySet()
    ): OrganizationPlan {
        val planId = OrganizationPlanId("plan_${UUID.randomUUID().toString().take(8)}")
        val items = mutableListOf<OrganizationPlanItem>()
        val claimedTargets = existingTargetPaths.toMutableSet()

        for ((file, context) in files) {
            val renderedRelativePath = templateEngine.render(template, context)
            val isSameVolume = file.location.storageId == targetRoot.id
            val isSameRelativePath = file.location.normalizedPath.equals(renderedRelativePath, ignoreCase = true)

            val operation = when {
                isSameRelativePath && isSameVolume -> OrganizationOperation.RenameInPlace
                isSameVolume -> OrganizationOperation.SameVolumeAtomicMove
                else -> OrganizationOperation.CrossVolumeCopyVerifyDelete
            }

            var finalTargetRelPath = renderedRelativePath
            var hasCollision = claimedTargets.contains(finalTargetRelPath.lowercase())

            if (hasCollision) {
                when (collisionPolicy) {
                    CollisionPolicy.KeepBoth -> {
                        // Disambiguate path: "Movie.mkv" -> "Movie (1).mkv"
                        val ext = finalTargetRelPath.substringAfterLast('.', "")
                        val base = finalTargetRelPath.substringBeforeLast('.')
                        var counter = 1
                        while (claimedTargets.contains("$base ($counter).$ext".lowercase())) {
                            counter++
                        }
                        finalTargetRelPath = "$base ($counter).$ext"
                        hasCollision = false
                    }
                    CollisionPolicy.Skip -> {
                        // Flag collision so executor can skip
                    }
                    CollisionPolicy.Overwrite -> {
                        // Keep path, explicit overwrite allowed by policy
                    }
                    else -> {}
                }
            }

            claimedTargets.add(finalTargetRelPath.lowercase())

            val targetLocation = StorageLocation(
                storageId = targetRoot.id,
                relativePath = finalTargetRelPath
            )

            items.add(
                OrganizationPlanItem(
                    fileId = file.id,
                    currentPath = file.location,
                    targetPath = targetLocation,
                    operation = operation,
                    expectedSizeBytes = file.sizeBytes,
                    hasCollision = hasCollision
                )
            )
        }

        return OrganizationPlan(
            id = planId,
            items = items,
            collisionPolicy = collisionPolicy
        )
    }

    /**
     * Executes the organization plan via provided IO operations (Section 111, 112).
     */
    suspend fun executePlan(
        plan: OrganizationPlan,
        moveOperation: suspend (from: StorageLocation, to: StorageLocation) -> Boolean,
        copyOperation: suspend (from: StorageLocation, to: StorageLocation) -> Boolean,
        deleteOperation: suspend (location: StorageLocation) -> Boolean,
        verifyOperation: suspend (location: StorageLocation, expectedBytes: Long) -> Boolean
    ): OrganizationExecutionResult {
        var completed = 0
        var failed = 0
        val errors = mutableListOf<String>()

        for (item in plan.items) {
            if (item.hasCollision && plan.collisionPolicy == CollisionPolicy.Skip) {
                // Skipped according to collision policy
                continue
            }

            try {
                when (item.operation) {
                    OrganizationOperation.RenameInPlace,
                    OrganizationOperation.SameVolumeAtomicMove -> {
                        val moved = moveOperation(item.currentPath, item.targetPath)
                        if (moved && verifyOperation(item.targetPath, item.expectedSizeBytes)) {
                            completed++
                        } else {
                            failed++
                            errors.add("Atomic move or verification failed for file ${item.fileId.value}")
                        }
                    }
                    OrganizationOperation.CrossVolumeCopyVerifyDelete -> {
                        // Section 61: Copy -> Verify -> Delete source
                        val copied = copyOperation(item.currentPath, item.targetPath)
                        if (!copied) {
                            failed++
                            errors.add("Cross-volume copy failed for file ${item.fileId.value}")
                            continue
                        }

                        val verified = verifyOperation(item.targetPath, item.expectedSizeBytes)
                        if (!verified) {
                            failed++
                            errors.add("Verification failed for copied target ${item.targetPath}; keeping source safe")
                            deleteOperation(item.targetPath) // clean bad copy
                            continue
                        }

                        // Target verified, safely delete source
                        deleteOperation(item.currentPath)
                        completed++
                    }
                }
            } catch (e: Exception) {
                failed++
                errors.add("Exception organizing file ${item.fileId.value}: ${e.message}")
            }
        }

        return when {
            failed == 0 -> OrganizationExecutionResult.Success
            completed > 0 -> OrganizationExecutionResult.PartialSuccess(completed, failed, errors)
            else -> OrganizationExecutionResult.Failed(errors.joinToString("; "))
        }
    }

    /**
     * Safe upgrade workflow (Section 65, 66).
     * Keeps old version intact until new file is completely verified and placed.
     */
    suspend fun executeSafeUpgrade(
        oldFileLocation: StorageLocation,
        newFileLocation: StorageLocation,
        expectedSizeBytes: Long,
        verifyNewFile: suspend (StorageLocation, Long) -> Boolean,
        deleteOldFile: suspend (StorageLocation) -> Boolean
    ): Boolean {
        // Step 1: Verify new file
        val isNewValid = verifyNewFile(newFileLocation, expectedSizeBytes)
        if (!isNewValid) {
            // Keep old version intact!
            return false
        }

        // Step 2: Delete old file only after new version is confirmed valid
        deleteOldFile(oldFileLocation)
        return true
    }
}
