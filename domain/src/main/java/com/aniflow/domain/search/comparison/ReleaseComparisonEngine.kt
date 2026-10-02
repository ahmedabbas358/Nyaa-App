package com.aniflow.domain.search.comparison

import com.aniflow.domain.search.model.ComparisonDifference
import com.aniflow.domain.search.model.ReleaseComparisonItem
import com.aniflow.domain.search.model.ReleaseComparisonResult

/**
 * ReleaseComparisonEngine (Section 32).
 * Compares 2 or more candidate releases attribute by attribute,
 * highlighting differences (Resolution, Codec, Audio, Subtitle, Source, Size, Seeds).
 * Does NOT select a winner; purely provides objective, explainable comparison.
 */
object ReleaseComparisonEngine {

    fun compare(items: List<ReleaseComparisonItem>): ReleaseComparisonResult {
        if (items.isEmpty()) {
            return ReleaseComparisonResult(emptyList(), emptyList())
        }

        val differences = mutableListOf<ComparisonDifference>()

        differences.add(evaluateAttribute("Resolution", items) { it.resolution })
        differences.add(evaluateAttribute("Video Codec", items) { it.codec })
        differences.add(evaluateAttribute("Audio Language", items) { it.audio })
        differences.add(evaluateAttribute("Subtitles", items) { it.subtitles })
        differences.add(evaluateAttribute("Source", items) { it.source })
        differences.add(evaluateAttribute("File Size", items) { it.sizeFormatted })
        differences.add(evaluateAttribute("Seeds", items) { "${it.seeds} seeds" })
        differences.add(evaluateAttribute("Peers", items) { "${it.peers} peers" })
        differences.add(evaluateAttribute("Uploader", items) { it.uploader })
        differences.add(evaluateAttribute("Release Group", items) { it.releaseGroup ?: "None" })
        differences.add(evaluateAttribute("Batch Type", items) { if (it.isBatch) "Batch / Season" else "Single Episode" })
        differences.add(evaluateAttribute("In Library", items) { if (it.inLibrary) "Yes" else "No" })

        return ReleaseComparisonResult(
            items = items,
            differences = differences
        )
    }

    private fun evaluateAttribute(
        name: String,
        items: List<ReleaseComparisonItem>,
        extractor: (ReleaseComparisonItem) -> String
    ): ComparisonDifference {
        val map = items.associate { it.releaseId.value to extractor(it) }
        val distinctValues = map.values.distinct()
        val hasDifference = distinctValues.size > 1

        return ComparisonDifference(
            attributeName = name,
            valuesByReleaseId = map,
            hasDifference = hasDifference
        )
    }
}
