package com.aniflow.provider.nyaa.mapper

import com.aniflow.domain.valueobject.UrlValue
import com.aniflow.provider.core.model.ProviderCategory
import com.aniflow.provider.core.model.ProviderIdentity
import com.aniflow.provider.core.model.ProviderRelease
import com.aniflow.provider.nyaa.model.NyaaCategory
import com.aniflow.provider.nyaa.model.NyaaDetailsDto
import com.aniflow.provider.nyaa.model.NyaaReleaseDto
import java.time.Instant

/**
 * Maps Nyaa DTOs to normalized ProviderRelease models (Sections 48, 49, 93).
 */
class NyaaMapper {

    fun toProviderRelease(dto: NyaaReleaseDto): ProviderRelease {
        val category = mapCategory(dto.categoryCode, dto.categoryName)
        val uploader = dto.uploaderName?.let {
            ProviderIdentity(providerId = null, displayName = it)
        }

        val publishedAt = dto.timestampSeconds?.let { Instant.ofEpochSecond(it) }

        val rawMeta = mutableMapOf<String, String>()
        dto.categoryCode?.let { rawMeta["nyaa_category_code"] = it }
        dto.categoryName?.let { rawMeta["nyaa_category_name"] = it }
        dto.sizeDisplay?.let { rawMeta["nyaa_size_display"] = it }
        dto.completedDownloads?.let { rawMeta["nyaa_completed_downloads"] = it.toString() }
        rawMeta["nyaa_is_trusted"] = dto.isTrusted.toString()
        rawMeta["nyaa_is_remake"] = dto.isRemake.toString()

        return ProviderRelease(
            providerReleaseId = dto.id,
            title = dto.title,
            detailsUrl = dto.viewUrl?.let { UrlValue.parse(it) },
            torrentUrl = dto.torrentUrl?.let { UrlValue.parse(it) },
            magnetUri = dto.magnetUri,
            sizeBytes = dto.sizeBytes,
            seeders = dto.seeders,
            leechers = dto.leechers,
            downloads = dto.completedDownloads,
            publishedAt = publishedAt,
            category = category,
            uploader = uploader,
            releaseGroup = null, // Extracted in Step 6 Release Parser
            isTrusted = dto.isTrusted,
            isRemake = dto.isRemake,
            description = null,
            rawMetadata = rawMeta
        )
    }

    fun toProviderRelease(dto: NyaaDetailsDto): ProviderRelease {
        val category = mapCategory(dto.categoryCode, dto.categoryName)
        val uploader = dto.uploaderName?.let {
            ProviderIdentity(
                providerId = dto.uploaderUrl?.substringAfterLast("/user/"),
                displayName = it
            )
        }

        val publishedAt = dto.timestampSeconds?.let { Instant.ofEpochSecond(it) }

        val rawMeta = mutableMapOf<String, String>()
        dto.categoryCode?.let { rawMeta["nyaa_category_code"] = it }
        dto.categoryName?.let { rawMeta["nyaa_category_name"] = it }
        dto.sizeDisplay?.let { rawMeta["nyaa_size_display"] = it }
        dto.infoHash?.let { rawMeta["nyaa_info_hash"] = it }
        dto.completedDownloads?.let { rawMeta["nyaa_completed_downloads"] = it.toString() }
        rawMeta["nyaa_comments_count"] = dto.commentsCount.toString()

        return ProviderRelease(
            providerReleaseId = dto.id,
            title = dto.title,
            detailsUrl = UrlValue.parse("https://nyaa.si/view/${dto.id}"),
            torrentUrl = dto.torrentUrl?.let { UrlValue.parse(it) },
            magnetUri = dto.magnetUri,
            sizeBytes = dto.sizeBytes,
            seeders = dto.seeders,
            leechers = dto.leechers,
            downloads = dto.completedDownloads,
            publishedAt = publishedAt,
            category = category,
            uploader = uploader,
            releaseGroup = null,
            isTrusted = false, // Full details page doesn't mark row color
            isRemake = false,
            description = dto.descriptionMarkdown ?: dto.descriptionHtml,
            rawMetadata = rawMeta
        )
    }

    private fun mapCategory(code: String?, name: String?): ProviderCategory {
        if (code != null) {
            val nyaaCat = NyaaCategory.fromCode(code)
            return when (nyaaCat) {
                NyaaCategory.All -> ProviderCategory.All
                NyaaCategory.AnimeAll -> ProviderCategory.Anime.AllAnime
                NyaaCategory.AnimeMusicVideo -> ProviderCategory.Anime.MusicVideo
                NyaaCategory.AnimeEnglish -> ProviderCategory.Anime.EnglishTranslated
                NyaaCategory.AnimeNonEnglish -> ProviderCategory.Anime.NonEnglishTranslated
                NyaaCategory.AnimeRaw -> ProviderCategory.Anime.Raw
                NyaaCategory.NonEnglishAll -> ProviderCategory.Anime.NonEnglishTranslated
                NyaaCategory.AudioAll -> ProviderCategory.NonAnime.Audio
                NyaaCategory.LiteratureAll -> ProviderCategory.NonAnime.Manga
                NyaaCategory.LiveActionAll -> ProviderCategory.NonAnime.LiveAction
                NyaaCategory.PicturesAll -> ProviderCategory.NonAnime.Pictures
                NyaaCategory.SoftwareAll -> ProviderCategory.NonAnime.Software
            }
        }
        return ProviderCategory.All
    }
}
