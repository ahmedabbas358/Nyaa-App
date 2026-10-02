package com.aniflow.domain.intelligence.identity

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.intelligence.model.AnimeIdentity
import com.aniflow.domain.intelligence.model.SeasonHint
import com.aniflow.domain.valueobject.InfoHash
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Persisted user override correcting parser or identity decisions (Section 40, 64, 109).
 * User overrides take top priority over automated parser heuristics.
 */
data class UserMappingOverride(
    val titleSignature: String,
    val canonicalAnimeId: AnimeId,
    val canonicalAnimeTitle: String,
    val seasonOverride: Int? = null,
    val episodeOverride: Int? = null
)

/**
 * Result of resolving anime identity (Section 5, 37, 39).
 */
data class AnimeIdentityMatch(
    val identity: AnimeIdentity,
    val confidence: Float,
    val matchType: MatchType,
    val isAmbiguous: Boolean = false
) {
    enum class MatchType {
        UserOverride,
        ExactCanonical,
        KnownAlias,
        StrongNormalized,
        Fuzzy,
        Ambiguous
    }
}

/**
 * Resolves candidate titles to canonical Anime identities and aliases (Section 5, 37, 38, 39, 40).
 */
class AnimeIdentityResolver(
    private val overrides: ConcurrentHashMap<String, UserMappingOverride> = ConcurrentHashMap(),
    private val knownAliases: Map<String, List<String>> = mapOf(
        "One Piece" to listOf("ワンピース", "Wan Pīsu", "OP"),
        "Sousou no Frieren" to listOf("Frieren: Beyond Journey's End", "葬送のフリーレン", "Frieren"),
        "Jujutsu Kaisen" to listOf("呪術廻戦", "JJK"),
        "Kimetsu no Yaiba" to listOf("Demon Slayer", "鬼滅の刃"),
        "Shingeki no Kyojin" to listOf("Attack on Titan", "進撃の巨人", "AoT"),
        "Boku no Hero Academia" to listOf("My Hero Academia", "僕のヒーローアカデミア", "MHA"),
        "86" to listOf("Eighty Six", "86 - Eighty Six", "86 -エイティシックス-"),
        "Bleach" to listOf("ブリーチ", "Bleach: Thousand-Year Blood War")
    )
) {

    fun resolve(
        candidateTitle: String,
        detectedYear: Int? = null,
        seasonHint: SeasonHint? = null
    ): AnimeIdentityMatch {
        val cleaned = candidateTitle.trim()
        val normalizedCandidate = normalizeForMatch(cleaned)

        // 1. User Override (Highest Priority - Section 40, 64, 109)
        overrides[normalizedCandidate]?.let { override ->
            return AnimeIdentityMatch(
                identity = AnimeIdentity(
                    canonicalTitle = override.canonicalAnimeTitle,
                    aliases = knownAliases[override.canonicalAnimeTitle] ?: emptyList(),
                    year = detectedYear,
                    seasonHint = seasonHint
                ),
                confidence = 1.0f,
                matchType = AnimeIdentityMatch.MatchType.UserOverride
            )
        }

        // 2. Exact match against known canonical titles
        for ((canonical, aliases) in knownAliases) {
            if (normalizeForMatch(canonical) == normalizedCandidate) {
                return AnimeIdentityMatch(
                    identity = AnimeIdentity(
                        canonicalTitle = canonical,
                        aliases = aliases,
                        year = detectedYear,
                        seasonHint = seasonHint
                    ),
                    confidence = 0.99f,
                    matchType = AnimeIdentityMatch.MatchType.ExactCanonical
                )
            }

            // 3. Known Alias Match
            for (alias in aliases) {
                if (normalizeForMatch(alias) == normalizedCandidate) {
                    return AnimeIdentityMatch(
                        identity = AnimeIdentity(
                            canonicalTitle = canonical,
                            aliases = aliases,
                            year = detectedYear,
                            seasonHint = seasonHint
                        ),
                        confidence = 0.97f,
                        matchType = AnimeIdentityMatch.MatchType.KnownAlias
                    )
                }
            }
        }

        // 4. Strong Fuzzy Match (Levenshtein similarity)
        var bestMatch: Pair<String, Double>? = null
        for ((canonical, aliases) in knownAliases) {
            val scoreCanonical = similarity(normalizedCandidate, normalizeForMatch(canonical))
            if (scoreCanonical >= 0.85 && (bestMatch == null || scoreCanonical > bestMatch.second)) {
                bestMatch = canonical to scoreCanonical
            }
            for (alias in aliases) {
                val scoreAlias = similarity(normalizedCandidate, normalizeForMatch(alias))
                if (scoreAlias >= 0.85 && (bestMatch == null || scoreAlias > bestMatch.second)) {
                    bestMatch = canonical to scoreAlias
                }
            }
        }

        if (bestMatch != null) {
            val isAmbiguous = bestMatch.second < 0.90
            return AnimeIdentityMatch(
                identity = AnimeIdentity(
                    canonicalTitle = bestMatch.first,
                    aliases = knownAliases[bestMatch.first] ?: emptyList(),
                    year = detectedYear,
                    seasonHint = seasonHint
                ),
                confidence = bestMatch.second.toFloat(),
                matchType = if (isAmbiguous) AnimeIdentityMatch.MatchType.Ambiguous else AnimeIdentityMatch.MatchType.StrongNormalized,
                isAmbiguous = isAmbiguous
            )
        }

        // 5. Unrecognized: Cleaned candidate is accepted as canonical candidate (Section 37)
        return AnimeIdentityMatch(
            identity = AnimeIdentity(
                canonicalTitle = cleaned,
                aliases = emptyList(),
                year = detectedYear,
                seasonHint = seasonHint
            ),
            confidence = 0.70f,
            matchType = AnimeIdentityMatch.MatchType.Fuzzy,
            isAmbiguous = false
        )
    }

    fun addOverride(override: UserMappingOverride) {
        overrides[normalizeForMatch(override.titleSignature)] = override
    }

    private fun normalizeForMatch(str: String): String {
        return str.lowercase(Locale.ROOT)
            .replace(Regex("""[^a-z0-9\u3040-\u30ff\u4e00-\u9faf\u0600-\u06ff]"""), "")
            .trim()
    }

    private fun similarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        val maxLen = maxOf(s1.length, s2.length)
        val distance = levenshteinDistance(s1, s2)
        return 1.0 - (distance.toDouble() / maxLen.toDouble())
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j

        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }
}
