package com.aniflow.domain.model.aggregate.library

import com.aniflow.domain.identity.FileFingerprint
import com.aniflow.domain.identity.LibraryFileId
import com.aniflow.domain.identity.LibraryItemId
import com.aniflow.domain.valueobject.ByteSize
import java.time.Instant

/**
 * Domain entity representing a physical or virtual file within a library item (Section 57).
 */
data class LibraryFile(
    val id: LibraryFileId,
    val libraryItemId: LibraryItemId,
    val path: String,
    val fileName: String,
    val size: ByteSize,
    val modifiedAt: Instant = Instant.now(),
    val fingerprint: FileFingerprint? = null
) {
    init {
        require(path.isNotBlank()) { "LibraryFile path cannot be blank" }
        require(fileName.isNotBlank()) { "LibraryFile fileName cannot be blank" }
    }
}
