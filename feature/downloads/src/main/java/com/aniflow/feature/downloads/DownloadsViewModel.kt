package com.aniflow.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aniflow.domain.identity.DownloadTaskId
import com.aniflow.domain.model.aggregate.download.DownloadPriority
import com.aniflow.domain.model.aggregate.download.DownloadSource
import com.aniflow.domain.model.aggregate.download.DownloadTask
import com.aniflow.domain.repository.DownloadRepository
import com.aniflow.domain.state.DownloadState
import com.aniflow.domain.usecase.CancelDownloadUseCase
import com.aniflow.domain.usecase.PauseDownloadUseCase
import com.aniflow.domain.usecase.ResumeDownloadUseCase
import com.aniflow.feature.downloads.aggregator.DownloadDashboardAggregator
import com.aniflow.feature.downloads.model.DownloadEngineBadge
import com.aniflow.feature.downloads.model.DownloadFilterCategory
import com.aniflow.feature.downloads.model.DownloadGroupUiModel
import com.aniflow.feature.downloads.model.DownloadGroupingMode
import com.aniflow.feature.downloads.model.DownloadPriorityUi
import com.aniflow.feature.downloads.model.DownloadSortOption
import com.aniflow.feature.downloads.model.DownloadStateUi
import com.aniflow.feature.downloads.model.DownloadStatisticsUiModel
import com.aniflow.feature.downloads.model.DownloadTaskUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

data class DownloadsUiState(
    val rawTasks: List<DownloadTaskUiModel> = emptyList(),
    val filteredTasks: List<DownloadTaskUiModel> = emptyList(),
    val groupedTasks: List<DownloadGroupUiModel> = emptyList(),
    val selectedFilter: DownloadFilterCategory = DownloadFilterCategory.All,
    val searchQuery: String = "",
    val sortOption: DownloadSortOption = DownloadSortOption.CreatedDate,
    val groupingMode: DownloadGroupingMode = DownloadGroupingMode.Flat,
    val totalDownloadSpeedFormatted: String = "0 B/s",
    val totalUploadSpeedFormatted: String = "0 B/s",
    val overallProgressPercent: Int = 0,
    val activeTasksCount: Int = 0,
    val queuedTasksCount: Int = 0,
    val pausedTasksCount: Int = 0,
    val completedTasksCount: Int = 0,
    val failedTasksCount: Int = 0,
    val statistics: DownloadStatisticsUiModel = DownloadStatisticsUiModel()
)

/**
 * DownloadsViewModel.
 * Real, reactive download manager observing DownloadRepository and controlling
 * pause, resume, and cancellation through domain use cases.
 * Zero hardcoded mock tasks.
 */
