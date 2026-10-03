package com.aniflow.core.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UiFormattersTest {

    @Test
    fun testUiFormattersByteSizes() {
        assertEquals("0 B", UiFormatters.formatBytes(0L))
        assertEquals("500 B", UiFormatters.formatBytes(500L))
        assertEquals("1.0 KB", UiFormatters.formatBytes(1024L))
        assertEquals("500.0 KB", UiFormatters.formatBytes(500 * 1024L))
        assertEquals("1.4 GB", UiFormatters.formatBytes((1.37 * 1024 * 1024 * 1024).toLong()))
        assertEquals("82.4 GB", UiFormatters.formatBytes((82.4 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun testUiFormattersSpeeds() {
        assertEquals("0 B/s", UiFormatters.formatSpeed(0L))
        assertEquals("4.2 MB/s", UiFormatters.formatSpeed((4.2 * 1024 * 1024).toLong()))
        assertEquals("12.4 MB/s", UiFormatters.formatSpeed((12.4 * 1024 * 1024).toLong()))
    }

    @Test
    fun testUiFormattersEtas() {
        assertEquals("—", UiFormatters.formatEta(null))
        assertEquals("—", UiFormatters.formatEta(-1L))
        assertEquals("45s", UiFormatters.formatEta(45L))
        assertEquals("02:14", UiFormatters.formatEta(134L))
        assertEquals("1h 32m", UiFormatters.formatEta(3600 + 32 * 60L))
    }

    @Test
    fun testUiFormattersEpisodeNumbers() {
        assertEquals("—", UiFormatters.formatEpisodeNumber(null))
        assertEquals("01", UiFormatters.formatEpisodeNumber(1.0))
        assertEquals("09", UiFormatters.formatEpisodeNumber(9.0))
        assertEquals("12", UiFormatters.formatEpisodeNumber(12.0))
        assertEquals("1050", UiFormatters.formatEpisodeNumber(1050.0))
        assertEquals("12.5", UiFormatters.formatEpisodeNumber(12.5))
    }
}
