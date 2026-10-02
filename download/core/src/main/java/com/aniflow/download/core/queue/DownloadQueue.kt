package com.aniflow.download.core.queue

import com.aniflow.download.core.model.DownloadPlan
import com.aniflow.download.core.model.DownloadPriority
import com.aniflow.download.core.model.DownloadTask
import com.aniflow.download.core.model.DownloadTaskId
import com.aniflow.download.core.model.DownloadTaskState
import com.aniflow.download.core.model.QueueEvent
import com.aniflow.download.core.planner.TaskFactory
import com.aniflow.download.core.statemachine.DownloadStateMachine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/**
 * Result of plan enqueueing (Section 91).
 */
sealed interface QueueResult {
    data class Enqueued(val tasks: List<DownloadTask>) : QueueResult
    data class Rejected(val reason: String) : QueueResult
    data class Duplicate(val existingTaskId: DownloadTaskId) : QueueResult

    val isSuccess: Boolean get() = this is Enqueued
}

interface QueueStateListener {
    suspend fun onTaskUpdated(task: DownloadTask)
    suspend fun onTaskRemoved(taskId: DownloadTaskId)
}

/**
 * Persistence interface for durable Room backing (Section 27, 29).
 */
interface QueuePersistenceStore {
    suspend fun insertTask(task: DownloadTask)
    suspend fun updateTask(task: DownloadTask)
    suspend fun deleteTask(taskId: String)
    suspend fun loadAllTasks(): List<DownloadTask>
}

class InMemoryQueuePersistenceStore : QueuePersistenceStore {
    private val store = mutableMapOf<String, DownloadTask>()

    override suspend fun insertTask(task: DownloadTask) {
        store[task.id.value] = task
    }

    override suspend fun updateTask(task: DownloadTask) {
        store[task.id.value] = task
    }

    override suspend fun deleteTask(taskId: String) {
        store.remove(taskId)
    }

    override suspend fun loadAllTasks(): List<DownloadTask> {
        return store.values.toList()
    }
}

/**
 * Manages the persistent queue of download tasks (Section 27, 28, 29, 30, 31, 32, 91).
 * Strictly decoupled from Network I/O and UI ViewModels.
 */