class DownloadsViewModel(
    private val downloadRepository: DownloadRepository,
    private val pauseDownloadUseCase: PauseDownloadUseCase,
    private val resumeDownloadUseCase: ResumeDownloadUseCase,
    private val cancelDownloadUseCase: CancelDownloadUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    init {
        observeDownloads()
    }

    private fun observeDownloads() {
        viewModelScope.launch {
            downloadRepository.observeTasks().collect { domainTasks ->
                val uiTasks = domainTasks.map { it.toUiModel() }
                recomputeState(uiTasks)
            }
        }
    }

    private fun recomputeState(uiTasks: List<DownloadTaskUiModel>) {
        val current = _uiState.value

        // Filter
        val filtered = uiTasks.filter { task ->
            val matchesFilter = when (current.selectedFilter) {
                DownloadFilterCategory.All -> true
                DownloadFilterCategory.Active -> task.state.isActive
                DownloadFilterCategory.Queued -> task.state == DownloadStateUi.Queued
                DownloadFilterCategory.Paused -> task.state == DownloadStateUi.Paused
                DownloadFilterCategory.Waiting -> task.state == DownloadStateUi.Waiting
                DownloadFilterCategory.Failed -> task.state == DownloadStateUi.Failed
                DownloadFilterCategory.Completed -> task.state == DownloadStateUi.Completed
            }
            val matchesSearch = current.searchQuery.isBlank() ||
                task.title.contains(current.searchQuery, ignoreCase = true) ||
                task.animeTitle.contains(current.searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }

        // Sort
        val sorted = when (current.sortOption) {
            DownloadSortOption.CreatedDate -> filtered.sortedByDescending { it.createdAt }
            DownloadSortOption.Priority -> filtered.sortedByDescending { it.priority.level }
            DownloadSortOption.Progress -> filtered.sortedByDescending { it.progressPercent }
            DownloadSortOption.Speed -> filtered.sortedByDescending { it.speedBytesPerSec }
            DownloadSortOption.Size -> filtered.sortedByDescending { it.downloadedBytes }
            DownloadSortOption.ETA -> filtered.sortedBy { it.etaSeconds ?: Long.MAX_VALUE }
            DownloadSortOption.Name -> filtered.sortedBy { it.title }
        }

        // Group
        val grouped = DownloadDashboardAggregator.groupTasks(sorted, current.groupingMode)

        // Metrics from aggregator
        val activeCount = uiTasks.count { it.state.isActive }
        val queuedCount = uiTasks.count { it.state == DownloadStateUi.Queued }
        val pausedCount = uiTasks.count { it.state == DownloadStateUi.Paused }
        val completedCount = uiTasks.count { it.state == DownloadStateUi.Completed }
        val failedCount = uiTasks.count { it.state == DownloadStateUi.Failed }

        val totalSpeed = DownloadDashboardAggregator.calculateTotalDownloadSpeed(uiTasks)
        val overallProgress = DownloadDashboardAggregator.calculateOverallProgress(uiTasks)

        _uiState.value = current.copy(
            rawTasks = uiTasks,
            filteredTasks = sorted,
            groupedTasks = grouped,
            totalDownloadSpeedFormatted = totalSpeed,
            overallProgressPercent = overallProgress,
            activeTasksCount = activeCount,
            queuedTasksCount = queuedCount,
            pausedTasksCount = pausedCount,
            completedTasksCount = completedCount,
            failedTasksCount = failedCount
        )
    }

    fun setFilter(filter: DownloadFilterCategory) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        recomputeState(_uiState.value.rawTasks)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        recomputeState(_uiState.value.rawTasks)
    }

    fun setSortOption(sort: DownloadSortOption) {
        _uiState.value = _uiState.value.copy(sortOption = sort)
        recomputeState(_uiState.value.rawTasks)
    }

    fun setGroupingMode(mode: DownloadGroupingMode) {
        _uiState.value = _uiState.value.copy(groupingMode = mode)
        recomputeState(_uiState.value.rawTasks)
    }

    fun pauseTask(taskId: DownloadTaskId) {
        viewModelScope.launch {
            pauseDownloadUseCase(taskId)
        }
    }

    fun resumeTask(taskId: DownloadTaskId) {
        viewModelScope.launch {
            resumeDownloadUseCase(taskId)
        }
    }

    fun cancelTask(taskId: DownloadTaskId) {
        viewModelScope.launch {
            cancelDownloadUseCase(taskId)
        }
    }

    fun pauseAll() {
        viewModelScope.launch {
            _uiState.value.rawTasks.filter { it.state.isActive }.forEach {
                pauseDownloadUseCase(it.id)
            }
        }
    }

    fun resumeAll() {
        viewModelScope.launch {
            _uiState.value.rawTasks.filter { it.state == DownloadStateUi.Paused }.forEach {
                resumeDownloadUseCase(it.id)
            }
        }
    }

    private fun DownloadTask.toUiModel(): DownloadTaskUiModel {
        val taskTitle = when (val s = source) {
            is DownloadSource.TorrentSource -> s.name
            is DownloadSource.HttpSource -> s.fileName
            is DownloadSource.DirectSource -> s.fileName
        }

        val stateUi = when (state) {
            DownloadState.Pending, DownloadState.Queued -> DownloadStateUi.Queued
            DownloadState.Starting -> DownloadStateUi.Starting
            DownloadState.Downloading -> DownloadStateUi.Downloading
            DownloadState.Paused -> DownloadStateUi.Paused
            DownloadState.Waiting -> DownloadStateUi.Waiting
            DownloadState.Retrying -> DownloadStateUi.Retrying
            DownloadState.Verifying -> DownloadStateUi.Verifying
            DownloadState.Moving -> DownloadStateUi.Moving
            DownloadState.Completed -> DownloadStateUi.Completed
            DownloadState.Failed -> DownloadStateUi.Failed
            DownloadState.Cancelled, DownloadState.Removed -> DownloadStateUi.Cancelled
        }

        val priorityUi = when (priority) {
            DownloadPriority.Highest -> DownloadPriorityUi.Highest
            DownloadPriority.High -> DownloadPriorityUi.High
            DownloadPriority.Normal -> DownloadPriorityUi.Normal
            DownloadPriority.Low, DownloadPriority.Lowest -> DownloadPriorityUi.Low
        }

        val engineBadge = if (source is DownloadSource.TorrentSource) {
            DownloadEngineBadge.Torrent
        } else {
            DownloadEngineBadge.HTTP
        }

        return DownloadTaskUiModel(
            id = id,
            title = taskTitle,
            animeTitle = taskTitle.substringBefore("—").substringBefore("-").trim().ifBlank { taskTitle },
            releaseId = releaseId,
            state = stateUi,
            engineBadge = engineBadge,
            priority = priorityUi,
            destinationPath = destination.path,
            createdAt = createdAt,
            errorMessage = errorMessage
        )
    }
}
