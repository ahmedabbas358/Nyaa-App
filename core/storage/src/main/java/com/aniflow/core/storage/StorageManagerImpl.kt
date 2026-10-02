package com.aniflow.core.storage

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.aniflow.domain.service.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class StorageManagerImpl(
    private val context: Context
) : StorageManager {

    override suspend fun getAvailableSpaceBytes(directoryPath: String): Long {
        return withContext(Dispatchers.IO) {
            try {
                val file = resolveFile(directoryPath)
                val targetDir = if (file.exists()) file else file.parentFile ?: context.filesDir
                val stat = StatFs(targetDir.absolutePath)
                stat.availableBlocksLong * stat.blockSizeLong
            } catch (e: Exception) {
                0L
            }
        }
    }

    override suspend fun hasWriteAccess(directoryPath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = resolveFile(directoryPath)
                file.mkdirs()
                val testFile = File(file, ".test_write_${UUID.randomUUID()}")
                val created = testFile.createNewFile()
                if (created) {
                    testFile.delete()
                    true
                } else false
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun createTempFile(prefix: String, suffix: String): String {
        return withContext(Dispatchers.IO) {
            val cacheDir = context.cacheDir
            val tempFile = File.createTempFile(prefix, suffix, cacheDir)
            tempFile.absolutePath
        }
    }

    override suspend fun moveToFinal(tempPath: String, finalPath: String): Boolean {
        return withContext(Dispatchers.IO) {
            val tempFile = File(tempPath)
            val finalFile = resolveFile(finalPath)
            finalFile.parentFile?.mkdirs()
            tempFile.renameTo(finalFile)
        }
    }

    override suspend fun deleteFile(path: String): Boolean {
        return withContext(Dispatchers.IO) {
            val file = resolveFile(path)
            if (file.exists()) file.delete() else false
        }
    }

    override suspend fun fileExists(path: String): Boolean {
        return withContext(Dispatchers.IO) {
            resolveFile(path).exists()
        }
    }

    override suspend fun getFileSize(path: String): Long {
        return withContext(Dispatchers.IO) {
            val file = resolveFile(path)
            if (file.exists()) file.length() else 0L
        }
    }

    private fun resolveFile(path: String): File {
        return if (path.startsWith("/")) {
            File(path)
        } else {
            val base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            File(base, path)
        }
    }
}
