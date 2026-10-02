package com.aniflow.download.http

import java.io.File
import java.io.RandomAccessFile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe writer for multi-segment file downloads (Section 59, 60).
 * Coordinates writes to a shared .aniflow.part file without opening conflicting file handles.
 */
class SegmentWriter(
    private val partFile: File
) : AutoCloseable {

    private val mutex = Mutex()
    private val raf: RandomAccessFile

    init {
        partFile.parentFile?.mkdirs()
        raf = RandomAccessFile(partFile, "rw")
    }

    suspend fun writeChunk(offset: Long, data: ByteArray, length: Int) = mutex.withLock {
        raf.seek(offset)
        raf.write(data, 0, length)
    }

    suspend fun truncate(totalBytes: Long) = mutex.withLock {
        raf.setLength(totalBytes)
    }

    override fun close() {
        raf.close()
    }
}