class DownloadQueue(
    private val stateListener: QueueStateListener? = null,
    private val persistenceStore: QueuePersistenceStore = InMemoryQueuePersistenceStore(),
    private val taskFactory: TaskFactory = TaskFactory()
) {
    private val mutex = Mutex()
    private val taskMap = mutableMapOf<DownloadTaskId, DownloadTask>()
    private val manualOrderList = mutableListOf<DownloadTaskId>()

    private val _tasksFlow = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasksFlow: Flow<List<DownloadTask>> = _tasksFlow.asStateFlow()

    private val _eventsFlow = MutableSharedFlow<QueueEvent>()
    val eventsFlow: Flow<QueueEvent> = _eventsFlow.asSharedFlow()

    /**
     * Initializes queue state from database persistence (Section 29).
     */
    suspend fun initializeFromPersistence() = mutex.withLock {
        val loaded = persistenceStore.loadAllTasks()
        taskMap.clear()
        manualOrderList.clear()
        loaded.forEach { task ->
            taskMap[task.id] = task
            manualOrderList.add(task.id)
        }
        updateFlow()
    }

    /**
     * Enqueues a full DownloadPlan (Section 91).
     */
    suspend fun enqueue(plan: DownloadPlan): QueueResult = mutex.withLock {
        val itemsToEnqueue = plan.selectedItems
        if (itemsToEnqueue.isEmpty()) {
            return@withLock QueueResult.Rejected("DownloadPlan contains no selected items to enqueue")
        }

        val tasks = itemsToEnqueue.map { item ->
            taskFactory.createTask(
                planId = plan.id,
                item = item,
                capabilities = com.aniflow.download.core.model.RuntimeCapabilities(),
                initialState = DownloadTaskState.Queued
            )
        }

        tasks.forEach { task ->
            taskMap[task.id] = task
            if (!manualOrderList.contains(task.id)) {
                manualOrderList.add(task.id)
            }
            persistenceStore.insertTask(task)
            _eventsFlow.emit(QueueEvent.TaskQueued(task))
        }

        updateFlow()
        tasks.forEach { stateListener?.onTaskUpdated(it) }
        QueueResult.Enqueued(tasks)
    }

    suspend fun enqueue(task: DownloadTask) = mutex.withLock {
        taskMap[task.id] = task
        if (!manualOrderList.contains(task.id)) {
            manualOrderList.add(task.id)
        }
        persistenceStore.insertTask(task)
        _eventsFlow.emit(QueueEvent.TaskQueued(task))
        publishAndNotify(task)
    }

    suspend fun enqueueAll(tasks: List<DownloadTask>) = mutex.withLock {
        tasks.forEach { task ->
            taskMap[task.id] = task
            if (!manualOrderList.contains(task.id)) {
                manualOrderList.add(task.id)
            }
            persistenceStore.insertTask(task)
            _eventsFlow.emit(QueueEvent.TaskQueued(task))
        }
        updateFlow()
        tasks.forEach { stateListener?.onTaskUpdated(it) }
    }

    suspend fun dequeue(): DownloadTask? = mutex.withLock {
        val next = getSortedTasksInternal().firstOrNull { it.state == DownloadTaskState.Queued }
        next
    }

    suspend fun getTask(taskId: DownloadTaskId): DownloadTask? = mutex.withLock {
        taskMap[taskId]
    }

    suspend fun getAllTasks(): List<DownloadTask> = mutex.withLock {
        getSortedTasksInternal()
    }

    suspend fun updateTaskState(taskId: DownloadTaskId, newState: DownloadTaskState): DownloadTask? = mutex.withLock {
        val task = taskMap[taskId] ?: return@withLock null

        // Validate state transition through DownloadStateMachine
        DownloadStateMachine.transition(taskId.value, task.state, newState)

        val updated = task.copy(state = newState, updatedAt = Instant.now())
        taskMap[taskId] = updated
        persistenceStore.updateTask(updated)

        when (newState) {
            DownloadTaskState.Starting -> _eventsFlow.emit(QueueEvent.TaskStarted(taskId))
            DownloadTaskState.Paused -> _eventsFlow.emit(QueueEvent.TaskPaused(taskId))
            DownloadTaskState.Queued -> _eventsFlow.emit(QueueEvent.TaskResumed(taskId))
            DownloadTaskState.Cancelled -> _eventsFlow.emit(QueueEvent.TaskCancelled(taskId))
            DownloadTaskState.Completed -> _eventsFlow.emit(QueueEvent.TaskCompleted(taskId, task.destination.finalFilePath))
            else -> {}
        }

        publishAndNotify(updated)
        updated
    }

    suspend fun updateProgress(
        taskId: DownloadTaskId,
        downloadedBytes: Long,
        totalBytes: Long?,
        speedBps: Long,
        etaSeconds: Long?
    ): DownloadTask? = mutex.withLock {
        val task = taskMap[taskId] ?: return@withLock null
        val updated = task.copy(
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes ?: task.totalBytes,
            speedBytesPerSecond = speedBps,
            etaSeconds = etaSeconds,
            updatedAt = Instant.now()
        )
        taskMap[taskId] = updated
        persistenceStore.updateTask(updated)
        updateFlow()
        updated
    }

    suspend fun pause(taskId: DownloadTaskId): DownloadTask? {
        return updateTaskState(taskId, DownloadTaskState.Paused)
    }

    suspend fun resume(taskId: DownloadTaskId): DownloadTask? {
        return updateTaskState(taskId, DownloadTaskState.Queued)
    }

    suspend fun cancel(taskId: DownloadTaskId, deleteFiles: Boolean = false): DownloadTask? {
        val updated = updateTaskState(taskId, DownloadTaskState.Cancelled)
        if (deleteFiles && updated != null) {
            java.io.File(updated.destination.tempFilePath).delete()
            java.io.File(updated.destination.finalFilePath).delete()
        }
        return updated
    }

    suspend fun retry(taskId: DownloadTaskId): DownloadTask? = mutex.withLock {
        val task = taskMap[taskId] ?: return@withLock null
        val updated = task.copy(
            retryCount = task.retryCount + 1,
            state = DownloadTaskState.Queued,
            lastError = null,
            updatedAt = Instant.now()
        )
        taskMap[taskId] = updated
        persistenceStore.updateTask(updated)
        _eventsFlow.emit(QueueEvent.TaskRetried(taskId))
        publishAndNotify(updated)
        updated
    }

    suspend fun prioritize(taskId: DownloadTaskId, priority: DownloadPriority) = mutex.withLock {
        val task = taskMap[taskId] ?: return@withLock
        val updated = task.copy(priority = priority, updatedAt = Instant.now())
        taskMap[taskId] = updated
        persistenceStore.updateTask(updated)
        publishAndNotify(updated)
    }

    suspend fun setPriority(taskId: DownloadTaskId, priority: DownloadPriority) {
        prioritize(taskId, priority)
    }

    suspend fun reorder(orderedTaskIds: List<DownloadTaskId>) = mutex.withLock {
        manualOrderList.clear()
        manualOrderList.addAll(orderedTaskIds)
        // Add any remaining
        taskMap.keys.forEach { id ->
            if (!manualOrderList.contains(id)) {
                manualOrderList.add(id)
            }
        }
        updateFlow()
    }

    suspend fun moveTop(taskId: DownloadTaskId) = mutex.withLock {
        if (manualOrderList.remove(taskId)) {
            manualOrderList.add(0, taskId)
            updateFlow()
        }
    }

    suspend fun moveBottom(taskId: DownloadTaskId) = mutex.withLock {
        if (manualOrderList.remove(taskId)) {
            manualOrderList.add(taskId)
            updateFlow()
        }
    }

    suspend fun remove(taskId: DownloadTaskId): DownloadTask? = mutex.withLock {
        val removed = taskMap.remove(taskId)
        manualOrderList.remove(taskId)
        if (removed != null) {
            persistenceStore.deleteTask(taskId.value)
        }
        updateFlow()
        stateListener?.onTaskRemoved(taskId)
        removed
    }

    suspend fun getNextEligibleTask(): DownloadTask? = mutex.withLock {
        getSortedTasksInternal().firstOrNull { it.state == DownloadTaskState.Queued }
    }

    private fun getSortedTasksInternal(): List<DownloadTask> {
        return taskMap.values.sortedWith(
            compareByDescending<DownloadTask> { it.priority.value }
                .thenBy { manualOrderList.indexOf(it.id).takeIf { idx -> idx >= 0 } ?: Int.MAX_VALUE }
                .thenBy { it.createdAt }
        )
    }

    private suspend fun publishAndNotify(task: DownloadTask) {
        updateFlow()
        stateListener?.onTaskUpdated(task)
    }

    private fun updateFlow() {
        _tasksFlow.value = getSortedTasksInternal()
    }
}
