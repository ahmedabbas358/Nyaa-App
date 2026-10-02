package com.aniflow.testing

import com.aniflow.core.ui.components.ReleaseCardDensity
import com.aniflow.core.ui.util.UiFormatters
import com.aniflow.feature.downloads.DownloadFilterTab
import com.aniflow.feature.downloads.DownloadsViewModel
import com.aniflow.feature.release.AnimeSeasonViewModel
import com.aniflow.feature.search.SearchViewModel
import com.aniflow.navigation.ScreenRoute
import com.aniflow.navigation.WindowWidthSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PresentationAndUiTests {

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

    @Test
    fun testSearchViewModelSelectionAndDensity() {
        val viewModel = SearchViewModel()

        assertEquals(ReleaseCardDensity.Comfortable, viewModel.uiState.value.density)
        viewModel.toggleDensity()
        assertEquals(ReleaseCardDensity.Compact, viewModel.uiState.value.density)
        viewModel.toggleDensity()
        assertEquals(ReleaseCardDensity.Comfortable, viewModel.uiState.value.density)

        // Test Selection
        viewModel.toggleSelection("rel-1")
        assertTrue(viewModel.uiState.value.selectedReleaseIds.contains("rel-1"))

        viewModel.selectAll()
        assertTrue(viewModel.uiState.value.selectedReleaseIds.size >= 2)

        viewModel.clearSelection()
        assertTrue(viewModel.uiState.value.selectedReleaseIds.isEmpty())

        // Test Filter Sheets
        viewModel.setFilterSheetVisible(true)
        assertTrue(viewModel.uiState.value.showFilterSheet)
        viewModel.setFilterSheetVisible(false)
        assertFalse(viewModel.uiState.value.showFilterSheet)
    }

    @Test
    fun testDownloadsViewModelFilterTabs() {
        val viewModel = DownloadsViewModel()

        assertEquals(DownloadFilterTab.All, viewModel.uiState.value.selectedFilterTab)
        viewModel.selectFilterTab(DownloadFilterTab.Active)
        assertEquals(DownloadFilterTab.Active, viewModel.uiState.value.selectedFilterTab)
        viewModel.selectFilterTab(DownloadFilterTab.Completed)
        assertEquals(DownloadFilterTab.Completed, viewModel.uiState.value.selectedFilterTab)
    }

    @Test
    fun testAnimeSeasonViewModelState() {
        val viewModel = AnimeSeasonViewModel()

        assertEquals(1, viewModel.uiState.value.currentSeason)
        viewModel.selectSeason(2)
        assertEquals(2, viewModel.uiState.value.currentSeason)

        val firstEp = viewModel.uiState.value.episodes.first()
        viewModel.openEpisodeDetails(firstEp)
        assertNotNull(viewModel.uiState.value.selectedEpisodeForDetail)
        assertEquals(firstEp.id, viewModel.uiState.value.selectedEpisodeForDetail?.id)

        viewModel.closeEpisodeDetails()
        assertNull(viewModel.uiState.value.selectedEpisodeForDetail)
    }

    @Test
    fun testTypedRouteGeneration() {
        assertEquals("release/rel-123", ScreenRoute.ReleaseDetails.createRoute("rel-123"))
        assertEquals("anime/one-piece", ScreenRoute.AnimeDetails.createRoute("one-piece"))
        assertEquals("anime/one-piece/season/2", ScreenRoute.SeasonDetails.createRoute("one-piece", 2))
        assertEquals("download_details/task-456", ScreenRoute.DownloadDetails.createRoute("task-456"))
    }
}
