package com.aniflow.provider.nyaa.parser

import com.aniflow.domain.valueobject.InfoHash
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Parsing helpers for Nyaa HTML/RSS normalization (Sections 40, 69, 70).
 */
object NyaaParserSupport {

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm [z][xxx]", Locale.US)
        .withZone(ZoneOffset.UTC)

    /**
     * Converts human-readable size string (e.g. "1.4 GiB", "750.5 MiB") to byte count.
     */
    fun parseSizeBytes(sizeString: String?): Long? {
        if (sizeString.isNullOrBlank()) return null
        val parts = sizeString.trim().split(Regex("""\s+"""))
        if (parts.isEmpty()) return null

        val value = parts[0].toDoubleOrNull() ?: return null
        if (parts.size == 1) return value.toLong()

        val unit = parts[1].uppercase(Locale.ROOT)
        val multiplier = when {
            unit.startsWith("T") -> 1024L * 1024L * 1024L * 1024L
            unit.startsWith("G") -> 1024L * 1024L * 1024L
            unit.startsWith("M") -> 1024L * 1024L
            unit.startsWith("K") -> 1024L
            unit.startsWith("B") -> 1L
            else -> 1L
        }

        return (value * multiplier).toLong()
    }

    /**
     * Parses and validates a Magnet URI (Section 69).
     * Extracts InfoHash, Display Name, and Trackers.
     */
    fun parseMagnet(magnetUri: String?): MagnetParsedData? {
        if (magnetUri.isNullOrBlank() || !magnetUri.startsWith("magnet:?", ignoreCase = true)) {
            return null
        }

        var infoHash: String? = null
        var displayName: String? = null
        val trackers = mutableListOf<String>()

        val query = magnetUri.substringAfter("magnet:?")
        val params = query.split('&')

        for (param in params) {
            val key = param.substringBefore('=').lowercase(Locale.ROOT)
            val rawVal = param.substringAfter('=', "")
            val decodedVal = try {
                URLDecoder.decode(rawVal, StandardCharsets.UTF_8.name())
            } catch (_: Exception) {
                rawVal
            }

            when (key) {
                "xt" -> {
                    val match = Regex("""urn:btih:([a-fA-F0-9]{40}|[a-zA-Z2-7]{32})""", RegexOption.IGNORE_CASE)
                        .find(decodedVal)
                    if (match != null) {
                        infoHash = match.groupValues[1].lowercase(Locale.ROOT)
                    }
                }
                "dn" -> displayName = decodedVal
                "tr" -> if (decodedVal.isNotBlank()) trackers.add(decodedVal)
            }
        }

        return if (infoHash != null) {
            MagnetParsedData(
                infoHash = InfoHash(infoHash),
                displayName = displayName,
                trackers = trackers,
                rawUri = magnetUri
            )
        } else null
    }

    /**
     * Parses timestamp string or epoch attribute into Instant.
     */
    fun parseDate(timestampSecStr: String?, textDate: String?): Instant? {
        timestampSecStr?.toLongOrNull()?.let { sec ->
            if (sec > 0) return Instant.ofEpochSecond(sec)
        }
        if (!textDate.isNullOrBlank()) {
            return try {
                val cleaned = textDate.trim().removeSuffix("UTC").trim()
                Instant.from(DATE_FORMATTER.parse(cleaned))
            } catch (_: Exception) {
                null
            }
        }
        return null
    }

    /**
     * Parses number string safely, stripping commas, dashes, and whitespace (Section 26).
     * Returns null if missing or unknown (Section 28).
     */
    fun safeToInt(text: String?): Int? {
        if (text.isNullOrBlank()) return null
        val cleaned = text.trim().replace(",", "").replace("—", "").replace("-", "")
        return cleaned.toIntOrNull()
    }

    /**
     * Parses 64-bit integer string safely.
     */
    fun safeToLong(text: String?): Long? {
        if (text.isNullOrBlank()) return null
        val cleaned = text.trim().replace(",", "").replace("—", "").replace("-", "")
        return cleaned.toLongOrNull()
    }

    data class MagnetParsedData(
        val infoHash: InfoHash,
        val displayName: String?,
        val trackers: List<String>,
        val rawUri: String
    )
}
