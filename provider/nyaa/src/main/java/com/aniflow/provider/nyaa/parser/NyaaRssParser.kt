package com.aniflow.provider.nyaa.parser

import com.aniflow.provider.core.error.ProviderError
import com.aniflow.provider.nyaa.model.NyaaReleaseDto
import com.aniflow.provider.nyaa.model.NyaaSearchPageDto
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Fallback RSS 2.0 XML Feed Parser for Nyaa (Sections 26, 27).
 * Used when HTML structure fails or lightweight discovery is requested.
 */
class NyaaRssParser {

    companion object {
        private val RSS_DATE_FORMAT = DateTimeFormatter.RFC_1123_DATE_TIME
    }

    fun parse(xml: String): NyaaSearchPageDto {
        if (xml.isBlank()) {
            throw ProviderError.InvalidResponse(0, "Received empty RSS feed XML")
        }

        val document = try {
            Jsoup.parse(xml, "", Parser.xmlParser())
        } catch (e: Exception) {
            throw ProviderError.ParserFailure("XML parsing failed: ${e.message}", e)
        }

        val items = document.select("item")
        val releases = mutableListOf<NyaaReleaseDto>()

        for (item in items) {
            val title = item.selectFirst("title")?.text()?.trim() ?: continue
            val link = item.selectFirst("link")?.text()?.trim()
            val guid = item.selectFirst("guid")?.text()?.trim() ?: link ?: title.hashCode().toString()
            val id = guid.substringAfterLast("/view/").substringBefore('?').substringBefore('#')

            val pubDateStr = item.selectFirst("pubDate")?.text()?.trim()
            val timestamp = pubDateStr?.let {
                try {
                    ZonedDateTime.parse(it, RSS_DATE_FORMAT).toEpochSecond()
                } catch (_: Exception) {
                    null
                }
            }

            val seeders = item.selectFirst("nyaa|seeders, seeders")?.text()?.toIntOrNull() ?: 0
            val leechers = item.selectFirst("nyaa|leechers, leechers")?.text()?.toIntOrNull() ?: 0
            val downloads = item.selectFirst("nyaa|downloads, downloads")?.text()?.toLongOrNull() ?: 0L
            val infoHash = item.selectFirst("nyaa|infoHash, infoHash")?.text()?.trim()?.ifBlank { null }
            val categoryId = item.selectFirst("nyaa|categoryId, categoryId")?.text()?.trim()
            val categoryName = item.selectFirst("nyaa|category, category")?.text()?.trim()
            val sizeStr = item.selectFirst("nyaa|size, size")?.text()?.trim()
            val sizeBytes = NyaaParserSupport.parseSizeBytes(sizeStr)

            // Construct magnet from infoHash if available
            val magnetUri = if (infoHash != null) {
                "magnet:?xt=urn:btih:$infoHash&dn=${title}"
            } else null

            val torrentUrl = if (id.isNotBlank()) "https://nyaa.si/download/$id.torrent" else null

            releases.add(
                NyaaReleaseDto(
                    id = id,
                    title = title,
                    categoryCode = categoryId,
                    categoryName = categoryName,
                    viewUrl = link ?: guid,
                    torrentUrl = torrentUrl,
                    magnetUri = magnetUri,
                    sizeBytes = sizeBytes,
                    sizeDisplay = sizeStr,
                    timestampSeconds = timestamp,
                    seeders = seeders,
                    leechers = leechers,
                    completedDownloads = downloads,
                    isTrusted = false,
                    isRemake = false,
                    uploaderName = null,
                    infoHash = infoHash
                )
            )
        }

        return NyaaSearchPageDto(
            releases = releases,
            currentPage = 1,
            hasNextPage = false, // RSS does not reliably support deep pagination
            totalResultsEstimate = releases.size.toLong()
        )
    }
}
