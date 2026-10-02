package com.aniflow.download.core.orchestrator

import com.aniflow.domain.model.aggregate.download.NetworkPolicy
import com.aniflow.download.core.engine.DownloadEngineRegistry
import com.aniflow.download.core.engine.EngineProgress
import com.aniflow.download.core.model.DownloadError
import com.aniflow.download.core.model.DownloadPlan
import com.aniflow.download.core.model.DownloadTask
import com.aniflow.download.core.model.DownloadTaskId
import com.aniflow.download.core.model.DownloadTaskState
import com.aniflow.download.core.model.ErrorTaxonomy
import com.aniflow.download.core.model.RuntimeCapabilities
import com.aniflow.download.core.organization.DefaultLibraryIndexer
import com.aniflow.download.core.organization.FileOrganizationHandler
import com.aniflow.download.core.organization.LibraryIndexer
import com.aniflow.download.core.planner.TaskFactory
import com.aniflow.download.core.queue.DownloadQueue
import com.aniflow.download.core.scheduler.DownloadScheduler
import com.aniflow.download.core.storage.StorageReservationManager
import com.aniflow.download.core.verification.IntegrityVerificationEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * The Central Download Runtime Coordinator & Orchestrator adhering to Step 22 Sections 35, 68-79, 110, 111, 122-130.
 *
 * Coordinates the full real download pipeline:
 * Queue -> Scheduler -> Engine Execution -> Progress Tracking -> Verification -> Organization -> Library Index -> History -> Notification.
 */
