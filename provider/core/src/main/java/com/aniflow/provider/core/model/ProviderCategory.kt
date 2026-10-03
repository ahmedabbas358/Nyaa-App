package com.aniflow.provider.core.model

/**
 * Generic provider category hierarchy (Section 31 & 32).
 * Strictly provider-agnostic. Provider-specific categories (e.g. Nyaa 1_2)
 * map to/from this generic model.
 */
sealed interface ProviderCategory {
    val id: String
    val displayName: String

    object All : ProviderCategory {
        override val id: String = "all"
        override val displayName: String = "All Categories"
    }

    sealed class Anime(
        override val id: String,
        override val displayName: String
    ) : ProviderCategory {
        object AllAnime : Anime("anime_all", "Anime - All")
        object EnglishTranslated : Anime("anime_en", "Anime - English-translated")
        object NonEnglishTranslated : Anime("anime_non_en", "Anime - Non-English-translated")
        object Raw : Anime("anime_raw", "Anime - Raw")
        object MusicVideo : Anime("anime_amv", "Anime - Music Video")
    }

    sealed class NonAnime(
        override val id: String,
        override val displayName: String
    ) : ProviderCategory {
        object Manga : NonAnime("literature_manga", "Literature / Manga")
        object Audio : NonAnime("audio", "Audio / Music")
        object Pictures : NonAnime("pictures", "Pictures / Graphics")
        object LiveAction : NonAnime("live_action", "Live Action")
        object Software : NonAnime("software", "Software / Games")
    }

    data class Custom(
        override val id: String,
        override val displayName: String
    ) : ProviderCategory

    companion object {
        val ANIME_ENGLISH: ProviderCategory = Anime.EnglishTranslated
        val ANIME_NON_ENGLISH: ProviderCategory = Anime.NonEnglishTranslated
        val ANIME_RAW: ProviderCategory = Anime.Raw
        val ANIME_ALL: ProviderCategory = Anime.AllAnime
        val ALL: ProviderCategory = All
    }
}
