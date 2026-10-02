package com.aniflow.core.ui.util

/**
 * Enforces STEP 13 Section 60 (RTL / Arabic Testing & Layout Resilience).
 *
 * Wraps technical tokens (e.g. "1080p", "HEVC", "[SubsPlease]", "123–128") with Unicode
 * directional isolation markers (LRI \u2066 and PDI \u2069) so that they render strictly
 * left-to-right inside right-to-left Arabic sentences without reversing or clipping punctuation.
 */
object BidiFormatter {

    private const val LRI = "\u2066" // Left-to-Right Isolate
    private const val PDI = "\u2069" // Pop Directional Isolate

    /**
     * Encloses technical string in an LTR isolate block.
     */
    fun isolateLtr(text: String): String {
        if (text.isBlank()) return text
        return "$LRI$text$PDI"
    }

    /**
     * Formats an episode range safely for bidirectional display.
     * Example: "123–128" will stay "123–128" even in Arabic RTL text.
     */
    fun formatEpisodeRange(start: Int, end: Int): String {
        return isolateLtr("$start–$end")
    }

    /**
     * Formats technical tags safely (e.g. "[SubsPlease] 1080p HEVC").
     */
    fun formatTechnicalTag(tag: String): String {
        return isolateLtr(tag)
    }
}
