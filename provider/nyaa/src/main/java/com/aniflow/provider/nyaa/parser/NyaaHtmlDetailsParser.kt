package com.aniflow.provider.nyaa.parser

import com.aniflow.provider.core.error.ProviderError
import com.aniflow.provider.nyaa.model.NyaaDetailsDto
import org.jsoup.Jsoup

/**
 * Parses enriched details from Nyaa's release page `/view/{id}` (Sections 38, 39, 91).
 */
class NyaaHtmlDetailsParser(
    private val baseUrl: String = "https://nyaa.si"
) {

    companion object {
        const val VERSION = "1.0.0"
    }

    fun parse(html: String, releaseId: String): NyaaDetailsDto {
        if (html.isBlank()) {
            throw ProviderError.InvalidResponse(0, "Received empty HTML for release $releaseId")
        }

        val document = try {
            Jsoup.parse(html, baseUrl)
        } catch (e: Exception) {
            throw ProviderError.ParserFailure("Failed to parse details HTML: ${e.message}", e)
        }

        // Title
        val titleEl = document.selectFirst(".panel-heading h3.panel-title")
            ?: document.selectFirst("h3.panel-title")
        val title = titleEl?.text()?.trim()
            ?: throw ProviderError.ParserStructureChanged("Release title element not found on view page")

        // Submitter / Uploader
        val uploaderLink = document.selectFirst(".panel-body a[href*=\"/user/\"]")
        val uploaderName = uploaderLink?.text()?.trim()
        val uploaderUrl = uploaderLink?.attr("href")?.let {
            if (it.startsWith("http")) it else "$baseUrl$it"
        }

        // Category
        val categoryLink = document.selectFirst(".panel-body a[href*=\"c=\"]")
        val categoryCode = categoryLink?.attr("href")?.substringAfter("c=")?.substringBefore('&')
        val categoryName = categoryLink?.text()?.trim()

        // Timestamp
        val dateEl = document.selectFirst(".panel-body [data-timestamp]")
        val timestampSeconds = dateEl?.attr("data-timestamp")?.toLongOrNull()

        // Seeders, Leechers, Completed
        var seeders: Int? = null
        var leechers: Int? = null
        var completed: Long? = null
        var sizeBytes: Long? = null
        var sizeDisplay: String? = null
        var infoHash: String? = null

        val rows = document.select(".panel-body .row")
        for (row in rows) {
            val text = row.text()
            when {
                text.contains("Seeders:", ignoreCase = true) -> {
                    seeders = NyaaParserSupport.safeToInt(row.select(".col-md-5, .col-md-1").lastOrNull()?.text())
                }
                text.contains("Leechers:", ignoreCase = true) -> {
                    leechers = NyaaParserSupport.safeToInt(row.select(".col-md-5, .col-md-1").lastOrNull()?.text())
                }
                text.contains("Completed:", ignoreCase = true) -> {
                    completed = NyaaParserSupport.safeToLong(row.select(".col-md-5, .col-md-1").lastOrNull()?.text())
                }
                text.contains("File size:", ignoreCase = true) -> {
                    val sizeText = row.select(".col-md-5").firstOrNull()?.text()?.trim()
                    sizeDisplay = sizeText
                    sizeBytes = NyaaParserSupport.parseSizeBytes(sizeText)
                }
                text.contains("Info hash:", ignoreCase = true) -> {
                    infoHash = row.select("kbd").text().trim().ifBlank { null }
                }
            }
        }

        // Torrent & Magnet Links
        var torrentUrl: String? = null
        var magnetUri: String? = null

        val downloadLinks = document.select(".panel-footer a, a.card-footer-item")
        for (a in downloadLinks) {
            val href = a.attr("href")
            when {
                href.startsWith("magnet:?", ignoreCase = true) -> magnetUri = href
                href.contains(".torrent", ignoreCase = true) -> {
                    torrentUrl = if (href.startsWith("http")) href else "$baseUrl$href"
                }
            }
        }

        // If infoHash was not found in table, extract from magnet
        if (infoHash == null && magnetUri != null) {
            infoHash = NyaaParserSupport.parseMagnet(magnetUri)?.infoHash?.hexString
        }

        // Description
        val descEl = document.selectFirst("#torrent-description")
        val descriptionHtml = descEl?.html()
        val descriptionMarkdown = descEl?.text()

        // Comments
        val commentsCount = document.select("#comments .comment-panel, .comment-panel").size

        return NyaaDetailsDto(
            id = releaseId,
            title = title,
            categoryCode = categoryCode,
            categoryName = categoryName,
            uploaderName = uploaderName,
            uploaderUrl = uploaderUrl,
            timestampSeconds = timestampSeconds,
            sizeBytes = sizeBytes,
            sizeDisplay = sizeDisplay,
            seeders = seeders,
            leechers = leechers,
            completedDownloads = completed,
            infoHash = infoHash,
            magnetUri = magnetUri,
            torrentUrl = torrentUrl,
            descriptionHtml = descriptionHtml,
            descriptionMarkdown = descriptionMarkdown,
            commentsCount = commentsCount
        )
    }
}
