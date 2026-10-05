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
        val extractedGroup = Regex("""^\[([^\]]+)\]""").find(dto.title)?.groupValues?.get(1)?.trim()?.takeIf {
            val lower = it.lowercase()
            !lower.contains("1080p") && !lower.contains("720p") && !lower.contains("480p") &&
            !lower.contains("hevc") && !lower.contains("x264") && !lower.contains("x265") &&
            !lower.contains("av1") && !lower.contains("aac") && !lower.contains("flac")
        }
        val uploader = dto.uploaderName?.let {
            ProviderIdentity(providerId = null, displayName = it)
        } ?: extractedGroup?.let {
            ProviderIdentity(providerId = null, displayName = it)
        }
        val releaseGroup = extractedGroup?.let {
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
        rawMeta["nyaa_is_batch"] = dto.isBatch.toString()
        rawMeta["nyaa_is_hidden"] = dto.isHidden.toString()
        dto.commentsCount?.let { rawMeta["nyaa_comments_count"] = it.toString() }
        if (extractedGroup != null) {
            rawMeta["nyaa_extracted_group"] = extractedGroup
        }

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
            releaseGroup = releaseGroup,
            isTrusted = dto.isTrusted,
            isRemake = dto.isRemake,
            description = null,
            rawMetadata = rawMeta
        )
    }

    fun toProviderRelease(dto: NyaaDetailsDto): ProviderRelease {
        val category = mapCategory(dto.categoryCode, dto.categoryName)
        val extractedGroup = Regex("""^\[([^\]]+)\]""").find(dto.title)?.groupValues?.get(1)?.trim()?.takeIf {
            val lower = it.lowercase()
            !lower.contains("1080p") && !lower.contains("720p") && !lower.contains("480p") &&
            !lower.contains("hevc") && !lower.contains("x264") && !lower.contains("x265") &&
            !lower.contains("av1") && !lower.contains("aac") && !lower.contains("flac")
        }
        val uploader = dto.uploaderName?.let {
            ProviderIdentity(
                providerId = dto.uploaderUrl?.substringAfterLast("/user/"),
                displayName = it
            )
        } ?: extractedGroup?.let {
            ProviderIdentity(providerId = null, displayName = it)
        }
        val releaseGroup = extractedGroup?.let {
            ProviderIdentity(providerId = null, displayName = it)
        }

        val publishedAt = dto.timestampSeconds?.let { Instant.ofEpochSecond(it) }

        val rawMeta = mutableMapOf<String, String>()
        dto.categoryCode?.let { rawMeta["nyaa_category_code"] = it }
        dto.categoryName?.let { rawMeta["nyaa_category_name"] = it }
        dto.sizeDisplay?.let { rawMeta["nyaa_size_display"] = it }
        dto.infoHash?.let { rawMeta["nyaa_info_hash"] = it }
        dto.completedDownloads?.let { rawMeta["nyaa_completed_downloads"] = it.toString() }
        rawMeta["nyaa_comments_count"] = dto.commentsCount.toString()
        if (extractedGroup != null) {
            rawMeta["nyaa_extracted_group"] = extractedGroup
        }

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
            releaseGroup = releaseGroup,
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
                NyaaCategory.AudioAll, NyaaCategory.AudioLossless, NyaaCategory.AudioLossy -> ProviderCategory.NonAnime.Audio
                NyaaCategory.LiteratureAll, NyaaCategory.LiteratureEnglish, NyaaCategory.LiteratureNonEnglish, NyaaCategory.LiteratureRaw -> ProviderCategory.NonAnime.Manga
                NyaaCategory.LiveActionAll, NyaaCategory.LiveActionEnglish, NyaaCategory.LiveActionIdolPromo, NyaaCategory.LiveActionNonEnglish, NyaaCategory.LiveActionRaw -> ProviderCategory.NonAnime.LiveAction
                NyaaCategory.PicturesAll, NyaaCategory.PicturesGraphics, NyaaCategory.PicturesPhotos -> ProviderCategory.NonAnime.Pictures
                NyaaCategory.SoftwareAll, NyaaCategory.SoftwareApplications, NyaaCategory.SoftwareGames -> ProviderCategory.NonAnime.Software
            }
        }
        return ProviderCategory.All
    }
}
