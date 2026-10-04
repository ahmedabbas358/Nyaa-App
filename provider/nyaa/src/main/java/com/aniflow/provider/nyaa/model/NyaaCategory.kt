package com.aniflow.provider.nyaa.model

import com.aniflow.provider.core.model.ProviderCategory

/**
 * Nyaa-specific category mapping enumeration (Sections 31, 32).
 */
enum class NyaaCategory(val code: String, val displayName: String) {
    All("0_0", "All categories"),

    // 1_0 Anime
    AnimeAll("1_0", "Anime"),
    AnimeMusicVideo("1_1", "Anime - Music Video"),
    AnimeEnglish("1_2", "Anime - English-translated"),
    AnimeNonEnglish("1_3", "Anime - Non-English-translated"),
    AnimeRaw("1_4", "Anime - Raw"),

    // 2_0 Audio
    AudioAll("2_0", "Audio"),
    AudioLossless("2_1", "Audio - Lossless"),
    AudioLossy("2_2", "Audio - Lossy"),

    // 3_0 Literature
    LiteratureAll("3_0", "Literature"),
    LiteratureEnglish("3_1", "Literature - English"),
    LiteratureNonEnglish("3_2", "Literature - Non-English"),
    LiteratureRaw("3_3", "Literature - Raw"),

    // 4_0 Live Action
    LiveActionAll("4_0", "Live Action"),
    LiveActionEnglish("4_1", "Live Action - English"),
    LiveActionIdolPromo("4_2", "Live Action - Idol/Promotional"),
    LiveActionNonEnglish("4_3", "Live Action - Non-English"),
    LiveActionRaw("4_4", "Live Action - Raw"),

    // 5_0 Pictures
    PicturesAll("5_0", "Pictures"),
    PicturesGraphics("5_1", "Pictures - Graphics"),
    PicturesPhotos("5_2", "Pictures - Photos"),

    // 6_0 Software
    SoftwareAll("6_0", "Software"),
    SoftwareApplications("6_1", "Software - Applications"),
    SoftwareGames("6_2", "Software - Games");

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
