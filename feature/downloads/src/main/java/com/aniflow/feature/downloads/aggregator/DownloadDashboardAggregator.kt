package com.aniflow.feature.downloads.aggregator

import com.aniflow.feature.downloads.model.DownloadFilterCategory
import com.aniflow.feature.downloads.model.DownloadGroupingMode
import com.aniflow.feature.downloads.model.DownloadGroupUiModel
import com.aniflow.feature.downloads.model.DownloadSortOption
import com.aniflow.feature.downloads.model.DownloadStateUi
import com.aniflow.feature.downloads.model.DownloadTaskUiModel
import com.aniflow.feature.downloads.util.ByteSizeFormatter

/**
 * Dashboard state summary (Section 3, 4, 107).
 */
data class DashboardAggregateSummary(
    val totalActiveSpeedFormatted: String,
    val overallDownloadedBytes: Long,
    val overallTotalBytes: Long,
    val overallProgressPercent: Int,
    val activeCount: Int,
    val queuedCount: Int,
    val pausedCount: Int,
    val waitingCount: Int,
    val failedCount: Int,
    val completedCount: Int,
    val totalCount: Int
)

/**
 * DownloadDashboardAggregator (Sections 4, 37, 38, 39, 40, 41, 42, 106, 107).
 * Pure business logic for aggregating, filtering, searching, sorting, and grouping download tasks.
 * Avoids redundant recompositions by isolating computation from UI composables.
 */
object DownloadDashboardAggregator {

    fun computeSummary(tasks: List<DownloadTaskUiModel>): DashboardAggregateSummary {
        var totalSpeed = 0L
        var sumDownloaded = 0L
        var sumTotal = 0L
        var active = 0
        var queued = 0
        var paused = 0
        var waiting = 0
        var failed = 0
        var completed = 0

        for (task in tasks) {
            if (task.state.isActive) {
                totalSpeed += task.speedBytesPerSec
                active++
            }
            when (task.state) {
                DownloadStateUi.Queued -> queued++
                DownloadStateUi.Paused -> paused++
                DownloadStateUi.Waiting -> waiting++
                DownloadStateUi.Failed -> failed++
                DownloadStateUi.Completed -> completed++
                else -> Unit
            }

            sumDownloaded += task.downloadedBytes
            sumTotal += (task.totalBytes ?: task.downloadedBytes)
        }

        // Section 4: Overall progress MUST be calculated from sum(downloaded) / sum(total), not average percent!
        val overallPercent = if (sumTotal > 0L) {
            ((sumDownloaded.toDouble() / sumTotal) * 100.0).toInt().coerceIn(0, 100)
        } else 0

        return DashboardAggregateSummary(
            totalActiveSpeedFormatted = ByteSizeFormatter.formatSpeed(totalSpeed),
            overallDownloadedBytes = sumDownloaded,
            overallTotalBytes = sumTotal,
            overallProgressPercent = overallPercent,
            activeCount = active,
            queuedCount = queued,
            pausedCount = paused,
            waitingCount = waiting,
            failedCount = failed,
            completedCount = completed,
            totalCount = tasks.size
        )
    }

