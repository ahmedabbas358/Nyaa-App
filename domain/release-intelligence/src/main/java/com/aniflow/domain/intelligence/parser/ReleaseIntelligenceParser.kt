package com.aniflow.domain.intelligence.parser

import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.valueobject.EpisodeRange
import com.aniflow.domain.intelligence.detector.AnimeTitleExtractor
import com.aniflow.domain.intelligence.detector.AudioDetector
import com.aniflow.domain.intelligence.detector.BatchDetector
import com.aniflow.domain.intelligence.detector.BitDepthDetector
import com.aniflow.domain.intelligence.detector.CodecDetector
import com.aniflow.domain.intelligence.detector.EpisodeDetector
import com.aniflow.domain.intelligence.detector.HdrDetector
import com.aniflow.domain.intelligence.detector.LanguageDetector
import com.aniflow.domain.intelligence.detector.MovieDetector
import com.aniflow.domain.intelligence.detector.ReleaseGroupDetector
import com.aniflow.domain.intelligence.detector.ResolutionDetector
import com.aniflow.domain.intelligence.detector.SeasonDetector
import com.aniflow.domain.intelligence.detector.SourceDetector
import com.aniflow.domain.intelligence.detector.SubtitleDetector
import com.aniflow.domain.intelligence.detector.VolumeDetector
import com.aniflow.domain.intelligence.detector.YearDetector
import com.aniflow.domain.intelligence.identity.AnimeIdentityResolver
import com.aniflow.domain.intelligence.model.BatchType
import com.aniflow.domain.intelligence.model.EpisodeCoverage
import com.aniflow.domain.intelligence.model.MediaType
import com.aniflow.domain.intelligence.model.MetadataConflict
import com.aniflow.domain.intelligence.model.NormalizationResult
import com.aniflow.domain.intelligence.model.NormalizedRelease
import com.aniflow.domain.intelligence.model.ParseContext
import com.aniflow.domain.intelligence.model.ParserState
import com.aniflow.domain.intelligence.model.ParsingWarning
import com.aniflow.domain.intelligence.model.RawReleaseInput
import com.aniflow.domain.intelligence.model.ReleaseConfidence
import com.aniflow.domain.intelligence.model.ReleaseGroupReference
import com.aniflow.domain.intelligence.model.ReleaseIdentity
import com.aniflow.domain.intelligence.model.ReleaseIntelligenceConfig
import com.aniflow.domain.intelligence.model.SeasonHint
import com.aniflow.domain.intelligence.model.SeasonReference
import com.aniflow.domain.intelligence.model.TechnicalIdentity
import com.aniflow.domain.intelligence.model.TechnicalMetadata
import com.aniflow.domain.intelligence.model.UploaderReference
import com.aniflow.domain.intelligence.model.VideoSource
import com.aniflow.domain.intelligence.normalizer.TitlePreNormalizer
import com.aniflow.domain.intelligence.resolver.ConfidenceEngine
import com.aniflow.domain.intelligence.resolver.ConflictResolver
import com.aniflow.domain.intelligence.tokenizer.ReleaseTokenizer
import com.aniflow.domain.model.aggregate.release.ReleaseSource
import com.aniflow.domain.model.aggregate.release.ReleaseType
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.InfoHash
import java.time.Instant
import java.util.UUID

/**
 * Step 19 — Master Release Intelligence Engine (Sections 1, 10, 71, 114, 120).
 * Deterministic, pure pipeline transforming raw provider release titles into fully normalized domain releases.
 * Free of side-effects, Room DAO, OkHttp, or UI dependencies.
 */
