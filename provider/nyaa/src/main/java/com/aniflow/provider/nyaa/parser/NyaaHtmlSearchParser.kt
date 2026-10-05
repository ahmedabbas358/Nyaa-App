package com.aniflow.provider.nyaa.parser

import com.aniflow.provider.core.error.ProviderError
import com.aniflow.provider.nyaa.model.NyaaReleaseDto
import com.aniflow.provider.nyaa.model.NyaaSearchPageDto
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * High-performance HTML search table parser for Nyaa (Sections 37, 39, 78, 79).
 * Extracts tabular release metadata from Nyaa's search response.
 */
class NyaaHtmlSearchParser(
    private val baseUrlProvider: () -> String = { "https://nyaa.si" }
) {
    constructor(baseUrl: String) : this({ baseUrl })

    private val baseUrl: String
        get() = baseUrlProvider().trimEnd('/')

    companion object {
        const val VERSION = "1.0.0"
    }

    fun parse(html: String, requestedPage: Int = 1): NyaaSearchPageDto {
        if (html.isBlank()) {
            throw ProviderError.InvalidResponse(0, "Received empty HTML response from provider")
        }

        val document = try {
            Jsoup.parse(html, baseUrl)
        } catch (e: Exception) {
            throw ProviderError.ParserFailure("HTML parsing failed: ${e.message}", e)
        }

        val table = document.selectFirst("table.torrent-list") ?: document.selectFirst("table")
        if (table == null) {
            // Check if page legitimately has zero results
            val isZeroResults = document.body().text().contains("No results found", ignoreCase = true) ||
                    document.selectFirst("div.container") != null && document.select("table").isEmpty()

            if (isZeroResults) {
                return NyaaSearchPageDto(
                    releases = emptyList(),
                    currentPage = requestedPage,
                    hasNextPage = false,
                    totalResultsEstimate = 0L
                )
            } else {
                throw ProviderError.ParserStructureChanged(
                    "Table 'table.torrent-list' or 'table' not found and no 'No results found' indicator present"
                )
            }
        }

        val rows = table.select("tbody tr")
        val releases = mutableListOf<NyaaReleaseDto>()

        for (row in rows) {
            parseRow(row)?.let { releases.add(it) }
        }

        val hasNextPage = detectHasNextPage(document, requestedPage)

        return NyaaSearchPageDto(
            releases = releases,
            currentPage = requestedPage,
            hasNextPage = hasNextPage,
            totalResultsEstimate = null
        )
    }

    private fun parseRow(row: Element): NyaaReleaseDto? {
        val cells = row.select("td")
        if (cells.size < 8) return null

        val rowClass = row.className()
        val isTrusted = rowClass.contains("success")
        val isRemake = rowClass.contains("danger")

        // Cell 0: Category
        val categoryEl = cells[0].selectFirst("a")
        val categoryCode = categoryEl?.attr("href")?.substringAfter("c=")?.substringBefore('&')
        val categoryName = categoryEl?.attr("title")?.ifBlank { null } ?: cells[0].text().trim()

        // Cell 1: Title & View Link
        val nameCell = cells[1]
        val links = nameCell.select("a")
        val titleLink = links.lastOrNull { !it.attr("href").contains("#comments") } ?: links.lastOrNull() ?: return null
        val title = titleLink.text().trim()
        if (title.isBlank()) return null

        val viewHref = titleLink.attr("href")
        val id = viewHref.substringAfterLast("/view/").substringBefore('#').substringBefore('?')
            .ifBlank { title.hashCode().toString() }
        val viewUrl = if (viewHref.startsWith("http")) viewHref else "$baseUrl$viewHref"

        // Cell 2: Download Links (Torrent & Magnet)
        val linksCell = cells[2]
        var torrentUrl: String? = null
        var magnetUri: String? = null

        for (a in linksCell.select("a")) {
            val href = a.attr("href")
            when {
                href.startsWith("magnet:?", ignoreCase = true) -> magnetUri = href
                href.contains("/download/", ignoreCase = true) || href.contains(".torrent", ignoreCase = true) -> {
                    torrentUrl = if (href.startsWith("http")) href else "$baseUrl$href"
                }
            }
        }

        if (torrentUrl == null && id.isNotBlank() && id.all { it.isDigit() }) {
            torrentUrl = "$baseUrl/download/$id.torrent"
        }

        // Cell 3: File Size
        val sizeText = cells[3].text().trim()
        val sizeBytes = NyaaParserSupport.parseSizeBytes(sizeText)

        // Cell 4: Timestamp / Date
        val dateCell = cells[4]
        val timestampStr = dateCell.attr("data-timestamp")
        val timestampSeconds = timestampStr.toLongOrNull()

        // Cell 5, 6, 7: Seeders, Leechers, Downloads (Sections 26, 28)
        val seeders = NyaaParserSupport.safeToInt(cells[5].text())
        val leechers = NyaaParserSupport.safeToInt(cells[6].text())
        val downloads = NyaaParserSupport.safeToLong(cells[7].text())

        val commentsLink = nameCell.selectFirst("a.comments") ?: nameCell.selectFirst("a[href*='#comments']")
        val commentsCount = commentsLink?.text()?.trim()?.toIntOrNull()

        val isBatch = title.contains("Batch", ignoreCase = true) ||
                title.contains("01-", ignoreCase = true) ||
                title.contains("01~", ignoreCase = true) ||
                title.contains("01 - ", ignoreCase = true) ||
                title.contains("Complete", ignoreCase = true)

        val isHidden = rowClass.contains("hidden")

        // InfoHash if extractable from magnet
        val infoHash = magnetUri?.let { NyaaParserSupport.parseMagnet(it)?.infoHash?.hexString }

        return NyaaReleaseDto(
            id = id,
            title = title,
            categoryCode = categoryCode,
            categoryName = categoryName,
            viewUrl = viewUrl,
            torrentUrl = torrentUrl,
            magnetUri = magnetUri,
            sizeBytes = sizeBytes,
            sizeDisplay = sizeText,
            timestampSeconds = timestampSeconds,
            seeders = seeders,
            leechers = leechers,
            completedDownloads = downloads,
            isTrusted = isTrusted,
            isRemake = isRemake,
            isBatch = isBatch,
            isHidden = isHidden,
            commentsCount = commentsCount,
            uploaderName = null,
            infoHash = infoHash
        )
    }

    private fun detectHasNextPage(document: Document, currentPage: Int): Boolean {
        val pagination = document.selectFirst("ul.pagination") ?: return false

        val nextLi = pagination.selectFirst("li.next")
        if (nextLi != null && !nextLi.className().contains("disabled")) {
            return true
        }

        val nextLink = pagination.selectFirst("a[rel=next]")
        if (nextLink != null) {
            return true
        }

        // Check if there is an explicit link to currentPage + 1
        val nextPageLink = pagination.selectFirst("a[href*=\"p=${currentPage + 1}\"]")
        return nextPageLink != null
    }
}
