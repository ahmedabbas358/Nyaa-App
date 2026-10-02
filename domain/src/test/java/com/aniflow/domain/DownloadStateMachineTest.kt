package com.aniflow.domain

import com.aniflow.domain.error.InvalidStateTransitionException
import com.aniflow.domain.state.DownloadState
import com.aniflow.domain.state.DownloadStateMachine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadStateMachineTest {

    @Test
    fun `Verifies entire happy path lifecycle`() {
        var state = DownloadState.Pending
        state = DownloadStateMachine.transition(state, DownloadState.Queued)
        assertEquals(DownloadState.Queued, state)

        state = DownloadStateMachine.transition(state, DownloadState.Starting)
        assertEquals(DownloadState.Starting, state)

        state = DownloadStateMachine.transition(state, DownloadState.Downloading)
        assertEquals(DownloadState.Downloading, state)

        state = DownloadStateMachine.transition(state, DownloadState.Verifying)
        assertEquals(DownloadState.Verifying, state)

        state = DownloadStateMachine.transition(state, DownloadState.Moving)
        assertEquals(DownloadState.Moving, state)

        state = DownloadStateMachine.transition(state, DownloadState.Completed)
        assertEquals(DownloadState.Completed, state)

        state = DownloadStateMachine.transition(state, DownloadState.Removed)
        assertEquals(DownloadState.Removed, state)
    }

    @Test
    fun `Verifies pause and resume cycle`() {
        val paused = DownloadStateMachine.transition(DownloadState.Downloading, DownloadState.Paused)
        assertEquals(DownloadState.Paused, paused)

        val resumed = DownloadStateMachine.transition(DownloadState.Paused, DownloadState.Downloading)
        assertEquals(DownloadState.Downloading, resumed)
    }

    @Test
    fun `Verifies retry and failure cycle`() {
        val failed = DownloadStateMachine.transition(DownloadState.Downloading, DownloadState.Failed)
        assertEquals(DownloadState.Failed, failed)

        val retrying = DownloadStateMachine.transition(DownloadState.Failed, DownloadState.Retrying)
        assertEquals(DownloadState.Retrying, retrying)

        val starting = DownloadStateMachine.transition(DownloadState.Retrying, DownloadState.Starting)
        assertEquals(DownloadState.Starting, starting)
    }

    @Test
    fun `Forbids illegal jumps according to Section 60`() {
        // Direct jump from Downloading to Completed (must verify and move first!)
        assertFalse(DownloadStateMachine.canTransition(DownloadState.Downloading, DownloadState.Completed))
        assertThrows(InvalidStateTransitionException::class.java) {
            DownloadStateMachine.transition(DownloadState.Downloading, DownloadState.Completed)
        }

        // Direct jump from Pending to Downloading
        assertFalse(DownloadStateMachine.canTransition(DownloadState.Pending, DownloadState.Downloading))

        // Direct jump from Queued to Completed
        assertFalse(DownloadStateMachine.canTransition(DownloadState.Queued, DownloadState.Completed))

        // Completed back to Downloading
        assertFalse(DownloadStateMachine.canTransition(DownloadState.Completed, DownloadState.Downloading))
    }
}
