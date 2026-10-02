package com.aniflow.feature.library.queue

import com.aniflow.platform.storage.model.StorageFile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.PriorityQueue

enum class IndexPriority(val rank: Int) {
    High(1),    // Just downloaded media (immediate user feedback)
    Normal(2),  // Manual single-file / single-folder scans
    Low(3)      // Historical bulk imports (10,000+ files)
}

data class IndexTask(
    val taskId: String,
    val file: StorageFile,
    val priority: IndexPriority,
    val enqueuedEpochMillis: Long = System.currentTimeMillis()
) : Comparable<IndexTask> {
    override fun compareTo(other: IndexTask): Int {
        val rankComp = this.priority.rank.compareTo(other.priority.rank)
        return if (rankComp != 0) rankComp else this.enqueuedEpochMillis.compareTo(other.enqueuedEpochMillis)
    }
}

/**
 * LibraryIndexQueue (Sections 108, 109, 110, 111, 112, 113, 169, 170).
 * Prioritized queue for indexing newly completed downloads ahead of background bulk operations,
 * ensuring high responsiveness in the UI.
 */
class LibraryIndexQueue {

    private val mutex = Mutex()
    private val queue = PriorityQueue<IndexTask>()

    suspend fun enqueue(task: IndexTask) = mutex.withLock {
        queue.offer(task)
    }

    suspend fun enqueueBatch(tasks: List<IndexTask>) = mutex.withLock {
        tasks.forEach { queue.offer(it) }
    }

    suspend fun poll(): IndexTask? = mutex.withLock {
        queue.poll()
    }

    suspend fun size(): Int = mutex.withLock {
        queue.size
    }

    suspend fun clear() = mutex.withLock {
        queue.clear()
    }
}
