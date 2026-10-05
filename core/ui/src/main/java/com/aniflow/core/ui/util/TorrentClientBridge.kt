package com.aniflow.core.ui.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
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
     * Dispatches magnet link to external BitTorrent client (e.g. 1DM, LibreTorrent, Flud).
     * If an external client exists, launches standard chooser; otherwise safely copies magnet and advises the user.
     */
     */
    fun openInExternalTorrentClient(
        context: Context,
        magnetUri: String? = null,
        torrentUrl: String? = null,
        title: String? = null
    ): Boolean {
        if (!magnetUri.isNullOrBlank()) {
            return try {
                val uri = Uri.parse(magnetUri)
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

                val chooser = Intent.createChooser(intent, "Open with BitTorrent App (1DM / Flud / LibreTorrent)").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(chooser)
                true
            } catch (e: Exception) {
                copyToClipboard(
                    context = context,
                    text = magnetUri,
                    toastMessage = "No external torrent app found. Magnet copied to clipboard!"
                )
                false
            }
        }
        if (!torrentUrl.isNullOrBlank()) {
            downloadTorrentFileDirectly(context, torrentUrl, title ?: "release", openAfterDownload = true)
            return true
        }
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
     * Internal network worker downloading .torrent bytes via OkHttpClient with valid browser headers.
     */
    private fun fetchTorrentFileBytes(
        context: Context,
        torrentUrl: String,
        fileName: String,
        subFolder: String = ""
    ): File? {
        val request = Request.Builder()
            .url(torrentUrl)
            .header("User-Agent", DEFAULT_USER_AGENT)
            .header("Accept", "application/x-bittorrent, text/html, */*")
            .header("Referer", "https://nyaa.si/")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null

        val bytes = response.body?.bytes() ?: return null
        if (bytes.isEmpty()) return null

        val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetDir = if (subFolder.isNotBlank()) {
            File(publicDir, "AniFlow/Torrents/$subFolder")
        } else {
            File(publicDir, "AniFlow/Torrents")
        }

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val targetFile = File(targetDir, fileName)
        FileOutputStream(targetFile).use { it.write(bytes) }

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
     */
    fun exportBatchMagnetsToTextFile(
        context: Context,
        batchTitle: String,
        items: List<Pair<String, String>> // title to magnetUri
    ): File? {
        return try {
            val safeTitle = batchTitle.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().ifBlank { "batch" }
            val fileName = "${safeTitle}_magnets.txt"

            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val targetDir = File(publicDir, "AniFlow/Torrents")
            if (!targetDir.exists()) targetDir.mkdirs()

            val targetFile = File(targetDir, fileName)
            targetFile.printWriter().use { out ->
                out.println("# AniFlow Batch Magnet Links: $batchTitle")
                out.println("# Generated on: ${java.util.Date()}")
                out.println()
                for ((epTitle, magnet) in items) {
                    out.println("# $epTitle")
                    out.println(magnet)
                    out.println()
                }
            }

            postToast(
                context,
                "Exported ${items.size} magnet(s) to Downloads/AniFlow/Torrents/$fileName",
                Toast.LENGTH_LONG
            )
            targetFile
        } catch (e: Exception) {
            postToast(context, "Export failed: ${e.message}")
            null
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
