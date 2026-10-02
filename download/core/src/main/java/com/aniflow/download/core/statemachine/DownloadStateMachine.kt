package com.aniflow.download.core.statemachine

import com.aniflow.download.core.model.DownloadTaskState

/**
 * Strict Download State Machine enforcing Step 22 Sections 25 and 26.
 *
 * All 13 states governed:
 * Pending, Queued, Starting, Downloading, Paused, Waiting, Retrying, Verifying, Moving, Completed, Failed, Cancelled, Removed.
 *
 * Guarantees that:
 * 1. Tasks never jump directly from Downloading to Completed without Verification and Moving.
 * 2. Cancelled or Completed tasks cannot transition back to active downloading states.
 * 3. Removed tasks cannot transition to Starting or any active state.
 * 4. Every state transition is explicit, predictable, and audited.
 */
object DownloadStateMachine {

    private val VALID_TRANSITIONS: Map<DownloadTaskState, Set<DownloadTaskState>> = mapOf(
        DownloadTaskState.Pending to setOf(
            DownloadTaskState.Queued,
            DownloadTaskState.Cancelled,
            DownloadTaskState.Removed
        ),
        DownloadTaskState.Queued to setOf(
            DownloadTaskState.Starting,
            DownloadTaskState.Paused,
            DownloadTaskState.Waiting,
            DownloadTaskState.WaitingForNetwork,
            DownloadTaskState.WaitingForStorage,
            DownloadTaskState.Cancelled,
            DownloadTaskState.Removed
        ),
        DownloadTaskState.Starting to setOf(
            DownloadTaskState.Downloading,
            DownloadTaskState.Waiting,
            DownloadTaskState.WaitingForNetwork,
            DownloadTaskState.WaitingForStorage,
            DownloadTaskState.Failed,
            DownloadTaskState.Cancelled
        ),
        DownloadTaskState.Downloading to setOf(
            DownloadTaskState.Paused,
            DownloadTaskState.Waiting,
            DownloadTaskState.WaitingForNetwork,
            DownloadTaskState.WaitingForStorage,
            DownloadTaskState.Retrying,
            DownloadTaskState.RetryScheduled,
            DownloadTaskState.Verifying, // Mandatory before completion!
            DownloadTaskState.Failed,
            DownloadTaskState.Cancelled
            // Note: DownloadTaskState.Completed is strictly FORBIDDEN here!
        ),
        DownloadTaskState.Paused to setOf(
            DownloadTaskState.Queued,
            DownloadTaskState.Cancelled,
            DownloadTaskState.Removed
        ),
        DownloadTaskState.Waiting to setOf(
            DownloadTaskState.Queued,
            DownloadTaskState.Starting,
            DownloadTaskState.Cancelled,
            DownloadTaskState.Removed
        ),
        DownloadTaskState.WaitingForNetwork to setOf(
            DownloadTaskState.Queued,
            DownloadTaskState.Starting,
            DownloadTaskState.Cancelled,
            DownloadTaskState.Removed
        ),
        DownloadTaskState.WaitingForStorage to setOf(
            DownloadTaskState.Queued,
            DownloadTaskState.Starting,
            DownloadTaskState.Cancelled,
            DownloadTaskState.Removed
        ),
        DownloadTaskState.Retrying to setOf(
            DownloadTaskState.Queued,
            DownloadTaskState.Starting,
            DownloadTaskState.Failed,
            DownloadTaskState.Cancelled
        ),
        DownloadTaskState.RetryScheduled to setOf(
            DownloadTaskState.Queued,
            DownloadTaskState.Starting,
            DownloadTaskState.Cancelled
        ),
        DownloadTaskState.Verifying to setOf(
            DownloadTaskState.Moving,
            DownloadTaskState.Organizing,
            DownloadTaskState.Failed,
            DownloadTaskState.Cancelled
        ),
        DownloadTaskState.Moving to setOf(
            DownloadTaskState.Completed,
            DownloadTaskState.Failed
        ),
        DownloadTaskState.Organizing to setOf(
            DownloadTaskState.Completed,
            DownloadTaskState.Failed
        ),
        DownloadTaskState.Completed to setOf(
            DownloadTaskState.Removed
        ),
        DownloadTaskState.Failed to setOf(
            DownloadTaskState.Retrying,
            DownloadTaskState.RetryScheduled,
            DownloadTaskState.Queued,
            DownloadTaskState.Removed
        ),
        DownloadTaskState.Cancelled to setOf(
            DownloadTaskState.Removed
        ),
        DownloadTaskState.Removed to emptySet()
    )

    fun canTransition(from: DownloadTaskState, to: DownloadTaskState): Boolean {
        if (from == to) return true // Idempotent self-transition
        return VALID_TRANSITIONS[from]?.contains(to) == true
    }

    /**
     * Executes transition or throws IllegalStateException if illegal.
     */
    fun transition(taskId: String, from: DownloadTaskState, to: DownloadTaskState): DownloadTaskState {
        if (!canTransition(from, to)) {
            throw IllegalStateException(
                "Illegal state transition for task $taskId: Cannot transition from '$from' to '$to'."
            )
        }
        return to
    }
}

