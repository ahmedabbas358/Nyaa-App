package com.aniflow.core.ui.util

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import java.io.File

/**
 * High-performance Platform Bridge for external BitTorrent clients,
 * native DownloadManager for .torrent files, clipboard operations, and web links.
 * Facilitates seamless integration with apps like 1DM, LibreTorrent, Flud, μTorrent, etc.
 */
object TorrentClientBridge {

    /**
     * Copies magnet link or text to system clipboard and displays clean user feedback.
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
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
    }

    /**
     * Dispatches magnet link or .torrent URI to external BitTorrent client (e.g. 1DM, LibreTorrent, Flud).
     * If an external client exists, launches standard chooser; otherwise safely copies magnet and advises the user.
     */
    fun openInExternalTorrentClient(
        context: Context,
        magnetUri: String,
        title: String? = null
    ): Boolean {
        return try {
            val uri = Uri.parse(magnetUri)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val chooser = Intent.createChooser(intent, "Download with...").apply {
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

    /**
     * Downloads .torrent file directly from Nyaa (e.g. https://nyaa.si/download/12345.torrent)
     * using the Android system DownloadManager into Downloads/AniFlow/Torrents/.
     */
    fun downloadTorrentFileDirectly(
        context: Context,
        torrentUrl: String,
        title: String
    ): Boolean {
        return try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                ?: return false

            val safeTitle = title.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().ifBlank { "release" }
            val fileName = "$safeTitle.torrent"

            val uri = Uri.parse(torrentUrl)
            val request = DownloadManager.Request(uri).apply {
                setTitle("Nyaa Torrent: $safeTitle")
                setDescription("Downloading .torrent file")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setMimeType("application/x-bittorrent")
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "AniFlow/Torrents/$fileName")
            }

            downloadManager.enqueue(request)
            Toast.makeText(context, "Downloading .torrent file to Downloads/AniFlow/Torrents/", Toast.LENGTH_LONG).show()
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Could not start .torrent download: ${e.message}", Toast.LENGTH_SHORT).show()
            false
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
            Toast.makeText(context, "Cannot open browser: ${e.message}", Toast.LENGTH_SHORT).show()
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
            val chooser = Intent.createChooser(intent, "Share via...").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Sharing failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
