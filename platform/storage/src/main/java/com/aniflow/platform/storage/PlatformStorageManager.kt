package com.aniflow.platform.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.aniflow.core.common.sanitization.FilenameSanitizer
import java.io.InputStream
import java.io.OutputStream

data class UserSelectedDirectory(
    val treeUri: Uri,
    val displayPath: String,
    val isWritable: Boolean
)

interface PlatformStorageManager {
    fun takePersistableUriPermission(uri: Uri)
    fun releasePersistableUriPermission(uri: Uri)
    fun createDocument(treeUri: Uri, mimeType: String, displayName: String): Uri?
    fun openInputStream(uri: Uri): InputStream?
    fun openOutputStream(uri: Uri): OutputStream?
    fun deleteDocument(uri: Uri): Boolean
    fun exists(uri: Uri): Boolean
}

class AndroidPlatformStorageManager(private val context: Context) : PlatformStorageManager {

    override fun takePersistableUriPermission(uri: Uri) {
        val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, flags)
    }

    override fun releasePersistableUriPermission(uri: Uri) {
        val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.releasePersistableUriPermission(uri, flags)
        } catch (_: Exception) {}
    }

    override fun createDocument(treeUri: Uri, mimeType: String, displayName: String): Uri? {
        val dir = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val safeName = FilenameSanitizer.sanitize(displayName)
        return dir.createFile(mimeType, safeName)?.uri
    }

    override fun openInputStream(uri: Uri): InputStream? =
        context.contentResolver.openInputStream(uri)

    override fun openOutputStream(uri: Uri): OutputStream? =
        context.contentResolver.openOutputStream(uri)

    override fun deleteDocument(uri: Uri): Boolean {
        val doc = DocumentFile.fromSingleUri(context, uri) ?: return false
        return doc.delete()
    }

    override fun exists(uri: Uri): Boolean {
        val doc = DocumentFile.fromSingleUri(context, uri) ?: return false
        return doc.exists()
    }
}
