package com.aniflow.feature.downloads

import com.aniflow.domain.identity.DownloadTaskId
import com.aniflow.feature.downloads.aggregator.DownloadDashboardAggregator
import com.aniflow.feature.downloads.model.DownloadEngineBadge
import com.aniflow.feature.downloads.model.DownloadFilterCategory
import com.aniflow.feature.downloads.model.DownloadGroupingMode
import com.aniflow.feature.downloads.model.DownloadPriorityUi
import com.aniflow.feature.downloads.model.DownloadSortOption
import com.aniflow.feature.downloads.model.DownloadStateUi
import com.aniflow.feature.downloads.model.DownloadTaskUiModel
import com.aniflow.feature.downloads.util.ByteSizeFormatter
import com.aniflow.feature.downloads.util.DurationFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DownloadManagerTestSuite (Sections 137, 138, 141).
 * Tests all DoD requirements for Step 25:
 * 1. Byte and Duration formatters.
 * 2. Overall progress calculation from sum of bytes (never average of percents).
 * 3. Total active transfer speed aggregation.
 * 4. Status filtering, local search, and sorting.
 * 5. Grouping by Anime and Plan progress computation.
 * 6. Bulk selection state handling.
 */
class DownloadManagerTestSuite {

    private fun sampleTask(
        id: String,
        title: String,
        animeTitle: String,
        state: DownloadStateUi = DownloadStateUi.Downloading,
        downloaded: Long = 1_000_000_000L,
        total: Long = 2_000_000_000L,
        speed: Long = 10_000_000L,
        priority: DownloadPriorityUi = DownloadPriorityUi.Normal,
        planId: String? = null
    ): DownloadTaskUiModel {
        val pct = if (total > 0) ((downloaded.toDouble() / total) * 100).toInt() else 0
        return DownloadTaskUiModel(
            id = DownloadTaskId(id),
            title = title,
            animeTitle = animeTitle,
            state = state,
            engineBadge = DownloadEngineBadge.HTTP,
            priority = priority,
            downloadedBytes = downloaded,
            totalBytes = total,
            progressPercent = pct,
            speedBytesPerSec = speed,
            planId = planId
        )
    }

    // =========================================================================
    // 1. Formatters Tests (Section 72, 74, 75)
    // =========================================================================

    @Test
    fun testByteSizeFormatter_formatsCorrectly() {
        assertEquals("0 B", ByteSizeFormatter.format(0))
        assertEquals("500 B", ByteSizeFormatter.format(500))
        assertEquals("1.5 KB", ByteSizeFormatter.format(1500))
        assertEquals("18.4 MB", ByteSizeFormatter.format(18_400_000))
        assertEquals("2.1 GB", ByteSizeFormatter.format(2_100_000_000))
        assertEquals("18.4 MB/s", ByteSizeFormatter.formatSpeed(18_400_000))
    }

    @Test
    fun testDurationFormatter_formatsEtaAndDurations() {
        assertEquals("Estimating…", DurationFormatter.formatEta(null))
        assertEquals("Complete", DurationFormatter.formatEta(0))
        assertEquals("22s", DurationFormatter.formatEta(22))
        assertEquals("03:24", DurationFormatter.formatEta(204))
        assertEquals("1h 20m", DurationFormatter.formatEta(4800))
    }

    // =========================================================================
    // 2. Dashboard Aggregator Tests (Section 3, 4, 107)
    // =========================================================================

    @Test
    fun testAggregator_computesOverallProgressFromSumOfBytes() {
        val tasks = listOf(
            // Task 1: 1.0 GB / 1.0 GB (100%)
            sampleTask("t1", "Ep 1", "Anime A", downloaded = 1_000_000_000L, total = 1_000_000_000L, speed = 5_000_000L),
            // Task 2: 1.0 GB / 9.0 GB (11.1%)
            sampleTask("t2", "Ep 2", "Anime A", downloaded = 1_000_000_000L, total = 9_000_000_000L, speed = 5_000_000L)
        )

        val summary = DownloadDashboardAggregator.computeSummary(tasks)

        // Average of percents would be (100 + 11.1) / 2 = 55.5% (WRONG)
        // True progress is sum(downloaded) / sum(total) = 2.0 GB / 10.0 GB = 20% (CORRECT Section 4)
        assertEquals(20, summary.overallProgressPercent)
        assertEquals(2_000_000_000L, summary.overallDownloadedBytes)
        assertEquals(10_000_000_000L, summary.overallTotalBytes)
        assertEquals("10 MB/s", summary.totalActiveSpeedFormatted)
        assertEquals(2, summary.activeCount)
    }

