package dev.bloc.sample.examples.counter

import dev.bloc.HydratedBloc
import dev.bloc.HydratedStorage
import dev.bloc.sample.InMemoryStorage
import dev.bloc.sample.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.serializer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Tests for the Counter example — a [HydratedBloc] that persists its count across sessions.
 *
 * Uses an injectable [InMemoryStorage] to keep tests hermetic: no SharedPreferences or Android
 * context is needed. The persistence round-trip (save → new instance → restore) is exercised
 * exactly as the iOS CounterExampleTests do.
 *
 * Mirrors the iOS CounterExampleTests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CounterExampleTest {

    @get:Rule val dispatcher = MainDispatcherRule()

    // Inline testable variant of CounterBloc that accepts injectable storage.
    private fun makeBloc(storage: HydratedStorage = InMemoryStorage()) =
        object : HydratedBloc<Int, CounterEvent>(
            initialState    = 0,
            serializer      = serializer<Int>(),
            storageKeyParam = "counter_test",
            storage         = storage,
        ) {
            init {
                on<CounterEvent.Increment> { _, emit -> emit(state + 1) }
                on<CounterEvent.Decrement> { _, emit -> emit(state - 1) }
                on<CounterEvent.Reset>     { _, emit -> emit(0) }
            }
        }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test fun `CounterBloc starts at zero`() {
        val bloc = makeBloc()
        assertEquals(0, bloc.state)
        bloc.close()
    }

    // ── Increment ─────────────────────────────────────────────────────────────

    @Test fun `Increment raises the count`() {
        val bloc = makeBloc()
        bloc.send(CounterEvent.Increment)
        bloc.send(CounterEvent.Increment)
        bloc.send(CounterEvent.Increment)
        assertEquals(3, bloc.state)
        bloc.close()
    }

    // ── Decrement ─────────────────────────────────────────────────────────────

    @Test fun `Decrement lowers the count, allowing negative values`() {
        val bloc = makeBloc()
        bloc.send(CounterEvent.Decrement)
        assertEquals(-1, bloc.state)
        bloc.close()
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    @Test fun `Reset returns the count to zero from any value`() {
        val bloc = makeBloc()
        repeat(3) { bloc.send(CounterEvent.Increment) }
        bloc.send(CounterEvent.Reset)
        assertEquals(0, bloc.state)
        bloc.close()
    }

    // ── HydratedBloc persistence ──────────────────────────────────────────────

    @Test fun `HydratedBloc rehydrates the count from a previous session`() {
        val storage = InMemoryStorage()

        val firstSession = makeBloc(storage)
        repeat(3) { firstSession.send(CounterEvent.Increment) }
        assertEquals(3, firstSession.state)
        firstSession.close()

        // Simulate a new app launch with the same storage — state should be restored.
        val secondSession = makeBloc(storage)
        assertEquals(3, secondSession.state)
        secondSession.close()
    }

    @Test fun `Latest emitted state is what gets persisted`() {
        val storage = InMemoryStorage()
        val bloc = makeBloc(storage)

        bloc.send(CounterEvent.Increment)
        bloc.send(CounterEvent.Increment)
        bloc.send(CounterEvent.Decrement)
        bloc.close()

        val restored = makeBloc(storage)
        assertEquals(1, restored.state)
        restored.close()
    }

    // ── clearStoredState ──────────────────────────────────────────────────────

    @Test fun `clearStoredState removes storage without affecting in-memory state`() {
        val storage = InMemoryStorage()
        val bloc = makeBloc(storage)
        bloc.send(CounterEvent.Increment)
        bloc.clearStoredState()

        // In-memory state is unchanged.
        assertEquals(1, bloc.state)

        // A new instance with the same storage starts at 0 (storage wiped).
        val fresh = makeBloc(storage)
        assertEquals(0, fresh.state)

        bloc.close()
        fresh.close()
    }

    // ── resetToInitialState ───────────────────────────────────────────────────

    @Test fun `resetToInitialState resets in-memory state and clears storage`() {
        val storage = InMemoryStorage()
        val bloc = makeBloc(storage)
        repeat(5) { bloc.send(CounterEvent.Increment) }

        bloc.resetToInitialState()
        assertEquals(0, bloc.state)

        val fresh = makeBloc(storage)
        assertEquals(0, fresh.state)
        bloc.close()
        fresh.close()
    }
}