class ReleaseIntelligenceParser(
    private val config: ReleaseIntelligenceConfig = ReleaseIntelligenceConfig.DEFAULT,
    private val identityResolver: AnimeIdentityResolver = AnimeIdentityResolver()
) {

    private val seasonDetector = SeasonDetector()
    private val episodeDetector = EpisodeDetector()
    private val volumeDetector = VolumeDetector()
    private val batchDetector = BatchDetector()
    private val resolutionDetector = ResolutionDetector()
    private val codecDetector = CodecDetector()
    private val bitDepthDetector = BitDepthDetector()
    private val hdrDetector = HdrDetector()
    private val audioDetector = AudioDetector()
    private val subtitleDetector = SubtitleDetector()
    private val sourceDetector = SourceDetector()
    private val languageDetector = LanguageDetector()
    private val groupDetector = ReleaseGroupDetector()
    private val movieDetector = MovieDetector()
    private val yearDetector = YearDetector()
    private val titleExtractor = AnimeTitleExtractor()
    private val conflictResolver = ConflictResolver()
    private val confidenceEngine = ConfidenceEngine()

    /**
     * Overload accepting Section 3 Input Contract: RawReleaseInput.
     */
    fun parse(input: RawReleaseInput): NormalizationResult {
        val infoHash = input.sources.filterIsInstance<ReleaseSource.Torrent>().firstOrNull()?.infoHash
        val rawMetadata = mutableMapOf<String, String>()
        input.size?.let { rawMetadata["sizeBytes"] = it.bytes.toString() }
        input.seeds?.let { rawMetadata["seeders"] = it.toString() }
        input.leeches?.let { rawMetadata["leechers"] = it.toString() }
        input.downloads?.let { rawMetadata["downloads"] = it.toString() }

        val normalized = parse(
            rawTitle = input.title,
            providerId = input.providerId,
            providerReleaseId = input.providerReleaseId,
            uploader = input.uploader,
            infoHash = infoHash,
            rawMetadata = rawMetadata
        )

        return NormalizationResult(
            release = normalized,
            parserState = normalized.state,
            confidence = normalized.confidence,
            warnings = normalized.warnings,
            conflicts = normalized.conflicts
        )
    }

    /**
     * Core deterministic parsing pipeline (Section 71, 120).
     */
    fun parse(
        rawTitle: String,
        providerId: ProviderId = ProviderId("nyaa"),
        providerReleaseId: String? = null,
        uploader: String? = null,
        infoHash: InfoHash? = null,
        rawMetadata: Map<String, String> = emptyMap()
    ): NormalizedRelease {
        val warnings = mutableListOf<ParsingWarning>()
        val conflicts = mutableListOf<MetadataConflict>()

        // 1. Safe Pre-Normalization (Sections 6, 7)
        val preNormalized = TitlePreNormalizer.preNormalize(rawTitle)

        // 2. Tokenization & Token Classification (Sections 8, 9)
        val tokens = ReleaseTokenizer.tokenize(preNormalized)

        val context = ParseContext(
            rawTitle = rawTitle,
            normalizedTitle = preNormalized,
            tokens = tokens,
            providerMetadata = rawMetadata,
            uploader = uploader,
            config = config
        )

        // 3. Technical & Structural Detection (Sections 10-30)
        // Detect technical tokens first to protect them (Section 72)
        val resRes = resolutionDetector.detect(context)
        val codecRes = codecDetector.detect(context)
        val bitDepthRes = bitDepthDetector.detect(context)
        val hdrRes = hdrDetector.detect(context)
        val audioRes = audioDetector.detect(context)
        val subRes = subtitleDetector.detect(context)
        val sourceRes = sourceDetector.detect(context)
        val langRes = languageDetector.detect(context)

        // Detect structural tokens
        val seasonRes = seasonDetector.detect(context)
        val volumeRes = volumeDetector.detect(context)
        val episodeRes = episodeDetector.detect(context)
        val batchRes = batchDetector.detect(context)
        val groupRes = groupDetector.detect(context)
        val movieRes = movieDetector.detect(context)
        val yearRes = yearDetector.detect(context)

        // Collect detector warnings
        warnings.addAll(seasonRes.warnings)
        warnings.addAll(episodeRes.warnings)
        warnings.addAll(codecRes.warnings)

        // 4. Conflict Resolution (Sections 31, 83)
        var seasonNum: Int? = seasonRes.value?.seasonNumber
        if (seasonRes.warnings.any { it.code == "ConflictingSeason" }) {
            conflicts.add(
                MetadataConflict(
                    field = "Season",
                    candidateA = seasonRes.evidence.firstOrNull()?.rawText ?: "unknown",
                    candidateB = seasonRes.evidence.lastOrNull()?.rawText ?: "unknown",
                    evidenceA = seasonRes.evidence.first(),
                    evidenceB = seasonRes.evidence.last(),
                    status = com.aniflow.domain.intelligence.model.ConflictStatus.Ambiguous,
                    resolvedValue = seasonNum?.toString()
                )
            )
        }

        if (codecRes.warnings.any { it.code == "ConflictingCodec" }) {
            conflicts.add(
                MetadataConflict(
                    field = "VideoCodec",
                    candidateA = codecRes.evidence.firstOrNull()?.rawText ?: "unknown",
                    candidateB = codecRes.evidence.lastOrNull()?.rawText ?: "unknown",
                    evidenceA = codecRes.evidence.first(),
                    evidenceB = codecRes.evidence.last(),
                    status = com.aniflow.domain.intelligence.model.ConflictStatus.Ambiguous,
                    resolvedValue = null
                )
            )
        }

        // 5. Determine Batch Type & Coverage (Sections 14, 15, 43, 54)
        val batchType = when {
            movieRes.isFound -> BatchType.SingleEpisode
            batchRes.value == BatchType.SeriesBatch -> BatchType.SeriesBatch
            batchRes.value == BatchType.SeasonBatch -> BatchType.SeasonBatch
            batchRes.value == BatchType.EpisodeRange -> BatchType.EpisodeRange
            episodeRes.value?.isRange == true -> BatchType.EpisodeRange
            episodeRes.value?.singleEpisode != null -> BatchType.SingleEpisode
            else -> BatchType.Unknown
        }

        val episodeCoverage: EpisodeCoverage = when {
            episodeRes.value?.range != null && episodeRes.value.isRange -> {
                val r = episodeRes.value.range
                if (r is EpisodeRange.Range) {
                    EpisodeCoverage.Range(r.start.major, r.end.major)
                } else {
                    val list = r?.toList() ?: emptyList()
                    if (list.size >= 2) EpisodeCoverage.Range(list.first().major, list.last().major)
                    else if (list.size == 1) EpisodeCoverage.Single(list.first().major)
                    else EpisodeCoverage.Unknown
                }
            }
            episodeRes.value?.singleEpisode != null -> {
                EpisodeCoverage.Single(episodeRes.value.singleEpisode!!)
            }
            batchType == BatchType.SeasonBatch -> {
                EpisodeCoverage.Season(seasonNum ?: 1)
            }
            batchType == BatchType.SeriesBatch -> {
                EpisodeCoverage.Series
            }
            else -> EpisodeCoverage.Unknown
        }

        // 6. Extract Anime Title & Resolve Identity (Sections 37, 38, 73)
        val candidateTitleRaw = titleExtractor.extractTitle(
            tokens = tokens,
            groupToken = groupRes.value?.name,
            detectedSeason = seasonNum,
            detectedEpisode = episodeRes.value?.singleEpisode
        )

        val seasonHint = seasonRes.value?.let {
            SeasonHint(it.seasonNumber, it.partNumber, it.isCour, it.rawText)
        }

        val identityMatch = identityResolver.resolve(
            candidateTitle = candidateTitleRaw,
            detectedYear = yearRes.value,
            seasonHint = seasonHint
        )

        // 7. Calculate Decision-Critical Confidence (Sections 33, 34, 85, 86)
        val epConf = episodeRes.confidence
        val seasonConf = if (seasonRes.isFound) seasonRes.confidence else 0.80f
        val titleConf = identityMatch.confidence
        val techConf = if (resRes.isFound || codecRes.isFound) 0.95f else 0.50f
        val sourceConf = if (sourceRes.isFound) sourceRes.confidence else 0.50f

        val releaseConfidence = confidenceEngine.calculate(
            episodeConfidence = epConf,
            seasonConfidence = seasonConf,
            titleConfidence = titleConf,
            technicalConfidence = techConf,
            sourceConfidence = sourceConf,
            conflicts = conflicts,
            warnings = warnings
        )

        // 8. Deterministic Release Identity (Section 44, 46)
        val releaseId = ReleaseId(
            if (infoHash != null) "rel_${infoHash.hexString.take(16)}"
            else "rel_${providerId.value}_${providerReleaseId ?: UUID.randomUUID().toString().take(8)}"
        )

        val technical = TechnicalMetadata(
            resolution = resRes.value,
            codec = codecRes.value,
            bitDepth = bitDepthRes.value,
            source = sourceRes.value,
            audio = audioRes.value?.tracks ?: emptyList(),
            subtitles = subRes.value ?: emptyList(),
            hdr = hdrRes.value,
            multiAudio = audioRes.value?.multiAudio ?: false
        )

        val seasonReference = seasonRes.value?.toSeasonReference() ?: SeasonReference(seasonNumber = 1)

        val uploaderRef = uploader?.let { UploaderReference(it) }

        // Determine parser state (Section 35)
        val parseState = when {
            conflicts.any { it.status == com.aniflow.domain.intelligence.model.ConflictStatus.Ambiguous } -> ParserState.Ambiguous
            releaseConfidence.needsManualReview -> ParserState.Ambiguous
            identityMatch.isAmbiguous -> ParserState.Ambiguous
            else -> ParserState.Parsed
        }

        val legacyType = when {
            movieRes.isFound -> ReleaseType.Movie
            batchType == BatchType.SeriesBatch -> ReleaseType.CompleteSeries
            batchType == BatchType.SeasonBatch -> ReleaseType.Season
            batchType == BatchType.EpisodeRange -> ReleaseType.Batch
            episodeRes.value?.isSpecial == true -> ReleaseType.Special
            episodeRes.value?.singleEpisode != null -> ReleaseType.SingleEpisode
            else -> ReleaseType.Unknown
        }

        return NormalizedRelease(
            releaseId = releaseId,
            animeIdentity = identityMatch.identity,
            season = seasonReference,
            episodes = episodeCoverage,
            technical = technical,
            uploader = uploaderRef,
            releaseGroup = groupRes.value,
            batchType = batchType,
            confidence = releaseConfidence,
            warnings = warnings,
            rawTitle = rawTitle,
            normalizedTitle = preNormalized,
            conflicts = conflicts,
            state = parseState,
            rawMetadata = rawMetadata,
            parserVersion = config.parserVersion,
            legacyReleaseType = legacyType
        )
    }

    /**
     * Reprocessing Pipeline (Section 66, 116).
     * Re-runs updated parser version over locally stored raw release metadata without network requests.
     */
    fun reprocess(
        rawReleases: List<RawReleaseInput>
    ): List<NormalizationResult> {
        return rawReleases.map { parse(it) }
    }
}