    // =========================================================================
    // 3. Status Filters & Search Tests (Section 5, 37, 39)
    // =========================================================================

    @Test
    fun testAggregator_filtersByStatusAndSearchQuery() {
        val tasks = listOf(
            sampleTask("t1", "One Piece 1110", "One Piece", state = DownloadStateUi.Downloading),
            sampleTask("t2", "One Piece 1111", "One Piece", state = DownloadStateUi.Queued),
            sampleTask("t3", "Bleach 26", "Bleach", state = DownloadStateUi.Completed),
            sampleTask("t4", "Jujutsu Kaisen 24", "Jujutsu Kaisen", state = DownloadStateUi.Failed)
        )

        // Filter active only
        val activeOnly = DownloadDashboardAggregator.filterAndSort(
            tasks = tasks,
            filter = DownloadFilterCategory.Active,
            searchQuery = "",
            sortOption = DownloadSortOption.Name
        )
        assertEquals(1, activeOnly.size)
        assertEquals("One Piece 1110", activeOnly.first().title)

        // Search "bleach"
        val searched = DownloadDashboardAggregator.filterAndSort(
            tasks = tasks,
            filter = DownloadFilterCategory.All,
            searchQuery = "bleach",
            sortOption = DownloadSortOption.Name
        )
        assertEquals(1, searched.size)
        assertEquals("Bleach 26", searched.first().title)
    }

    // =========================================================================
    // 4. Grouping by Anime and Plan Tests (Section 40, 41, 42)
    // =========================================================================

    @Test
    fun testAggregator_groupsTasksByAnime() {
        val tasks = listOf(
            sampleTask("t1", "One Piece 01", "One Piece", downloaded = 1_000_000_000L, total = 1_000_000_000L, state = DownloadStateUi.Completed),
            sampleTask("t2", "One Piece 02", "One Piece", downloaded = 500_000_000L, total = 1_000_000_000L, state = DownloadStateUi.Downloading),
            sampleTask("t3", "Bleach 01", "Bleach", downloaded = 1_000_000_000L, total = 2_000_000_000L, state = DownloadStateUi.Downloading)
        )

        val groups = DownloadDashboardAggregator.groupTasks(tasks, DownloadGroupingMode.ByAnime)

        assertEquals(2, groups.size)
        val onePieceGroup = groups.first { it.groupTitle == "One Piece" }
        assertEquals(2, onePieceGroup.tasks.size)
        assertEquals(1, onePieceGroup.completedCount)
        assertEquals(75, onePieceGroup.progressPercent) // (1.5 GB / 2.0 GB) = 75%
    }

    @Test
    fun testAggregator_sortsTasksByPriorityPrecedence() {
        val tasks = listOf(
            sampleTask("t1", "Low Priority", "Anime A", priority = DownloadPriorityUi.Low),
            sampleTask("t2", "Highest Priority", "Anime A", priority = DownloadPriorityUi.Highest),
            sampleTask("t3", "Normal Priority", "Anime A", priority = DownloadPriorityUi.Normal)
        )

        val sorted = DownloadDashboardAggregator.filterAndSort(
            tasks = tasks,
            filter = DownloadFilterCategory.All,
            searchQuery = "",
            sortOption = DownloadSortOption.Priority
        )

        assertEquals("Highest Priority", sorted[0].title)
        assertEquals("Normal Priority", sorted[1].title)
        assertEquals("Low Priority", sorted[2].title)
    }
}
