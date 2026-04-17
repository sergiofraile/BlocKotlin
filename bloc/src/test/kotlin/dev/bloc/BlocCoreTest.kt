package dev.bloc

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BlocCoreTest {

    @get:Rule val dispatcher = MainDispatcherRule()

    private lateinit var bloc: CounterBloc

    @Before fun setUp()    { bloc = CounterBloc() }
    @After  fun tearDown() { bloc.close() }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test fun `initial state is 0`() = assertEquals(0, bloc.state)

    // ── send + state ──────────────────────────────────────────────────────────

    @Test fun `send Increment updates state`() {
        bloc.send(TestEvent.Increment)
        assertEquals(1, bloc.state)
    }

    @Test fun `send multiple events accumulates state`() {
        repeat(3) { bloc.send(TestEvent.Increment) }
        assertEquals(3, bloc.state)
    }

    @Test fun `send Decrement updates state`() {
        bloc.send(TestEvent.Increment)
        bloc.send(TestEvent.Decrement)
        assertEquals(0, bloc.state)
    }

    @Test fun `send IncrementBy updates state by payload`() {
        bloc.send(TestEvent.IncrementBy(10))
        assertEquals(10, bloc.state)
    }

    // ── eventsFlow ────────────────────────────────────────────────────────────

    @Test fun `eventsFlow receives dispatched events`() = runTest(dispatcher.testDispatcher) {
        val events = mutableListOf<TestEvent>()
        val job = launch { bloc.eventsFlow.toList(events) }

        bloc.send(TestEvent.Increment)
        bloc.send(TestEvent.Decrement)

        job.cancel()
        assertEquals(listOf(TestEvent.Increment, TestEvent.Decrement), events)
    }

    // ── Observer hooks ────────────────────────────────────────────────────────

    @Test fun `onEvent is called for each send`() {
        bloc.send(TestEvent.Increment)
        bloc.send(TestEvent.Decrement)
        assertEquals(listOf(TestEvent.Increment, TestEvent.Decrement), bloc.capturedEvents)
    }

    @Test fun `onTransition contains correct event, currentState, nextState`() {
        bloc.send(TestEvent.Increment)

        assertEquals(1, bloc.capturedTransitions.size)
        val t = bloc.capturedTransitions[0]
        assertEquals(TestEvent.Increment, t.event)
        assertEquals(0, t.currentState)
        assertEquals(1, t.nextState)
    }

    @Test fun `onChange is called after every state change`() {
        bloc.send(TestEvent.Increment)
        bloc.send(TestEvent.IncrementBy(4))

        assertEquals(2, bloc.capturedChanges.size)
        assertEquals(Change(0, 1), bloc.capturedChanges[0])
        assertEquals(Change(1, 5), bloc.capturedChanges[1])
    }

    // ── addError ──────────────────────────────────────────────────────────────

    @Test fun `addError is broadcast on errorsFlow`() = runTest(dispatcher.testDispatcher) {
        val errors = mutableListOf<Throwable>()
        val job = launch { bloc.errorsFlow.toList(errors) }

        val err = RuntimeException("boom")
        bloc.addError(err)

        job.cancel()
        assertEquals(1, errors.size)
        assertEquals(err, errors[0])
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Test fun `send after close is a no-op`() {
        bloc.close()
        bloc.send(TestEvent.Increment)
        assertEquals(0, bloc.state)
    }

    @Test fun `isClosed is false before close`() = assertFalse(bloc.isClosed)

    @Test fun `isClosed is true after close`() {
        bloc.close()
        assertTrue(bloc.isClosed)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EventTransformer tests
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
class EventTransformerTest {

    @get:Rule val dispatcher = MainDispatcherRule()

    // ── Sequential (default) ─────────────────────────────────────────────────

    @Test fun `Sequential - rapid events are all processed`() = runTest(dispatcher.testDispatcher) {
        val bloc = CounterBloc()
        repeat(5) { bloc.send(TestEvent.Increment) }
        advanceUntilIdle()
        assertEquals(5, bloc.state)
        bloc.close()
    }

    // ── Droppable ─────────────────────────────────────────────────────────────

    @Test fun `Droppable - second event dropped while first is running`() =
        runTest(dispatcher.testDispatcher) {
            val bloc = DroppableBloc()

            bloc.send(TestEvent.SlowIncrement(500))  // starts, runs for 500ms
            bloc.send(TestEvent.SlowIncrement(500))  // dropped — first still running

            // Before 500ms: state still 0
            assertEquals(0, bloc.state)

            advanceTimeBy(600)
            advanceUntilIdle()

            // Only one increment happened
            assertEquals(1, bloc.state)
            bloc.close()
        }

    @Test fun `Droppable - new event accepted after first completes`() =
        runTest(dispatcher.testDispatcher) {
            val bloc = DroppableBloc()

            bloc.send(TestEvent.SlowIncrement(100))
            advanceTimeBy(200)
            advanceUntilIdle()
            assertEquals(1, bloc.state)

            bloc.send(TestEvent.SlowIncrement(100))  // accepted — previous finished
            advanceTimeBy(200)
            advanceUntilIdle()
            assertEquals(2, bloc.state)

            bloc.close()
        }

    // ── Restartable ───────────────────────────────────────────────────────────

    @Test fun `Restartable - second event cancels first`() =
        runTest(dispatcher.testDispatcher) {
            val bloc = RestartableBloc()

            bloc.send(TestEvent.SlowIncrement(500))  // starts
            advanceTimeBy(100)
            bloc.send(TestEvent.SlowIncrement(100))  // cancels first, starts new (100ms)

            advanceTimeBy(200)
            advanceUntilIdle()

            // First event was cancelled (never emitted); only second completed
            assertEquals(1, bloc.state)
            bloc.close()
        }

    @Test fun `Restartable - rapid sends result in only last completing`() =
        runTest(dispatcher.testDispatcher) {
            val bloc = RestartableBloc()

            repeat(5) { bloc.send(TestEvent.SlowIncrement(300)) }
            advanceTimeBy(400)
            advanceUntilIdle()

            // Only the last event completed
            assertEquals(1, bloc.state)
            bloc.close()
        }

    // ── Concurrent ────────────────────────────────────────────────────────────

    @Test fun `Concurrent - multiple events start immediately`() =
        runTest(dispatcher.testDispatcher) {
            val bloc = ConcurrentBloc()

            bloc.send(TestEvent.SlowIncrement(200))
            bloc.send(TestEvent.SlowIncrement(200))
            bloc.send(TestEvent.SlowIncrement(200))

            // All three handlers started
            assertEquals(3, bloc.handlerStartCount)

            advanceTimeBy(300)
            advanceUntilIdle()

            assertEquals(3, bloc.state)
            bloc.close()
        }

    // ── Debounce ──────────────────────────────────────────────────────────────

    @Test fun `Debounce - only last event fires after idle period`() =
        runTest(dispatcher.testDispatcher) {
            val bloc = DebounceBloc()

            bloc.send(TestEvent.Increment)   // start 200ms timer
            advanceTimeBy(100)               // cancel — send again
            bloc.send(TestEvent.Increment)
            advanceTimeBy(100)               // cancel — send again
            bloc.send(TestEvent.Increment)   // this is the last one
            advanceTimeBy(300)               // allow 200ms debounce to elapse
            advanceUntilIdle()

            // Only one increment (last event), not three
            assertEquals(1, bloc.state)
            bloc.close()
        }

    @Test fun `Debounce - event fires if no subsequent events before timeout`() =
        runTest(dispatcher.testDispatcher) {
            val bloc = DebounceBloc()

            bloc.send(TestEvent.Increment)
            advanceTimeBy(300)  // 300ms > 200ms debounce
            advanceUntilIdle()

            assertEquals(1, bloc.state)
            bloc.close()
        }
}
