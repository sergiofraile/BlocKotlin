package dev.bloc.sample

import dev.bloc.Bloc
import dev.bloc.sample.examples.stopwatch.StopwatchCubit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// ─────────────────────────────────────────────────────────────────────────────
// These tests demonstrate how straightforward it is to unit-test a Bloc or Cubit.
// No mocking framework is needed — create the object, send events / call methods,
// and assert on `state`. Both examples mirror real-world patterns from the sample app.
//
// Mirrors the iOS ExampleShowcaseTests.
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
// CounterBloc showcase
// ─────────────────────────────────────────────────────────────────────────────

private sealed interface CounterEvent {
    data object Increment : CounterEvent
    data object Decrement : CounterEvent
    data object Reset     : CounterEvent
}

private class ShowcaseCounterBloc : Bloc<Int, CounterEvent>(0) {
    init {
        on<CounterEvent.Increment> { _, emit -> emit(state + 1) }
        on<CounterEvent.Decrement> { _, emit -> emit(state - 1) }
        on<CounterEvent.Reset>     { _, emit -> emit(0) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CounterBlocShowcaseTest {

    @get:Rule val dispatcher = MainDispatcherRule()

    @Test fun `CounterBloc starts at zero`() {
        val bloc = ShowcaseCounterBloc()
        assertEquals(0, bloc.state)
        bloc.close()
    }

    @Test fun `Increment event increases state by one`() {
        val bloc = ShowcaseCounterBloc()
        bloc.send(CounterEvent.Increment)
        bloc.send(CounterEvent.Increment)
        assertEquals(2, bloc.state)
        bloc.close()
    }

    @Test fun `Decrement event decreases state by one`() {
        val bloc = ShowcaseCounterBloc()
        bloc.send(CounterEvent.Increment)
        bloc.send(CounterEvent.Increment)
        bloc.send(CounterEvent.Decrement)
        assertEquals(1, bloc.state)
        bloc.close()
    }

    @Test fun `Reset event returns state to zero regardless of current value`() {
        val bloc = ShowcaseCounterBloc()
        repeat(3) { bloc.send(CounterEvent.Increment) }
        bloc.send(CounterEvent.Reset)
        assertEquals(0, bloc.state)
        bloc.close()
    }

    @Test fun `stateFlow emits every state transition in order`() = runTest(dispatcher.testDispatcher) {
        val bloc = ShowcaseCounterBloc()
        val history = mutableListOf<Int>()
        val job = launch { bloc.stateFlow.toList(history) }

        bloc.send(CounterEvent.Increment)
        bloc.send(CounterEvent.Increment)
        bloc.send(CounterEvent.Reset)

        job.cancel()
        assertEquals(listOf(0, 1, 2, 0), history)
        bloc.close()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// StopwatchCubit showcase
// A Cubit is even simpler to test: call methods directly, then assert on state.
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
class StopwatchCubitShowcaseTest {

    @get:Rule val dispatcher = MainDispatcherRule()

    @Test fun `StopwatchCubit starts stopped at zero elapsed time`() {
        val cubit = StopwatchCubit()
        assertFalse(cubit.state.isRunning)
        assertEquals(0.0, cubit.state.elapsed, 0.001)
        cubit.close()
    }

    @Test fun `start() sets isRunning to true`() {
        val cubit = StopwatchCubit()
        cubit.start()
        assertTrue(cubit.state.isRunning)
        cubit.close()
    }

    @Test fun `pause() stops the stopwatch while preserving elapsed time`() {
        val cubit = StopwatchCubit()
        cubit.start()
        val elapsed = cubit.state.elapsed
        cubit.pause()
        assertFalse(cubit.state.isRunning)
        assertEquals(elapsed, cubit.state.elapsed, 0.001)
        cubit.close()
    }

    @Test fun `reset() returns to the initial stopped-at-zero state`() {
        val cubit = StopwatchCubit()
        cubit.start()
        cubit.reset()
        assertFalse(cubit.state.isRunning)
        assertEquals(0.0, cubit.state.elapsed, 0.001)
        cubit.close()
    }

    @Test fun `start() is a no-op when the stopwatch is already running`() {
        val cubit = StopwatchCubit()
        cubit.start()
        val stateAfterStart = cubit.state
        cubit.start()
        assertEquals(stateAfterStart, cubit.state)
        cubit.close()
    }

    @Test fun `pause() is a no-op when the stopwatch is already paused`() {
        val cubit = StopwatchCubit()
        val stateBeforePause = cubit.state
        cubit.pause()
        assertEquals(stateBeforePause, cubit.state)
        cubit.close()
    }
}
