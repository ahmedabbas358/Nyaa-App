package com.aniflow.domain.model.aggregate.library

import com.aniflow.domain.identity.LibraryItemId
import com.aniflow.domain.identity.MediaIdentity
import com.aniflow.domain.state.LibraryItemState
import com.aniflow.domain.valueobject.StorageTarget
import java.time.Instant

/**
 * Domain entity representing an indexed item within the user's local library (Section 56).
 */
data class LibraryItem(
    val id: LibraryItemId,
    val mediaIdentity: MediaIdentity,
    val state: LibraryItemState = LibraryItemState.Indexed,
    val location: StorageTarget = StorageTarget.DEFAULT,
    val indexedAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
