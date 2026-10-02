package com.aniflow.platform.storage.provider

import com.aniflow.platform.storage.model.StorageAccessState
import com.aniflow.platform.storage.model.StorageDirectory
import com.aniflow.platform.storage.model.StorageFile
import com.aniflow.platform.storage.model.StorageLocation
import com.aniflow.platform.storage.model.StorageTarget
import java.io.InputStream
import java.io.OutputStream

/**
 * Universal Storage Provider interface (Section 12, 13).
 * Acts as the boundary between Domain and Android storage abstractions (SAF, MediaStore, AppPrivate).
 */
interface StorageProvider {

    suspend fun checkAccess(location: StorageLocation): StorageAccessState

    suspend fun getAvailableBytes(location: StorageLocation): Long?

    suspend fun createDirectory(target: StorageTarget): StorageDirectory

    suspend fun createFile(
        target: StorageTarget,
        name: String,
        mimeType: String = "video/x-matroska"
    ): StorageFile

    suspend fun move(source: StorageFile, target: StorageTarget): StorageFile

    suspend fun copy(source: StorageFile, target: StorageTarget): StorageFile

    suspend fun delete(file: StorageFile): Boolean

    suspend fun openInputStream(file: StorageFile): InputStream?

    suspend fun openOutputStream(file: StorageFile): OutputStream?

    suspend fun exists(target: StorageTarget): Boolean
}
