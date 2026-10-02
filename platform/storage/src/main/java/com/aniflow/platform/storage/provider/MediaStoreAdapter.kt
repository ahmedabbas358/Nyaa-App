package com.aniflow.platform.storage.provider

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
 * MediaStore storage provider adapter (Section 21, 22, 117, 121).
 * Uses Scoped Storage and MediaStore collections for shared user-facing video/audio files
 * without requiring broad MANAGE_EXTERNAL_STORAGE permission.
 */
class MediaStoreAdapter(
    private val context: Context
) : StorageProvider {

    override suspend fun checkAccess(location: StorageLocation): StorageAccessState = withContext(Dispatchers.IO) {
        StorageAccessState.Granted
    }

    override suspend fun getAvailableBytes(location: StorageLocation): Long? = withContext(Dispatchers.IO) {
        null
    }

    override suspend fun createDirectory(target: StorageTarget): StorageDirectory = withContext(Dispatchers.IO) {
        // MediaStore directories are logical relative paths
        StorageDirectory(
            name = target.relativePath.substringAfterLast('/'),
            relativePath = target.relativePath,
            persistentUri = "mediastore://${target.relativePath}"
        )
    }

    override suspend fun createFile(
        target: StorageTarget,
        name: String,
        mimeType: String
    ): StorageFile = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/${target.relativePath}")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val insertedUri = resolver.insert(collectionUri, contentValues)
            ?: throw IllegalStateException("Failed to insert MediaStore item: $name")

        StorageFile(
            name = name,
            relativePath = "${target.relativePath}/$name",
            persistentUri = insertedUri.toString(),
            sizeBytes = 0L,
            lastModified = Instant.now(),
            mimeType = mimeType
        )
    }

    suspend fun markComplete(file: StorageFile) = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            context.contentResolver.update(Uri.parse(file.persistentUri), contentValues, null, null)
        }
    }

    override suspend fun move(source: StorageFile, target: StorageTarget): StorageFile = withContext(Dispatchers.IO) {
        val newFile = createFile(target, source.name, source.mimeType ?: "video/x-matroska")
        val input = openInputStream(source) ?: throw IllegalStateException("Cannot open source stream")
        val output = openOutputStream(newFile) ?: throw IllegalStateException("Cannot open target stream")

        input.use { inStream ->
            output.use { outStream ->
                inStream.copyTo(outStream)
            }
        }
        delete(source)
        markComplete(newFile)
        newFile
    }

    override suspend fun copy(source: StorageFile, target: StorageTarget): StorageFile = withContext(Dispatchers.IO) {
        val newFile = createFile(target, source.name, source.mimeType ?: "video/x-matroska")
        val input = openInputStream(source) ?: throw IllegalStateException("Cannot open source stream")
        val output = openOutputStream(newFile) ?: throw IllegalStateException("Cannot open target stream")

        input.use { inStream ->
            output.use { outStream ->
                inStream.copyTo(outStream)
            }
        }
        markComplete(newFile)
        newFile
    }

    override suspend fun delete(file: StorageFile): Boolean = withContext(Dispatchers.IO) {
        try {
            val rows = context.contentResolver.delete(Uri.parse(file.persistentUri), null, null)
            rows > 0
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun openInputStream(file: StorageFile): InputStream? = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(Uri.parse(file.persistentUri))
    }

    override suspend fun openOutputStream(file: StorageFile): OutputStream? = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(Uri.parse(file.persistentUri))
    }

    override suspend fun exists(target: StorageTarget): Boolean = withContext(Dispatchers.IO) {
        // Query MediaStore by relative path
        true
    }
}
