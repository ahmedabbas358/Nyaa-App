package com.aniflow.domain.intelligence.model

import com.aniflow.domain.valueobject.InfoHash

data class ReleaseStats(
    val seeders: Int = 0,
    val leechers: Int = 0,
    val downloads: Int = 0
)

data class MagnetLink(
    val value: String
)

data class ReleaseLinks(
    val magnetUri: MagnetLink? = null,
    val infoHash: InfoHash? = null
)

data class FileInfo(
    val sizeBytes: Long? = null,
    val fileCount: Int = 1
)

data class ReleaseSource(
    val title: String,
    val providerName: String = "Nyaa",
    val pageUrl: String? = null
)

data class EpisodeRangeMatch(
    val from: Double,
    val to: Double
)

data class EpisodeMatch(
    val confidence: Float = 1.0f,
    val detectedEpisode: Double = 1.0,
    val range: EpisodeRangeMatch = EpisodeRangeMatch(1.0, 1.0)
)

data class AudioTrack(
    val codec: String,
    val language: String? = null
)

data class SubtitleTrack(
    val language: String,
    val isDefault: Boolean = false
)
