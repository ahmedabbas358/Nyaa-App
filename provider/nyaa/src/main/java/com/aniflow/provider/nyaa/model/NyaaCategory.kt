package com.aniflow.provider.nyaa.model

import com.aniflow.provider.core.model.ProviderCategory

/**
 * Nyaa-specific category mapping enumeration (Sections 31, 32).
 */
enum class NyaaCategory(val code: String, val displayName: String) {
    All("0_0", "All categories"),
    AnimeAll("1_0", "Anime"),
    AnimeMusicVideo("1_1", "Anime - Music Video"),
    AnimeEnglish("1_2", "Anime - English-translated"),
    AnimeNonEnglish("1_3", "Anime - Non-English-translated"),
    AnimeRaw("1_4", "Anime - Raw"),
    NonEnglishAll("2_0", "Non-English-translated"),
    AudioAll("3_0", "Audio"),
    LiteratureAll("4_0", "Literature"),
    LiveActionAll("5_0", "Live Action"),
    PicturesAll("6_0", "Pictures"),
    SoftwareAll("7_0", "Software");

    companion object {
        fun fromCode(code: String): NyaaCategory {
            return entries.firstOrNull { it.code == code } ?: All
        }

        fun fromProviderCategory(category: ProviderCategory?): NyaaCategory {
            return when (category) {
                null, ProviderCategory.All -> All
                ProviderCategory.Anime.AllAnime -> AnimeAll
                ProviderCategory.Anime.EnglishTranslated -> AnimeEnglish
                ProviderCategory.Anime.NonEnglishTranslated -> AnimeNonEnglish
                ProviderCategory.Anime.Raw -> AnimeRaw
                ProviderCategory.Anime.MusicVideo -> AnimeMusicVideo
                ProviderCategory.NonAnime.Manga -> LiteratureAll
                ProviderCategory.NonAnime.Audio -> AudioAll
                ProviderCategory.NonAnime.LiveAction -> LiveActionAll
                ProviderCategory.NonAnime.Pictures -> PicturesAll
                ProviderCategory.NonAnime.Software -> SoftwareAll
                is ProviderCategory.Custom -> fromCode(category.id)
            }
        }
    }
}
