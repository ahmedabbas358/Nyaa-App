package com.aniflow.platform.storage.provider

import android.content.Context
import com.aniflow.platform.storage.model.StorageAccessState
import com.aniflow.platform.storage.model.StorageDirectory
import com.aniflow.platform.storage.model.StorageFile
import com.aniflow.platform.storage.model.StorageLocation
import com.aniflow.platform.storage.model.StorageTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant

/**
 * App-private storage adapter (Section 4, 5, 23, 25, 177).
 * Used strictly for temporary downloads (.part), segmented downloads, torrent staging,
 * parser caches, and internal databases.
 * Files in this storage are isolated and deleted upon application uninstall.
 */
class AppPrivateStorageAdapter(
    private val context: Context
) : StorageProvider {

    private val baseDir: File = File(context.filesDir, ".aniflow").apply {
        if (!exists()) mkdirs()
    }

    val tempDownloadsDir: File = File(baseDir, "downloads").apply { if (!exists()) mkdirs() }
    val segmentsDir: File = File(baseDir, "segments").apply { if (!exists()) mkdirs() }
    val torrentDir: File = File(baseDir, "torrent").apply { if (!exists()) mkdirs() }
    val verificationDir: File = File(baseDir, "verification").apply { if (!exists()) mkdirs() }
    val recoveryDir: File = File(baseDir, "recovery").apply { if (!exists()) mkdirs() }

    override suspend fun checkAccess(location: StorageLocation): StorageAccessState = withContext(Dispatchers.IO) {
        if (baseDir.exists() && baseDir.canWrite()) {
            StorageAccessState.Granted
        } else {
            StorageAccessState.Unavailable
        }
    }

    override suspend fun getAvailableBytes(location: StorageLocation): Long = withContext(Dispatchers.IO) {
        baseDir.usableSpace
    }

    override suspend fun createDirectory(target: StorageTarget): StorageDirectory = withContext(Dispatchers.IO) {
        val targetDir = File(baseDir, target.relativePath)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        StorageDirectory(
            name = targetDir.name,
            relativePath = target.relativePath,
            persistentUri = targetDir.toURI().toString()
        )
    }

    override suspend fun createFile(
        target: StorageTarget,
        name: String,
        mimeType: String
    ): StorageFile = withContext(Dispatchers.IO) {
        val targetDir = File(baseDir, target.relativePath).apply { if (!exists()) mkdirs() }
        val targetFile = File(targetDir, name)
        if (!targetFile.exists()) {
            targetFile.createNewFile()
        }
        StorageFile(
            name = name,
            relativePath = "${target.relativePath}/$name",
            persistentUri = targetFile.toURI().toString(),
            sizeBytes = targetFile.length(),
            lastModified = Instant.ofEpochMilli(targetFile.lastModified()),
            mimeType = mimeType
        )
    }

    override suspend fun move(source: StorageFile, target: StorageTarget): StorageFile = withContext(Dispatchers.IO) {
        val sourceFile = resolveFile(source.relativePath)
        val targetDir = File(baseDir, target.relativePath).apply { if (!exists()) mkdirs() }
        val destFile = File(targetDir, source.name)

        val moved = sourceFile.renameTo(destFile)
        if (!moved) {
            // Fallback to copy + delete
            sourceFile.inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            sourceFile.delete()
        }

        StorageFile(
            name = destFile.name,
            relativePath = "${target.relativePath}/${destFile.name}",
            persistentUri = destFile.toURI().toString(),
            sizeBytes = destFile.length(),
            lastModified = Instant.ofEpochMilli(destFile.lastModified()),
            mimeType = source.mimeType
        )
    }

    override suspend fun copy(source: StorageFile, target: StorageTarget): StorageFile = withContext(Dispatchers.IO) {
        val sourceFile = resolveFile(source.relativePath)
        val targetDir = File(baseDir, target.relativePath).apply { if (!exists()) mkdirs() }
        val destFile = File(targetDir, source.name)

        sourceFile.inputStream().use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        StorageFile(
            name = destFile.name,
            relativePath = "${target.relativePath}/${destFile.name}",
            persistentUri = destFile.toURI().toString(),
            sizeBytes = destFile.length(),
            lastModified = Instant.ofEpochMilli(destFile.lastModified()),
            mimeType = source.mimeType
        )
    }

    override suspend fun delete(file: StorageFile): Boolean = withContext(Dispatchers.IO) {
        val targetFile = resolveFile(file.relativePath)
        if (targetFile.exists()) {
            targetFile.delete()
        } else {
            true
        }
    }

    override suspend fun openInputStream(file: StorageFile): InputStream? = withContext(Dispatchers.IO) {
        val targetFile = resolveFile(file.relativePath)
        if (targetFile.exists()) FileInputStream(targetFile) else null
    }

    override suspend fun openOutputStream(file: StorageFile): OutputStream? = withContext(Dispatchers.IO) {
        val targetFile = resolveFile(file.relativePath)
        targetFile.parentFile?.mkdirs()
        FileOutputStream(targetFile)
    }

    override suspend fun exists(target: StorageTarget): Boolean = withContext(Dispatchers.IO) {
        File(baseDir, target.relativePath).exists()
    }

    fun getPhysicalFile(relativePath: String): File {
        return File(baseDir, relativePath)
    }

    private fun resolveFile(relativePath: String): File {
        return File(baseDir, relativePath)
    }
}
