package dev.bloc

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CubitTest {

    @get:Rule val dispatcher = MainDispatcherRule()

    private lateinit var cubit: CounterCubit

    @Before fun setUp() { cubit = CounterCubit() }
    @After  fun tearDown() { cubit.close() }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test fun `initial state is 0`() {
        assertEquals(0, cubit.state)
    }

    @Test fun `stateFlow initial value is 0`() = runTest(dispatcher.testDispatcher) {
        assertEquals(0, cubit.stateFlow.value)
    }

    // ── emit ──────────────────────────────────────────────────────────────────

    @Test fun `increment updates state to 1`() {
        cubit.increment()
        assertEquals(1, cubit.state)
    }

    @Test fun `multiple emits accumulate correctly`() {
        repeat(5) { cubit.increment() }
        assertEquals(5, cubit.state)
    }

    @Test fun `decrement updates state`() {
        cubit.increment()
        cubit.decrement()
        assertEquals(0, cubit.state)
    }

    @Test fun `reset returns state to 0`() {
        repeat(3) { cubit.increment() }
        cubit.reset()
        assertEquals(0, cubit.state)
    }

    // ── Deduplication ────────────────────────────────────────────────────────

    @Test fun `emitting same state is a no-op`() = runTest(dispatcher.testDispatcher) {
        val states = mutableListOf<Int>()
        val job = launch { cubit.stateFlow.toList(states) }

        cubit.increment()   // 0 → 1
        cubit.reset()       // 1 → 0
        cubit.reset()       // 0 == 0  → no emission
        cubit.increment()   // 0 → 1

        job.cancel()
        // States should be [0, 1, 0, 1] — no duplicate 0
        assertEquals(listOf(0, 1, 0, 1), states)
    }

    // ── stateFlow ─────────────────────────────────────────────────────────────

    @Test fun `stateFlow emits each state change`() = runTest(dispatcher.testDispatcher) {
        val emitted = mutableListOf<Int>()
        val job = launch { cubit.stateFlow.toList(emitted) }

        cubit.increment()
        cubit.increment()
        cubit.decrement()

        job.cancel()
        assertEquals(listOf(0, 1, 2, 1), emitted)
    }

    // ── errorsFlow ────────────────────────────────────────────────────────────

    @Test fun `addError is broadcast on errorsFlow`() = runTest(dispatcher.testDispatcher) {
        // Using a custom Cubit subclass to expose addError
        val trackingCubit = object : Cubit<Int>(0) {
            fun fireError(t: Throwable) = addError(t)
        }
        val errors = mutableListOf<Throwable>()
        val job = launch { trackingCubit.errorsFlow.toList(errors) }

        val error = RuntimeException("test error")
        trackingCubit.fireError(error)

        job.cancel()
        trackingCubit.close()
        assertEquals(1, errors.size)
        assertEquals(error, errors[0])
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Test fun `isClosed is false before close`() {
        assertFalse(cubit.isClosed)
    }

    @Test fun `isClosed is true after close`() {
        cubit.close()
        assertTrue(cubit.isClosed)
    }

    @Test fun `emit after close is a no-op`() {
        cubit.close()
        cubit.increment()
        assertEquals(0, cubit.state)
    }

    @Test fun `close is idempotent`() {
        cubit.close()
        cubit.close()   // must not throw
        assertTrue(cubit.isClosed)
    }

    // ── BlocObserver hooks ────────────────────────────────────────────────────

    @Test fun `onChange is invoked with correct Change`() {
        val changes = mutableListOf<Change<Int>>()
        BlocObserver.shared = object : BlocObserver() {
            override fun <S : Any> onChange(emitter: StateEmitter<S>, change: Change<S>) {
                @Suppress("UNCHECKED_CAST")
                changes += change as Change<Int>
            }
        }

        cubit.increment()
        cubit.increment()

        assertEquals(2, changes.size)
        assertEquals(Change(currentState = 0, nextState = 1), changes[0])
        assertEquals(Change(currentState = 1, nextState = 2), changes[1])

        BlocObserver.shared = BlocObserver()
    }

    @Test fun `onCreate is invoked on construction`() {
        var created = false
        BlocObserver.shared = object : BlocObserver() {
            override fun <S : Any> onCreate(emitter: StateEmitter<S>) { created = true }
        }

        CounterCubit().close()

        assertTrue(created)
        BlocObserver.shared = BlocObserver()
    }

    @Test fun `onClose is invoked on close`() {
        var closed = false
        BlocObserver.shared = object : BlocObserver() {
            override fun <S : Any> onClose(emitter: StateEmitter<S>) { closed = true }
        }

        cubit.close()

        assertTrue(closed)
        BlocObserver.shared = BlocObserver()
    }
}
