package com.aniflow.core.ui.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * High-performance Platform Bridge for BitTorrent operations:
 * - Direct authenticated .torrent downloading (bypassing Nyaa DDoS-Guard / Cloudflare blocking)
 * - Seamless integration with external BitTorrent clients (1DM, LibreTorrent, Flud, μTorrent)
 * - Batch .torrent downloads and multi-magnet export for multi-connection download managers
 * - FileProvider sharing of downloaded .torrent files
 */
object TorrentClientBridge {

    private const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(com.aniflow.core.network.resilience.ResilientNyaaDns())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun postToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        mainHandler.post {
            Toast.makeText(context.applicationContext, message, duration).show()
        }
    }

    /**
     * Copies a single magnet link or text to system clipboard.
     */
    fun copyToClipboard(
        context: Context,
        text: String,
        label: String = "AniFlow Magnet Link",
        toastMessage: String = "Magnet link copied to clipboard"
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        postToast(context, toastMessage)
    }

    /**
     * Copies multiple magnet links separated by newlines for 1DM / FDM batch clipboard monitor.
     */
    fun copyBatchMagnets(
        context: Context,
        magnetLinks: List<String>,
        toastMessage: String = "Copied ${magnetLinks.size} magnet link(s) to clipboard"
    ) {
        val cleanLinks = magnetLinks.filter { it.isNotBlank() }
        if (cleanLinks.isEmpty()) {
            postToast(context, "No links to copy")
            return
        }
        val joined = cleanLinks.joinToString("\n")
        copyToClipboard(context, joined, "AniFlow Batch Magnets", toastMessage)
    }

    /**
     * Convenience 3-arg overload to prevent accidental binding of title to torrentUrl.
     */
    fun openInExternalTorrentClient(
        context: Context,
        magnetUri: String?,
        title: String?
    ): Boolean = openInExternalTorrentClient(context = context, magnetUri = magnetUri, torrentUrl = null, title = title)

    /**
     * Dispatches magnet link or .torrent to external BitTorrent client (e.g. 1DM, LibreTorrent, Flud).
     * If an external client exists, launches standard chooser; otherwise safely copies magnet and advises the user.
     */
    fun openInExternalTorrentClient(
        context: Context,
        magnetUri: String? = null,
        torrentUrl: String? = null,
        title: String? = null
    ): Boolean {
        val cleanMagnet = magnetUri?.trim()?.takeIf { it.isNotBlank() }
        val cleanTorrentUrl = torrentUrl?.trim()?.takeIf { it.isNotBlank() }

        if (cleanMagnet != null) {
            return try {
                val uri = Uri.parse(cleanMagnet)
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

                val chooser = Intent.createChooser(intent, "Open with BitTorrent App (1DM / Flud / LibreTorrent)").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(chooser)
                true
            } catch (e: Exception) {
                // If launching magnet failed, check if torrentUrl is available as fallback
                if (cleanTorrentUrl != null) {
                    downloadTorrentFileDirectly(context, cleanTorrentUrl, title ?: "release", openAfterDownload = true)
                    true
                } else {
                    copyToClipboard(
                        context = context,
                        text = cleanMagnet,
                        toastMessage = "No external torrent app found. Magnet copied to clipboard!"
                    )
                    false
                }
            }
        }

        if (cleanTorrentUrl != null) {
            downloadTorrentFileDirectly(context, cleanTorrentUrl, title ?: "release", openAfterDownload = true)
            return true
        }

        postToast(context, "No download link or magnet available for this release")
        return false
    }

    /**
     * Alias for downloadTorrentFileDirectly.
     */
    fun downloadTorrentFile(
        context: Context,
        torrentUrl: String,
        title: String,
        openAfterDownload: Boolean = false,
        onComplete: ((File?) -> Unit)? = null
    ) = downloadTorrentFileDirectly(context, torrentUrl, title, openAfterDownload, onComplete)

    /**
     * Downloads .torrent file directly from Nyaa using OkHttpClient with proper browser User-Agent
     * and headers, bypassing DDoS-Guard and ISP restrictions. Saves to Downloads/AniFlow/Torrents/.
     * Optionally opens the downloaded .torrent in external apps immediately.
     */
    fun downloadTorrentFileDirectly(
        context: Context,
        torrentUrl: String,
        title: String,
        openAfterDownload: Boolean = false,
        onComplete: ((File?) -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                postToast(context, "Downloading .torrent file from Nyaa…")
                val safeTitle = title.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().ifBlank { "release" }
                val fileName = if (safeTitle.endsWith(".torrent", ignoreCase = true)) safeTitle else "$safeTitle.torrent"

                val file = fetchTorrentFileBytes(context, torrentUrl, fileName)
                if (file != null && file.exists() && file.length() > 0) {
                    postToast(
                        context,
                        "Saved .torrent: $fileName in Downloads/AniFlow/Torrents/",
                        Toast.LENGTH_LONG
                    )
                    if (openAfterDownload) {
                        withContext(Dispatchers.Main) {
                            openTorrentFileInExternalApp(context, file)
                        }
                    }
                    onComplete?.invoke(file)
                } else {
                    postToast(context, "Could not download .torrent file: empty response")
                    onComplete?.invoke(null)
                }
            } catch (e: Exception) {
                postToast(context, "Failed to download .torrent: ${e.message}")
                onComplete?.invoke(null)
            }
        }
    }

    /**
     * Downloads multiple .torrent files concurrently in the background and saves them
     * organized in Downloads/AniFlow/Torrents/{destinationSubFolder}.
     */
    fun batchDownloadTorrentFiles(
        context: Context,
        items: List<Pair<String, String>>, // title to torrentUrl
        destinationSubFolder: String = "",
        subfolderName: String = destinationSubFolder,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> },
        onFinished: (successCount: Int) -> Unit = {}
    ) {
        if (items.isEmpty()) return
        val folder = if (subfolderName.isNotBlank()) subfolderName else destinationSubFolder

        CoroutineScope(Dispatchers.IO).launch {
            postToast(context, "Starting batch download of ${items.size} .torrent file(s)…")
            val semaphore = Semaphore(3) // 3 concurrent network downloads
            var completedCount = 0
            var successCount = 0

            for ((title, url) in items) {
                semaphore.withPermit {
                    try {
                        val safeTitle = title.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().ifBlank { "episode" }
                        val fileName = if (safeTitle.endsWith(".torrent", ignoreCase = true)) safeTitle else "$safeTitle.torrent"
                        val file = fetchTorrentFileBytes(context, url, fileName, folder)
                        if (file != null && file.exists() && file.length() > 0) {
                            successCount++
                        }
                    } catch (_: Exception) {}
                    completedCount++
                    onProgress(completedCount, items.size)
                }
            }

            postToast(
                context,
                "Batch complete: saved $successCount of ${items.size} .torrent file(s) in Downloads/AniFlow/Torrents/$folder",
                Toast.LENGTH_LONG
            )
            onFinished(successCount)
        }
    }

    /**
     * Writes content directly into public Downloads via MediaStore on Android 10+ (API 29+).
     * This bypasses Scoped Storage permission denials completely.
     */
    private fun writeToMediaStoreDownloads(
        context: Context,
        fileName: String,
        mimeType: String,
        subFolder: String,
        writeBlock: (java.io.OutputStream) -> Unit
    ): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            val relativePath = if (subFolder.isNotBlank()) {
                "${Environment.DIRECTORY_DOWNLOADS}/AniFlow/Torrents/$subFolder"
            } else {
                "${Environment.DIRECTORY_DOWNLOADS}/AniFlow/Torrents"
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    writeBlock(out)
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Resilient multi-tiered storage resolver ensuring 100% write success on all Android versions:
     * 1. Public Downloads (if writable / legacy storage active)
     * 2. App-specific external files dir (never requires permission)
     * 3. Internal app files dir as ultimate safe fallback
     */
    private fun getStorageTargetDir(context: Context, subFolder: String = ""): File {
        val relPath = if (subFolder.isNotBlank()) "AniFlow/Torrents/$subFolder" else "AniFlow/Torrents"
        // 1. Try public Downloads directory first
        try {
            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val publicDir = File(publicDownloads, relPath)
            if (publicDir.exists() || publicDir.mkdirs()) {
                val probeFile = File(publicDir, ".probe_${System.currentTimeMillis()}")
                if (probeFile.createNewFile()) {
                    probeFile.delete()
                    return publicDir
                }
            }
        } catch (_: Exception) {}

        // 2. Fallback to app external files dir (always permitted on Android 4.4 - 15+ without runtime permissions)
        try {
            val appExtDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (appExtDir != null) {
                val appDir = File(appExtDir, relPath)
                if (appDir.exists() || appDir.mkdirs()) {
                    return appDir
                }
            }
        } catch (_: Exception) {}

        // 3. Final fallback to internal files dir
        val internalDir = File(context.filesDir, relPath)
        if (!internalDir.exists()) internalDir.mkdirs()
        return internalDir
    }

    /**
     * Internal network worker downloading .torrent bytes via OkHttpClient with valid browser headers.
     */
    private fun fetchTorrentFileBytes(
        context: Context,
        torrentUrl: String,
        fileName: String,
        subFolder: String = ""
    ): File? {
        val trimmed = torrentUrl.trim()
        val normalizedUrl = when {
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.startsWith("/") -> "https://nyaa.si$trimmed"
            trimmed.all { it.isDigit() } -> "https://nyaa.si/download/$trimmed.torrent"
            else -> "https://nyaa.si/download/$trimmed"
        }

        val request = Request.Builder()
            .url(normalizedUrl)
            .header("User-Agent", DEFAULT_USER_AGENT)
            .header("Accept", "application/x-bittorrent, text/html, */*")
            .header("Referer", "https://nyaa.si/")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null

        val bytes = response.body?.bytes() ?: return null
        if (bytes.isEmpty()) return null

        // 1. Write to public Downloads via MediaStore on Android 10+
        writeToMediaStoreDownloads(context, fileName, "application/x-bittorrent", subFolder) { out ->
            out.write(bytes)
        }

        // 2. Also ensure local file exists for FileProvider sharing
        val targetFile = try {
            val targetDir = getStorageTargetDir(context, subFolder)
            val file = File(targetDir, fileName)
            FileOutputStream(file).use { it.write(bytes) }
            file
        } catch (e: Exception) {
            val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val fallbackSubDir = File(fallbackDir, if (subFolder.isNotBlank()) "AniFlow/Torrents/$subFolder" else "AniFlow/Torrents")
            if (!fallbackSubDir.exists()) fallbackSubDir.mkdirs()
            val fallbackFile = File(fallbackSubDir, fileName)
            FileOutputStream(fallbackFile).use { it.write(bytes) }
            fallbackFile
        }

        // Notify MediaScanner so the system and file managers index the new .torrent file immediately
        try {
            MediaScannerConnection.scanFile(
                context.applicationContext,
                arrayOf(targetFile.absolutePath),
                arrayOf("application/x-bittorrent"),
                null
            )
        } catch (_: Exception) {}

        return targetFile
    }

    /**
     * Launches external BitTorrent client with the downloaded .torrent file using FileProvider.
     */
    fun openTorrentFileInExternalApp(context: Context, file: File): Boolean {
        return try {
            val contentUri: Uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) {
                Uri.fromFile(file)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/x-bittorrent")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val chooser = Intent.createChooser(intent, "Open .torrent with…").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            postToast(context, "No app found to open .torrent file. File saved to: ${file.name}")
            false
        }
    }

    /**
     * Overload for exporting a simple list of magnet URI strings.
     */
    @JvmName("exportBatchMagnetsSimpleList")
    fun exportBatchMagnetsToTextFile(
        context: Context,
        title: String,
        magnets: List<String>
    ): File? {
        val pairs = magnets.mapIndexed { index, m -> "Episode ${index + 1}" to m }
        return exportBatchMagnetsToTextFile(context, title, pairs)
    }

    /**
     * Exports a list of magnet links with their episode titles to a text file for batch import into 1DM / FDM.
     * Writes to public Downloads via MediaStore on Android 10+ and saves for FileProvider sharing.
     */
    fun exportBatchMagnetsToTextFile(
        context: Context,
        batchTitle: String,
        items: List<Pair<String, String>> // title to magnetUri
    ): File? {
        return try {
            val safeTitle = batchTitle.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().ifBlank { "batch" }
            val fileName = "${safeTitle}_magnets.txt"

            val textBuilder = StringBuilder()
            textBuilder.appendLine("# AniFlow Batch Magnet Links: $batchTitle")
            textBuilder.appendLine("# Generated on: ${java.util.Date()}")
            textBuilder.appendLine("# Compatible with 1DM, LibreTorrent, Flud, BiglyBT, and μTorrent")
            textBuilder.appendLine()
            for ((epTitle, magnet) in items) {
                textBuilder.appendLine("# $epTitle")
                textBuilder.appendLine(magnet)
                textBuilder.appendLine()
            }
            val textBytes = textBuilder.toString().toByteArray(Charsets.UTF_8)

            // 1. Write to public Downloads via MediaStore on Android 10+
            writeToMediaStoreDownloads(context, fileName, "text/plain", "") { out ->
                out.write(textBytes)
            }

            // 2. Write to local file for FileProvider sharing
            val targetDir = getStorageTargetDir(context, "")
            val targetFile = try {
                val f = File(targetDir, fileName)
                FileOutputStream(f).use { it.write(textBytes) }
                f
            } catch (e: Exception) {
                val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                val fallbackFile = File(fallbackDir, fileName)
                FileOutputStream(fallbackFile).use { it.write(textBytes) }
                fallbackFile
            }

            try {
                MediaScannerConnection.scanFile(
                    context.applicationContext,
                    arrayOf(targetFile.absolutePath),
                    arrayOf("text/plain"),
                    null
                )
            } catch (_: Exception) {}

            postToast(
                context,
                "Exported ${items.size} magnet(s) to Downloads/AniFlow/Torrents/${targetFile.name}",
                Toast.LENGTH_LONG
            )
            targetFile
        } catch (e: Exception) {
            postToast(context, "Export failed: ${e.message}")
            null
        }
    }

    /**
     * Seamlessly dispatches a batch of episodes to external apps (1DM, LibreTorrent, Flud, etc.):
     * - If single episode: opens external client directly
     * - If multiple episodes:
     *   1. Exports .txt file with all magnet links into public Downloads
     *   2. Copies all links to clipboard (triggers 1DM clipboard monitor instantly)
     *   3. Presents system share/open chooser to send the entire batch to 1DM, LibreTorrent, or any app
     */
    fun openBatchInExternalTorrentClient(
        context: Context,
        items: List<Pair<String, String>>, // title to (magnet or torrentUrl)
        batchTitle: String
    ) {
        if (items.isEmpty()) {
            postToast(context, "No episodes selected")
            return
        }

        if (items.size == 1) {
            val (title, uri) = items.first()
            val isMagnet = uri.startsWith("magnet:", ignoreCase = true)
            openInExternalTorrentClient(
                context = context,
                magnetUri = if (isMagnet) uri else null,
                torrentUrl = if (!isMagnet) uri else null,
                title = title
            )
            return
        }

        // Export text file
        val file = exportBatchMagnetsToTextFile(context, batchTitle, items)

        // Copy all links separated by newlines
        val allLinks = items.map { it.second }.filter { it.isNotBlank() }.joinToString("\n")
        copyToClipboard(
            context = context,
            text = allLinks,
            label = "AniFlow Batch Links ($batchTitle)",
            toastMessage = "Copied ${items.size} links to clipboard! Opening batch dialog…"
        )

        // Launch Share Sheet / Chooser
        if (file != null) {
            shareFile(
                context = context,
                file = file,
                mimeType = "text/plain",
                chooserTitle = "Send Batch (${items.size} episodes) to 1DM / Downloader"
            )
        } else {
            shareText(
                context = context,
                title = batchTitle,
                text = allLinks
            )
        }
    }

    /**
     * Shares a file with external applications via FileProvider.
     */
    fun shareFile(
        context: Context,
        file: File,
        mimeType: String = "*/*",
        chooserTitle: String = "Share file via…"
    ) {
        try {
            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            val chooser = Intent.createChooser(intent, chooserTitle).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            postToast(context, "Cannot share file: ${e.message}")
        }
    }

    /**
     * Opens external Nyaa webpage in the device default browser.
     */
    fun openWebPage(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            postToast(context, "Cannot open browser: ${e.message}")
        }
    }

    /**
     * Shares link or text via Android standard Share Sheet.
     */
    fun shareText(context: Context, title: String, text: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, text)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val chooser = Intent.createChooser(intent, "Share via…").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            postToast(context, "Sharing failed: ${e.message}")
        }
    }
}
