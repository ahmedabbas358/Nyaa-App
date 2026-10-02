package com.aniflow.domain.state

import com.aniflow.domain.error.InvalidStateTransitionException

/**
 * The 13 canonical states of a download operation (Section 59).
 */
enum class DownloadState {
    Pending,
    Queued,
    Starting,
    Downloading,
    Paused,
    Waiting,
    Retrying,
    Verifying,
    Moving,
    Completed,
    Failed,
    Cancelled,
    Removed;

    val isTerminal: Boolean
        get() = this == Completed || this == Cancelled || this == Removed

    val isActive: Boolean
        get() = this == Starting || this == Downloading || this == Retrying || this == Verifying || this == Moving

    val isPausedOrWaiting: Boolean
        get() = this == Paused || this == Waiting || this == Queued
}

/**
 * Strict Download State Machine preventing invalid state jumps (Section 60 & 100).
 */
object DownloadStateMachine {

    /**
     * Complete transition matrix according to Section 60:
     * Pending → Queued
     * Queued → Starting
     * Starting → Downloading, Failed
     * Downloading → Paused, Waiting, Retrying, Verifying, Failed, Cancelled
     * Paused → Queued, Downloading, Cancelled
     * Waiting → Starting, Cancelled
     * Retrying → Starting, Failed, Cancelled
     * Verifying → Moving, Failed
     * Moving → Completed, Failed
     * Completed → Removed
     * Failed → Retrying, Queued, Removed
     * Cancelled → Removed
     */
    private val VALID_TRANSITIONS: Map<DownloadState, Set<DownloadState>> = mapOf(
        DownloadState.Pending to setOf(DownloadState.Queued, DownloadState.Cancelled),
        DownloadState.Queued to setOf(DownloadState.Starting, DownloadState.Cancelled),
        DownloadState.Starting to setOf(DownloadState.Downloading, DownloadState.Failed, DownloadState.Cancelled),
        DownloadState.Downloading to setOf(
            DownloadState.Paused,
            DownloadState.Waiting,
            DownloadState.Retrying,
            DownloadState.Verifying,
            DownloadState.Failed,
            DownloadState.Cancelled
        ),
        DownloadState.Paused to setOf(DownloadState.Queued, DownloadState.Downloading, DownloadState.Cancelled),
        DownloadState.Waiting to setOf(DownloadState.Starting, DownloadState.Cancelled),
        DownloadState.Retrying to setOf(DownloadState.Starting, DownloadState.Failed, DownloadState.Cancelled),
        DownloadState.Verifying to setOf(DownloadState.Moving, DownloadState.Failed),
        DownloadState.Moving to setOf(DownloadState.Completed, DownloadState.Failed),
        DownloadState.Completed to setOf(DownloadState.Removed),
        DownloadState.Failed to setOf(DownloadState.Retrying, DownloadState.Queued, DownloadState.Removed),
        DownloadState.Cancelled to setOf(DownloadState.Removed),
        DownloadState.Removed to emptySet()
    )

    fun canTransition(current: DownloadState, target: DownloadState): Boolean {
        return VALID_TRANSITIONS[current]?.contains(target) == true
    }

    /**
     * Executes state transition or throws a typed domain exception if illegal.
     */
    fun transition(current: DownloadState, target: DownloadState): DownloadState {
        if (canTransition(current, target)) {
            return target
        }
        throw InvalidStateTransitionException(
            fromState = current.name,
            toState = target.name,
            reason = "Illegal transition from $current to $target. State jumps are forbidden by Section 60."
        )
    }
}
