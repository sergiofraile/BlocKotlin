package dev.bloc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the lightweight value types used throughout the library:
 * [Change], [Transition], and [BlocError].
 *
 * Mirrors the iOS ModelTests.
 */
class ModelTest {

    // ── Change ────────────────────────────────────────────────────────────────

    @Test fun `Change stores currentState and nextState`() {
        val change = Change(currentState = 0, nextState = 1)
        assertEquals(0, change.currentState)
        assertEquals(1, change.nextState)
    }

    @Test fun `Change equality holds for same values`() {
        assertEquals(Change(currentState = 0, nextState = 1), Change(currentState = 0, nextState = 1))
    }

    @Test fun `Change toString contains both state values`() {
        val change = Change(currentState = "idle", nextState = "loading")
        val str = change.toString()
        assertTrue("toString should contain currentState", str.contains("idle"))
        assertTrue("toString should contain nextState", str.contains("loading"))
    }

    // ── Transition ────────────────────────────────────────────────────────────

    private sealed interface SampleEvent {
        data object DoIt : SampleEvent
    }

    @Test fun `Transition stores currentState, event, and nextState`() {
        val t = Transition(currentState = 0, event = SampleEvent.DoIt, nextState = 1)
        assertEquals(0, t.currentState)
        assertEquals(SampleEvent.DoIt, t.event)
        assertEquals(1, t.nextState)
    }

    @Test fun `Transition equality holds for same values`() {
        assertEquals(
            Transition(currentState = 0, event = SampleEvent.DoIt, nextState = 1),
            Transition(currentState = 0, event = SampleEvent.DoIt, nextState = 1),
        )
    }

    @Test fun `Transition toString contains all three field values`() {
        val t = Transition(currentState = 0, event = SampleEvent.DoIt, nextState = 1)
        val str = t.toString()
        assertTrue("toString should contain currentState", str.contains("0"))
        assertTrue("toString should contain nextState", str.contains("1"))
    }

    // ── BlocError ─────────────────────────────────────────────────────────────

    @Test fun `BlocError is a Throwable`() {
        val error: Throwable = BlocError("test error")
        assertTrue(error is BlocError)
        assertEquals("test error", error.message)
    }

    @Test fun `BlocError can wrap a cause`() {
        val cause = RuntimeException("root cause")
        val error = BlocError("wrapper", cause)
        assertEquals(cause, error.cause)
    }

    @Test fun `BlocError with no cause has null cause`() {
        val error = BlocError("standalone")
        assertEquals(null, error.cause)
    }
}
