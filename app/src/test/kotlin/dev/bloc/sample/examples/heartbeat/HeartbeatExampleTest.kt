package dev.bloc.sample.examples.heartbeat

import dev.bloc.sample.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for [HeartbeatBloc] — the scoped lifecycle / DisposableEffect example.
 *
 * The real HeartbeatBloc starts an internal one-second ticker coroutine on [HeartbeatEvent.Start].
 * Rather than waiting for real wall-clock time, these tests drive the Bloc by sending
 * [HeartbeatEvent.Tick] directly — the recommended pattern for testing async Blocs with
 * internal timers: decouple "what happens on each tick" from "when ticks are scheduled".
 *
 * Mirrors the iOS HeartbeatExampleTests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HeartbeatExampleTest {

    @get:Rule val dispatcher = MainDispatcherRule()

    private lateinit var bloc: HeartbeatBloc

    @Before fun setUp()    { bloc = HeartbeatBloc() }
    @After  fun tearDown() { if (!bloc.isClosed) bloc.close() }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test fun `HeartbeatBloc starts stopped with zero ticks`() {
        assertEquals(0, bloc.state.tickCount)
        assertFalse(bloc.state.isRunning)
    }

    // ── Start ─────────────────────────────────────────────────────────────────

    @Test fun `start event sets isRunning to true and resets tick count to zero`() {
        bloc.send(HeartbeatEvent.Start)
        assertTrue(bloc.state.isRunning)
        assertEquals(0, bloc.state.tickCount)
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    @Test fun `Each tick event increments the tick count by one`() {
        bloc.send(HeartbeatEvent.Start)
        bloc.send(HeartbeatEvent.Tick)
        bloc.send(HeartbeatEvent.Tick)
        bloc.send(HeartbeatEvent.Tick)
        assertEquals(3, bloc.state.tickCount)
    }

    @Test fun `Tick events accumulate correctly over many ticks`() {
        bloc.send(HeartbeatEvent.Start)
        repeat(60) { bloc.send(HeartbeatEvent.Tick) }
        assertEquals(60, bloc.state.tickCount)
    }

    // ── Close ─────────────────────────────────────────────────────────────────

    @Test fun `close cancels the Bloc so subsequent send is a no-op`() {
        bloc.send(HeartbeatEvent.Start)
        bloc.send(HeartbeatEvent.Tick)
        assertEquals(1, bloc.state.tickCount)

        bloc.close()
        assertTrue(bloc.isClosed)

        // After close, further tick events must be silently dropped.
        bloc.send(HeartbeatEvent.Tick)
        assertEquals(1, bloc.state.tickCount)
    }

    @Test fun `closedFlow becomes true when close is called`() {
        assertFalse(bloc.closedFlow.value)
        bloc.close()
        assertTrue(bloc.closedFlow.value)
    }

    // ── Formatted duration ────────────────────────────────────────────────────

    @Test fun `formattedDuration formats 65 ticks as 01 colon 05`() {
        bloc.send(HeartbeatEvent.Start)
        repeat(65) { bloc.send(HeartbeatEvent.Tick) }
        assertEquals("01:05", bloc.state.formattedDuration)
    }

    @Test fun `formattedDuration formats 0 ticks as 00 colon 00`() {
        assertEquals("00:00", bloc.state.formattedDuration)
    }

    @Test fun `formattedDuration formats 3600 ticks as 60 colon 00`() {
        bloc.send(HeartbeatEvent.Start)
        repeat(3600) { bloc.send(HeartbeatEvent.Tick) }
        assertEquals("60:00", bloc.state.formattedDuration)
    }
}