    fun filterAndSort(
        tasks: List<DownloadTaskUiModel>,
        filter: DownloadFilterCategory,
        searchQuery: String,
        sortOption: DownloadSortOption
    ): List<DownloadTaskUiModel> {
        // 1. Status Filter (Section 5)
        val statusFiltered = when (filter) {
            DownloadFilterCategory.All -> tasks
            DownloadFilterCategory.Active -> tasks.filter { it.state.isActive }
            DownloadFilterCategory.Queued -> tasks.filter { it.state == DownloadStateUi.Queued }
            DownloadFilterCategory.Paused -> tasks.filter { it.state == DownloadStateUi.Paused }
            DownloadFilterCategory.Waiting -> tasks.filter { it.state == DownloadStateUi.Waiting }
            DownloadFilterCategory.Failed -> tasks.filter { it.state == DownloadStateUi.Failed }
            DownloadFilterCategory.Completed -> tasks.filter { it.state == DownloadStateUi.Completed }
        }

        // 2. Search Query Filter (Section 37)
        val queryFiltered = if (searchQuery.isBlank()) {
            statusFiltered
        } else {
            val q = searchQuery.trim().lowercase()
            statusFiltered.filter { task ->
                task.title.lowercase().contains(q) ||
                    task.animeTitle.lowercase().contains(q) ||
                    task.id.value.lowercase().contains(q) ||
                    task.destinationPath.lowercase().contains(q) ||
                    (task.episodeNumber != null && task.episodeNumber.toString().contains(q))
            }
        }

        // 3. Sort Order (Section 39)
        return when (sortOption) {
            DownloadSortOption.CreatedDate -> queryFiltered.sortedByDescending { it.createdAt }
            DownloadSortOption.Priority -> queryFiltered.sortedWith(
                compareByDescending<DownloadTaskUiModel> { it.priority.level }
                    .thenBy { it.queuePosition ?: Int.MAX_VALUE }
            )
            DownloadSortOption.Progress -> queryFiltered.sortedByDescending { it.progressPercent }
            DownloadSortOption.Speed -> queryFiltered.sortedByDescending { it.speedBytesPerSec }
            DownloadSortOption.Size -> queryFiltered.sortedByDescending { it.totalBytes ?: 0L }
            DownloadSortOption.ETA -> queryFiltered.sortedBy { it.etaSeconds ?: Long.MAX_VALUE }
            DownloadSortOption.Name -> queryFiltered.sortedBy { it.title.lowercase() }
        }
    }

    fun groupTasks(
        tasks: List<DownloadTaskUiModel>,
        groupingMode: DownloadGroupingMode
    ): List<DownloadGroupUiModel> {
        return when (groupingMode) {
            DownloadGroupingMode.Flat -> {
                // Flat mode: single dummy group or direct list
                listOf(
                    createGroup(
                        groupKey = "all_tasks",
                        groupTitle = "All Downloads",
                        tasks = tasks
                    )
                )
            }
            DownloadGroupingMode.ByAnime -> {
                tasks.groupBy { it.animeTitle.ifBlank { "Other / Unknown" } }
                    .map { (anime, groupTasks) ->
                        createGroup(
                            groupKey = "anime_$anime",
                            groupTitle = anime,
                            tasks = groupTasks
                        )
                    }
                    .sortedBy { it.groupTitle }
            }
            DownloadGroupingMode.ByPlan -> {
                tasks.groupBy { it.planId ?: "standalone" }
                    .map { (planId, groupTasks) ->
                        val title = if (planId == "standalone") "Standalone Downloads" else "Plan: $planId"
                        createGroup(
                            groupKey = "plan_$planId",
                            groupTitle = title,
                            tasks = groupTasks
                        )
                    }
            }
        }
    }

    private fun createGroup(
        groupKey: String,
        groupTitle: String,
        tasks: List<DownloadTaskUiModel>
    ): DownloadGroupUiModel {
        var groupDownloaded = 0L
        var groupTotal = 0L
        var groupSpeed = 0L
        var completedCount = 0

        for (task in tasks) {
            groupDownloaded += task.downloadedBytes
            groupTotal += (task.totalBytes ?: task.downloadedBytes)
            if (task.state.isActive) {
                groupSpeed += task.speedBytesPerSec
            }
            if (task.state == DownloadStateUi.Completed) {
                completedCount++
            }
        }

        val groupPercent = if (groupTotal > 0L) {
            ((groupDownloaded.toDouble() / groupTotal) * 100.0).toInt().coerceIn(0, 100)
        } else 0

        return DownloadGroupUiModel(
            groupKey = groupKey,
            groupTitle = groupTitle,
            tasks = tasks,
            totalBytes = groupTotal,
            downloadedBytes = groupDownloaded,
            progressPercent = groupPercent,
            totalSpeedFormatted = ByteSizeFormatter.formatSpeed(groupSpeed),
            completedCount = completedCount,
            totalCount = tasks.size,
            isExpanded = true
        )
    }
}
