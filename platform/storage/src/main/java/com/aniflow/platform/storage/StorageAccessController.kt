package com.aniflow.platform.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.aniflow.platform.storage.model.StorageAccessState
import com.aniflow.platform.storage.model.StorageLocation
import com.aniflow.platform.storage.model.StorageLocationId
import com.aniflow.platform.storage.model.StorageLocationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * StorageAccessController (Section 7, 14, 176).
 * Manages SAF interaction, folder tree selection, persistable URI permissions,
 * and permission recovery when access is lost or revoked.
 */
interface StorageAccessController {
    fun getPersistedUriPermissions(): List<Uri>
    suspend fun persistTreePermission(treeUri: Uri): Boolean
    suspend fun releaseTreePermission(treeUri: Uri): Boolean
    suspend fun checkPermission(treeUri: Uri): StorageAccessState
    suspend fun buildStorageLocationFromTree(treeUri: Uri, customName: String? = null): StorageLocation
}

class AndroidStorageAccessController(
    private val context: Context
) : StorageAccessController {

    override fun getPersistedUriPermissions(): List<Uri> {
        return context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission && it.isWritePermission }
            .map { it.uri }
    }

    override suspend fun persistTreePermission(treeUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    override suspend fun releaseTreePermission(treeUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.releasePersistableUriPermission(treeUri, flags)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    override suspend fun checkPermission(treeUri: Uri): StorageAccessState = withContext(Dispatchers.IO) {
        try {
            val doc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext StorageAccessState.Missing
            if (!doc.exists()) {
                return@withContext StorageAccessState.Missing
            }
            if (!doc.canRead()) {
                return@withContext StorageAccessState.Revoked
            }
            if (!doc.canWrite()) {
                return@withContext StorageAccessState.ReadOnly
            }
            StorageAccessState.Granted
        } catch (_: SecurityException) {
            StorageAccessState.Revoked
        } catch (_: Exception) {
            StorageAccessState.Unavailable
        }
    }

    override suspend fun buildStorageLocationFromTree(
        treeUri: Uri,
        customName: String?
    ): StorageLocation = withContext(Dispatchers.IO) {
        val doc = DocumentFile.fromTreeUri(context, treeUri)
        val name = customName ?: doc?.name ?: "Custom Folder"
        val state = checkPermission(treeUri)

        StorageLocation(
            id = StorageLocationId("saf-${treeUri.hashCode()}"),
            type = StorageLocationType.UserSelectedTree,
            displayName = name,
            persistentReference = treeUri.toString(),
            availableBytes = null,
            totalBytes = null,
            isWritable = state == StorageAccessState.Granted,
            isRemovable = false,
            isMounted = true,
            accessState = state
        )
    }
}
