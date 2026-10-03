package com.aniflow.domain.valueobject

/**
 * Value Object representing an ISO 639-1 / 639-2 language code or normalized language representation.
 */
@JvmInline
value class LanguageCode(val code: String) {
    init {
        require(code.isNotBlank()) { "LanguageCode cannot be blank" }
    }

    val isJapanese: Boolean get() = code.equals("ja", ignoreCase = true) || code.equals("jpn", ignoreCase = true) || code.equals("japanese", ignoreCase = true)
    val isEnglish: Boolean get() = code.equals("en", ignoreCase = true) || code.equals("eng", ignoreCase = true) || code.equals("english", ignoreCase = true)
    val isArabic: Boolean get() = code.equals("ar", ignoreCase = true) || code.equals("ara", ignoreCase = true) || code.equals("arabic", ignoreCase = true)
    val isMulti: Boolean get() = code.equals("multi", ignoreCase = true) || code.equals("dual", ignoreCase = true)

    val displayName: String get() = when {
        isEnglish -> "English"
        isJapanese -> "Japanese"
        isArabic -> "Arabic"
        isMulti -> "Multi"
        code.equals("fr", ignoreCase = true) || code.equals("fra", ignoreCase = true) || code.equals("french", ignoreCase = true) -> "French"
        code.equals("de", ignoreCase = true) || code.equals("deu", ignoreCase = true) || code.equals("ger", ignoreCase = true) || code.equals("german", ignoreCase = true) -> "German"
        code.equals("es", ignoreCase = true) || code.equals("spa", ignoreCase = true) || code.equals("spanish", ignoreCase = true) -> "Spanish"
        code.equals("it", ignoreCase = true) || code.equals("ita", ignoreCase = true) || code.equals("italian", ignoreCase = true) -> "Italian"
        else -> code
    }

    override fun toString(): String = code

    companion object {
        val JAPANESE = LanguageCode("ja")
        val ENGLISH = LanguageCode("en")
        val ARABIC = LanguageCode("ar")
        val FRENCH = LanguageCode("fr")
        val GERMAN = LanguageCode("de")
        val SPANISH = LanguageCode("es")
        val ITALIAN = LanguageCode("it")
        val MULTI = LanguageCode("multi")
    }
}

/**
 * Strongly-typed URI/URL representation across network protocols.
 */
sealed interface UrlValue {
    val rawValue: String

    data class HttpsUrl(override val rawValue: String) : UrlValue {
        init {
            require(rawValue.startsWith("https://", ignoreCase = true) || rawValue.startsWith("http://", ignoreCase = true)) {
                "HttpsUrl must start with http:// or https://"
            }
        }
    }

    data class MagnetUri(override val rawValue: String) : UrlValue {
        init {
            require(rawValue.startsWith("magnet:?", ignoreCase = true)) {
                "MagnetUri must start with magnet:?"
            }
        }

        val extractInfoHash: InfoHash?
            get() {
                val match = Regex("""xt=urn:btih:([a-fA-F0-9]{40}|[a-zA-Z2-7]{32})""", RegexOption.IGNORE_CASE)
                    .find(rawValue)
                return match?.groupValues?.get(1)?.let { InfoHash(it) }
            }
    }

    data class TorrentUrl(override val rawValue: String) : UrlValue {
        init {
            require(rawValue.endsWith(".torrent", ignoreCase = true) || rawValue.contains("torrent", ignoreCase = true)) {
                "TorrentUrl must point to a torrent file resource"
            }
        }
    }

    data class GenericUrl(override val rawValue: String) : UrlValue

    companion object {
        fun parse(url: String): UrlValue {
            val trimmed = url.trim()
            return when {
                trimmed.startsWith("magnet:?", ignoreCase = true) -> MagnetUri(trimmed)
                trimmed.endsWith(".torrent", ignoreCase = true) -> TorrentUrl(trimmed)
                trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> HttpsUrl(trimmed)
                else -> GenericUrl(trimmed)
            }
        }
    }
}

/**
 * Value Object representing a BitTorrent InfoHash (hexadecimal 40-character or base32 32-character).
 */
@JvmInline
value class InfoHash(val value: String) {
    init {
        val normalized = value.trim()
        require(normalized.length == 40 || normalized.length == 32 || normalized.length == 64) {
            "InfoHash length must be 32 (base32), 40 (v1 hex SHA-1), or 64 (v2 hex SHA-256): ${normalized.length}"
        }
    }

    val hexString: String get() = value.lowercase()

    override fun toString(): String = hexString
}

/**
 * Storage Target abstraction for download destinations.
 * Decoupled from Android Uri and File objects (Section 78).
 */
data class StorageTarget(
    val identifier: String,
    val displayName: String = identifier
) {
    init {
        require(identifier.isNotBlank()) { "StorageTarget identifier cannot be blank" }
        require(displayName.isNotBlank()) { "StorageTarget displayName cannot be blank" }
    }

    companion object {
        val DEFAULT = StorageTarget(
            identifier = "default_storage",
            displayName = "Internal / Default Storage"
        )
    }
}
