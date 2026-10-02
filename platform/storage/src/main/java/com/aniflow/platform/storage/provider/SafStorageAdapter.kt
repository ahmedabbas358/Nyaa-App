package com.aniflow.platform.storage.provider

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.aniflow.platform.storage.model.StorageAccessState
import com.aniflow.platform.storage.model.StorageDirectory
import com.aniflow.platform.storage.model.StorageFile
import com.aniflow.platform.storage.model.StorageLocation
import com.aniflow.platform.storage.model.StorageTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant

/**
 * Storage Access Framework (SAF) adapter (Section 7, 11, 14, 176).
 * Manages user-selected folder trees, persistable URI permissions, and permission recovery.
 */
class SafStorageAdapter(
    private val context: Context
) : StorageProvider {

    fun takePersistablePermission(treeUri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        } catch (_: Exception) {}
    }

    fun releasePersistablePermission(treeUri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.releasePersistableUriPermission(treeUri, flags)
        } catch (_: Exception) {}
    }

    override suspend fun checkAccess(location: StorageLocation): StorageAccessState = withContext(Dispatchers.IO) {
        val uri = Uri.parse(location.persistentReference)
        val doc = DocumentFile.fromTreeUri(context, uri) ?: return@withContext StorageAccessState.Missing

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
    }

    override suspend fun getAvailableBytes(location: StorageLocation): Long? = withContext(Dispatchers.IO) {
        // Fallback to null or stat if available
        null
    }

    override suspend fun createDirectory(target: StorageTarget): StorageDirectory = withContext(Dispatchers.IO) {
        val rootDoc = resolveTreeRoot(target.locationId.value)
            ?: throw IllegalStateException("Cannot resolve SAF tree root for location ${target.locationId}")

        var currentDir = rootDoc
        val segments = target.relativePath.split('/', '\\').filter { it.isNotBlank() }

        for (segment in segments) {
            val existing = currentDir.findFile(segment)
            currentDir = if (existing != null && existing.isDirectory) {
                existing
            } else {
                currentDir.createDirectory(segment)
                    ?: throw IllegalStateException("Failed to create SAF directory: $segment")
            }
        }

        StorageDirectory(
            name = currentDir.name ?: target.relativePath.substringAfterLast('/'),
            relativePath = target.relativePath,
            persistentUri = currentDir.uri.toString()
        )
    }

    override suspend fun createFile(
        target: StorageTarget,
        name: String,
        mimeType: String
    ): StorageFile = withContext(Dispatchers.IO) {
        val dir = createDirectory(target)
        val dirDoc = DocumentFile.fromTreeUri(context, Uri.parse(dir.persistentUri))
            ?: throw IllegalStateException("Cannot access directory document: ${dir.persistentUri}")

        val fileDoc = dirDoc.createFile(mimeType, name)
            ?: throw IllegalStateException("Failed to create SAF file: $name")

        StorageFile(
            name = fileDoc.name ?: name,
            relativePath = "${target.relativePath}/$name",
            persistentUri = fileDoc.uri.toString(),
            sizeBytes = 0L,
            lastModified = Instant.now(),
            mimeType = mimeType
        )
    }

    override suspend fun move(source: StorageFile, target: StorageTarget): StorageFile = withContext(Dispatchers.IO) {
        val newFile = createFile(target, source.name, source.mimeType ?: "video/x-matroska")
        val input = openInputStream(source) ?: throw IllegalStateException("Cannot open source stream for move")
        val output = openOutputStream(newFile) ?: throw IllegalStateException("Cannot open target stream for move")

        input.use { inStream ->
            output.use { outStream ->
                inStream.copyTo(outStream)
            }
        }
        delete(source)
        newFile
    }

    override suspend fun copy(source: StorageFile, target: StorageTarget): StorageFile = withContext(Dispatchers.IO) {
        val newFile = createFile(target, source.name, source.mimeType ?: "video/x-matroska")
        val input = openInputStream(source) ?: throw IllegalStateException("Cannot open source stream for copy")
        val output = openOutputStream(newFile) ?: throw IllegalStateException("Cannot open target stream for copy")

        input.use { inStream ->
            output.use { outStream ->
                inStream.copyTo(outStream)
            }
        }
        newFile
    }

    override suspend fun delete(file: StorageFile): Boolean = withContext(Dispatchers.IO) {
        val doc = DocumentFile.fromSingleUri(context, Uri.parse(file.persistentUri))
            ?: return@withContext false
        doc.delete()
    }

    override suspend fun openInputStream(file: StorageFile): InputStream? = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(Uri.parse(file.persistentUri))
    }

    override suspend fun openOutputStream(file: StorageFile): OutputStream? = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(Uri.parse(file.persistentUri))
    }

    override suspend fun exists(target: StorageTarget): Boolean = withContext(Dispatchers.IO) {
        val root = resolveTreeRoot(target.locationId.value) ?: return@withContext false
        var current = root
        val segments = target.relativePath.split('/', '\\').filter { it.isNotBlank() }
        for (segment in segments) {
            val child = current.findFile(segment) ?: return@withContext false
            current = child
        }
        true
    }

    private fun resolveTreeRoot(uriString: String): DocumentFile? {
        return try {
            DocumentFile.fromTreeUri(context, Uri.parse(uriString))
        } catch (_: Exception) {
            null
        }
    }
}
