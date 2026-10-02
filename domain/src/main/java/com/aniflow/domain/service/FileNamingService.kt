package com.aniflow.domain.service

import com.aniflow.domain.model.aggregate.media.Anime
import com.aniflow.domain.model.aggregate.media.Episode
import com.aniflow.domain.model.aggregate.media.Season
import com.aniflow.domain.model.aggregate.release.Release

/**
 * Domain service resolving organization paths and file names from templates (Section 77).
 * Purely algorithmic string processing without touching Android storage or java.io.File.
 */
object FileNamingService {

    const val DEFAULT_TEMPLATE = "{anime}/Season {season}/{anime} - S{season_pad}E{episode_pad} - {title}"

    private val FORBIDDEN_CHARS_REGEX = Regex("""[\\/:*?"<>|]""")

    fun sanitizePathComponent(component: String): String {
        return FORBIDDEN_CHARS_REGEX.replace(component, "_").trim()
    }

    fun resolvePath(
        template: String = DEFAULT_TEMPLATE,
        anime: Anime,
        season: Season?,
        episode: Episode?,
        release: Release? = null
    ): String {
        val animeTitle = sanitizePathComponent(anime.canonicalTitle)
        val seasonNum = season?.number?.numericValue ?: 1
        val seasonStr = seasonNum.toString()
        val seasonPad = seasonNum.toString().padStart(2, '0')

        val episodeNum = episode?.number?.major ?: 1
        val episodeStr = episode?.number?.displayString ?: "01"
        val episodePad = episode?.number?.major?.toString()?.padStart(2, '0') ?: "01"

        val epTitle = sanitizePathComponent(episode?.title ?: "Episode $episodeStr")
        val resolution = release?.technical?.resolution?.displayName ?: "1080p"
        val codec = release?.technical?.videoCodec?.displayName ?: "HEVC"
        val group = sanitizePathComponent(release?.releaseGroup?.name ?: "AniFlow")

        return template
            .replace("{anime}", animeTitle)
            .replace("{season}", seasonStr)
            .replace("{season_pad}", seasonPad)
            .replace("{episode}", episodeStr)
            .replace("{episode_pad}", episodePad)
            .replace("{title}", epTitle)
            .replace("{resolution}", resolution)
            .replace("{codec}", codec)
            .replace("{group}", group)
    }
}
