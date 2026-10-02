package com.aniflow.domain.state

/**
 * State of an item in the local library index (Section 102).
 */
enum class LibraryItemState {
    Indexed,
    Available,
    PartiallyAvailable,
    Missing,
    Stale,
    Invalid
}
