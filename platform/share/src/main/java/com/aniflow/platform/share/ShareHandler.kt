package com.aniflow.platform.share

import android.content.Intent
import android.net.Uri

sealed interface SharedContent {
    data class MagnetUri(val uri: String) : SharedContent
    data class TorrentFile(val fileUri: Uri) : SharedContent
    data class DirectUrl(val url: String) : SharedContent
    data class SearchText(val text: String) : SharedContent
}

interface ShareHandler {
    fun parseSharedIntent(intent: Intent): SharedContent?
}

class AndroidShareHandler : ShareHandler {

    override fun parseSharedIntent(intent: Intent): SharedContent? {
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_VIEW == action) {
            val data = intent.dataString ?: return null
            return when {
                data.startsWith("magnet:", ignoreCase = true) -> SharedContent.MagnetUri(data)
                data.endsWith(".torrent", ignoreCase = true) -> SharedContent.DirectUrl(data)
                else -> SharedContent.DirectUrl(data)
            }
        }

        if (Intent.ACTION_SEND == action) {
            if ("text/plain" == type) {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim() ?: return null
                return when {
                    text.startsWith("magnet:", ignoreCase = true) -> SharedContent.MagnetUri(text)
                    text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true) ->
                        SharedContent.DirectUrl(text)
                    else -> SharedContent.SearchText(text)
                }
            } else if (type?.contains("torrent") == true || intent.data?.path?.endsWith(".torrent") == true) {
                val streamUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: intent.data
                if (streamUri != null) {
                    return SharedContent.TorrentFile(streamUri)
                }
            }
        }

        return null
    }
}
