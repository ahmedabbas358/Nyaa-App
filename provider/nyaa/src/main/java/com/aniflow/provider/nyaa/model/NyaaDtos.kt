package com.aniflow.provider.nyaa.model

/**
 * Raw data transfer object representing a release row from Nyaa HTML or RSS (Section 17, 37).
 */
data class NyaaReleaseDto(
    val id: String,
    val title: String,
    val categoryCode: String?,
    val categoryName: String?,
    val viewUrl: String?,
    val torrentUrl: String?,
    val magnetUri: String?,
    val sizeBytes: Long?,
    val sizeDisplay: String?,
    val timestampSeconds: Long?,
    val seeders: Int?,
    val leechers: Int?,
    val completedDownloads: Long?,
    val isTrusted: Boolean = false,
    val isRemake: Boolean = false,
    val uploaderName: String? = null,
    val infoHash: String? = null
)

/**
 * Enriched details DTO parsed from Nyaa release view page `/view/{id}` (Section 38).
 */
data class NyaaDetailsDto(
    val id: String,
    val title: String,
    val categoryCode: String?,
    val categoryName: String?,
    val uploaderName: String?,
    val uploaderUrl: String?,
    val timestampSeconds: Long?,
    val sizeBytes: Long?,
    val sizeDisplay: String?,
    val seeders: Int?,
    val leechers: Int?,
    val completedDownloads: Long?,
    val infoHash: String?,
    val magnetUri: String?,
    val torrentUrl: String?,
    val descriptionHtml: String?,
    val descriptionMarkdown: String?,
    val commentsCount: Int = 0
)

/**
 * Page response DTO from Nyaa search parser.
 */
data class NyaaSearchPageDto(
    val releases: List<NyaaReleaseDto>,
    val currentPage: Int,
    val hasNextPage: Boolean,
    val totalResultsEstimate: Long? = null
)