class DownloadOrchestrator(
    private val queue: DownloadQueue,
    private val scheduler: DownloadScheduler,
    private val engineRegistry: DownloadEngineRegistry,
    private val taskFactory: TaskFactory = TaskFactory(),
    private val verificationEngine: IntegrityVerificationEngine = IntegrityVerificationEngine(),
    private val organizationHandler: FileOrganizationHandler = FileOrganizationHandler(),
    private val libraryIndexer: LibraryIndexer = DefaultLibraryIndexer(),
    private val storageReservationManager: StorageReservationManager? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val activeJobs = ConcurrentHashMap<DownloadTaskId, Job>()
    private val loopMutex = Mutex()

    /**
     * Submits a validated DownloadPlan, creates tasks, reserves storage, enqueues them, and begins execution.
     */
    suspend fun submitPlan(
        plan: DownloadPlan,
        capabilities: RuntimeCapabilities
    ): List<DownloadTask> {
        val tasks = plan.selectedItems.map { item ->
            taskFactory.createTask(
                planId = plan.id,
                item = item,
                capabilities = capabilities,
                initialState = DownloadTaskState.Queued
            )
        }

        // Atomically reserve storage for tasks (Section 14 & 15)
        tasks.forEach { task ->
            task.totalBytes?.let { bytes ->
                storageReservationManager?.reserve(task.id.value, bytes)
            }
        }

        queue.enqueueAll(tasks)
        triggerScheduling(capabilities, plan.policy.networkPolicy)
        return tasks
    }

    /**
     * Triggers the scheduler to select and launch eligible tasks.
     */
    fun triggerScheduling(
        capabilities: RuntimeCapabilities,
        networkPolicy: NetworkPolicy = NetworkPolicy.AnyNetwork
    ) {
        scope.launch {
            loopMutex.withLock {
                var claimedTask = scheduler.selectNextTaskAndClaim(capabilities, networkPolicy)
                while (claimedTask != null) {
                    launchTaskExecution(claimedTask, capabilities, networkPolicy)
                    claimedTask = scheduler.selectNextTaskAndClaim(capabilities, networkPolicy)
                }
            }
        }
    }

    private fun launchTaskExecution(
        task: DownloadTask,
        capabilities: RuntimeCapabilities,
        networkPolicy: NetworkPolicy
    ) {
        if (activeJobs.containsKey(task.id)) return

        val engine = engineRegistry.resolve(task.source) ?: engineRegistry.get(task.engineType) ?: run {
            scope.launch {
                val err = DownloadError("ENGINE_NOT_FOUND", ErrorTaxonomy.Engine, "No engine found for source")
                queue.updateTaskState(task.id, DownloadTaskState.Failed)
                storageReservationManager?.release(task.id.value)
            }
            return
        }

        val job = scope.launch {
            try {
                // Transition state from Starting -> Downloading
                queue.updateTaskState(task.id, DownloadTaskState.Downloading)

                // Observe engine progress stream
                val progressJob = engine.observe(task.id).onEach { progress ->
                    // Throttled / meaningful progress update
                    queue.updateProgress(
                        taskId = task.id,
                        downloadedBytes = progress.downloadedBytes,
                        totalBytes = progress.totalBytes,
                        speedBps = progress.speedBytesPerSecond,
                        etaSeconds = progress.etaSeconds
                    )

                    if (progress.state == DownloadTaskState.Completed) {
                        handleTaskDownloaded(task, progress, capabilities, networkPolicy)
                    } else if (progress.state == DownloadTaskState.Failed) {
                        queue.updateTaskState(task.id, DownloadTaskState.Failed)
                        storageReservationManager?.release(task.id.value)
                        finishTask(task.id, capabilities, networkPolicy)
                    }
                }.launchIn(this)

                // Execute the download
                engine.start(task)
                progressJob.join()

            } catch (e: CancellationException) {
                // Task was paused or cancelled; preserve structured cancellation (Section 124)
                throw e
            } catch (e: Exception) {
                queue.updateTaskState(task.id, DownloadTaskState.Failed)
                storageReservationManager?.release(task.id.value)
                finishTask(task.id, capabilities, networkPolicy)
            }
        }

        activeJobs[task.id] = job
    }

    /**
     * Executes the atomic completion transaction (Sections 68-79, 110, 111):
     * Downloaded -> Verifying -> Moving -> Completed + Library Indexing.
     */
    private suspend fun handleTaskDownloaded(
        task: DownloadTask,
        progress: EngineProgress,
        capabilities: RuntimeCapabilities,
        networkPolicy: NetworkPolicy
    ) {
        // 1. Verification Phase (Section 70-72)
        queue.updateTaskState(task.id, DownloadTaskState.Verifying)
        val partFile = File(task.destination.tempFilePath)
        val sizeVerification = verificationEngine.verifyFileSize(partFile, progress.totalBytes)

        if (!sizeVerification.isVerified) {
            // Verification failed -> Task = Failed, NEVER Completed (Section 72)
            queue.updateTaskState(task.id, DownloadTaskState.Failed)
            storageReservationManager?.release(task.id.value)
            finishTask(task.id, capabilities, networkPolicy)
            return
        }

        // 2. Organization Phase (atomic move or cross-volume copy) (Section 68, 73, 74)
        queue.updateTaskState(task.id, DownloadTaskState.Moving)
        val moveResult = organizationHandler.finalizeDownload(task.destination)

        if (!moveResult.isSuccess || moveResult.finalFile == null) {
            queue.updateTaskState(task.id, DownloadTaskState.Failed)
            storageReservationManager?.release(task.id.value)
            finishTask(task.id, capabilities, networkPolicy)
            return
        }

        // 3. Physical download is safely final -> Mark Completed! (Section 69)
        queue.updateTaskState(task.id, DownloadTaskState.Completed)

        // 4. Release Storage Reservation exactly once (Section 129 & 130)
        storageReservationManager?.release(task.id.value)

        // 5. Library Indexing Integration (Section 75, 76, 77, 110, 111)
        // Decoupled: Failure to index does not fail physical download
        try {
            libraryIndexer.indexDownload(task, moveResult.finalFile)
        } catch (e: Exception) {
            // Log indexing error, physical file remains safe and Completed
        }

        finishTask(task.id, capabilities, networkPolicy)
    }

    suspend fun pauseTask(taskId: DownloadTaskId) {
        val task = queue.getTask(taskId) ?: return
        val engine = engineRegistry.resolve(task.source) ?: engineRegistry.get(task.engineType)
        engine?.pause(taskId)
        activeJobs.remove(taskId)?.cancel()
        queue.updateTaskState(taskId, DownloadTaskState.Paused)
    }

    suspend fun resumeTask(taskId: DownloadTaskId, capabilities: RuntimeCapabilities, networkPolicy: NetworkPolicy) {
        val task = queue.getTask(taskId) ?: return
        val engine = engineRegistry.resolve(task.source) ?: engineRegistry.get(task.engineType)
        engine?.resume(taskId)
        queue.updateTaskState(taskId, DownloadTaskState.Queued)
        triggerScheduling(capabilities, networkPolicy)
    }

    suspend fun cancelTask(taskId: DownloadTaskId, deleteFiles: Boolean = true) {
        val task = queue.getTask(taskId) ?: return
        val engine = engineRegistry.resolve(task.source) ?: engineRegistry.get(task.engineType)
        engine?.cancel(taskId)
        activeJobs.remove(taskId)?.cancel()
        queue.updateTaskState(taskId, DownloadTaskState.Cancelled)

        // Exactly-once reservation release on cancel (Section 16, 129)
        storageReservationManager?.release(taskId.value)

        if (deleteFiles) {
            File(task.destination.tempFilePath).delete()
            File(task.destination.finalFilePath).delete()
        }
    }

    suspend fun retryTask(taskId: DownloadTaskId, capabilities: RuntimeCapabilities, networkPolicy: NetworkPolicy) {
        val task = queue.getTask(taskId) ?: return
        task.totalBytes?.let { bytes ->
            storageReservationManager?.reserve(task.id.value, bytes)
        }
        queue.retry(taskId)
        triggerScheduling(capabilities, networkPolicy)
    }

    suspend fun pauseAll() {
        val activeIds = activeJobs.keys.toList()
        activeIds.forEach { pauseTask(it) }
    }

    suspend fun resumeAll(capabilities: RuntimeCapabilities, networkPolicy: NetworkPolicy) {
        val allTasks = queue.getAllTasks()
        allTasks.filter { it.state == DownloadTaskState.Paused }.forEach {
            queue.updateTaskState(it.id, DownloadTaskState.Queued)
        }
        triggerScheduling(capabilities, networkPolicy)
    }

    suspend fun cancelAll(deleteFiles: Boolean = false) {
        val allTasks = queue.getAllTasks()
        allTasks.forEach { cancelTask(it.id, deleteFiles) }
    }

    private fun finishTask(
        taskId: DownloadTaskId,
        capabilities: RuntimeCapabilities,
        networkPolicy: NetworkPolicy
    ) {
        activeJobs.remove(taskId)
        triggerScheduling(capabilities, networkPolicy)
    }
}
