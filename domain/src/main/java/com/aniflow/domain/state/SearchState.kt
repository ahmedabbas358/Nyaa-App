package com.aniflow.domain.state

import com.aniflow.domain.error.InvalidStateTransitionException

/**
 * The 9 search states for domain queries and pagination (Section 62).
 */
enum class SearchState {
    Idle,
    Searching,
    LoadingMore,
    Refreshing,
    Success,
    Empty,
    Partial,
    Failed,
    Cancelled;

    val isBusy: Boolean get() = this == Searching || this == LoadingMore || this == Refreshing
    val hasResults: Boolean get() = this == Success || this == Partial
}

/**
 * State machine governing search query lifecycle (Section 63).
 */
object SearchStateMachine {

    private val VALID_TRANSITIONS: Map<SearchState, Set<SearchState>> = mapOf(
        SearchState.Idle to setOf(SearchState.Searching),
        SearchState.Searching to setOf(
            SearchState.Success,
            SearchState.Empty,
            SearchState.Partial,
            SearchState.Failed,
            SearchState.Cancelled
        ),
        SearchState.Success to setOf(
            SearchState.LoadingMore,
            SearchState.Refreshing,
            SearchState.Searching,
            SearchState.Idle
        ),
        SearchState.Empty to setOf(
            SearchState.Refreshing,
            SearchState.Searching,
            SearchState.Idle
        ),
        SearchState.Partial to setOf(
            SearchState.LoadingMore,
            SearchState.Refreshing,
            SearchState.Searching,
            SearchState.Idle
        ),
        SearchState.Failed to setOf(
            SearchState.Searching,
            SearchState.Refreshing,
            SearchState.Idle
        ),
        SearchState.Cancelled to setOf(
            SearchState.Searching,
            SearchState.Idle
        ),
        SearchState.LoadingMore to setOf(
            SearchState.Success,
            SearchState.Partial,
            SearchState.Failed,
            SearchState.Cancelled
        ),
        SearchState.Refreshing to setOf(
            SearchState.Success,
            SearchState.Empty,
            SearchState.Partial,
            SearchState.Failed,
            SearchState.Cancelled
        )
    )

    fun canTransition(current: SearchState, target: SearchState): Boolean {
        return VALID_TRANSITIONS[current]?.contains(target) == true
    }

    fun transition(current: SearchState, target: SearchState): SearchState {
        if (canTransition(current, target)) {
            return target
        }
        throw InvalidStateTransitionException(
            fromState = current.name,
            toState = target.name,
            reason = "Illegal transition in SearchStateMachine from $current to $target"
        )
    }
}
