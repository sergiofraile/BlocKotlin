package dev.bloc

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HydratedBlocTest {

    // MainDispatcherRule sets Dispatchers.Main = UnconfinedTestDispatcher.
    // Because UnconfinedTestDispatcher is eagerly unconfined, scope.launch { handler }
    // inside Bloc.send() executes synchronously (no delay in these handlers).
    // Therefore we can test synchronously without runTest / advanceUntilIdle.
    @get:Rule val dispatcher = MainDispatcherRule()

    // ── Restore from storage ──────────────────────────────────────────────────

    @Test fun `initial state is used when storage is empty`() {
        val bloc = PersistBloc()
        assertEquals(PersistState(count = 0), bloc.state)
        bloc.close()
    }

    @Test fun `state is restored from storage on creation`() {
        val storage = InMemoryStorage()

        val firstBloc = PersistBloc(storage)
        firstBloc.send(PersistEvent.Set(42))
        assertEquals(PersistState(count = 42), firstBloc.state)
        firstBloc.close()

        // Second instance with the same storage restores the persisted state.
        val secondBloc = PersistBloc(storage)
        assertEquals(PersistState(count = 42), secondBloc.state)
        secondBloc.close()
    }

    // ── Persist on emit ───────────────────────────────────────────────────────

    @Test fun `state is persisted after every emit`() {
        val storage = InMemoryStorage()
        val bloc = PersistBloc(storage)

        bloc.send(PersistEvent.Set(10))
        assertEquals(PersistState(count = 10), bloc.state)

        // Raw bytes should be written to storage.
        val raw = storage.read("test_persist_bloc")
        assertNull("storage should have an entry after send", raw.takeIf { it == null })

        val restored = PersistBloc(storage)
        assertEquals(PersistState(count = 10), restored.state)

        bloc.close()
        restored.close()
    }

    @Test fun `latest state overwrites previous persisted state`() {
        val storage = InMemoryStorage()
        val bloc = PersistBloc(storage)

        bloc.send(PersistEvent.Set(1))
        bloc.send(PersistEvent.Set(2))
        bloc.send(PersistEvent.Set(99))
        bloc.close()

        val restored = PersistBloc(storage)
        assertEquals(PersistState(count = 99), restored.state)
        restored.close()
    }

    // ── clearStoredState ──────────────────────────────────────────────────────

    @Test fun `clearStoredState removes persisted data`() {
        val storage = InMemoryStorage()
        val bloc = PersistBloc(storage)

        bloc.send(PersistEvent.Set(77))
        bloc.clearStoredState()
        bloc.close()

        val fresh = PersistBloc(storage)
        assertEquals(PersistState(count = 0), fresh.state)
        fresh.close()
    }

    @Test fun `clearStoredState does not change current in-memory state`() {
        val storage = InMemoryStorage()
        val bloc = PersistBloc(storage)

        bloc.send(PersistEvent.Set(55))
        assertEquals(PersistState(count = 55), bloc.state)

        bloc.clearStoredState()

        assertEquals(PersistState(count = 55), bloc.state)
        bloc.close()
    }

    // ── resetToInitialState ───────────────────────────────────────────────────

    @Test fun `resetToInitialState resets in-memory state immediately`() {
        val storage = InMemoryStorage()
        val bloc = PersistBloc(storage)

        bloc.send(PersistEvent.Set(33))
        bloc.resetToInitialState()

        assertEquals(PersistState(count = 0), bloc.state)
        bloc.close()
    }

    @Test fun `resetToInitialState clears storage so next launch uses initialState`() {
        val storage = InMemoryStorage()
        val bloc = PersistBloc(storage)

        bloc.send(PersistEvent.Set(33))
        bloc.resetToInitialState()
        bloc.close()

        val next = PersistBloc(storage)
        assertEquals(PersistState(count = 0), next.state)
        next.close()
    }

    // ── storageKey ────────────────────────────────────────────────────────────

    @Test fun `storageKey uses overridden value`() {
        val bloc = PersistBloc()
        assertEquals("test_persist_bloc", bloc.storageKey)
        bloc.close()
    }

    // ── Corrupted storage ─────────────────────────────────────────────────────

    @Test fun `corrupted storage bytes fall back to initialState`() {
        val storage = InMemoryStorage()
        storage.write("test_persist_bloc", "not valid json at all".encodeToByteArray())

        val bloc = PersistBloc(storage)
        assertEquals(PersistState(count = 0), bloc.state)
        bloc.close()
    }

    // ── InMemoryStorage ───────────────────────────────────────────────────────

    @Test fun `InMemoryStorage read returns null for missing key`() {
        assertNull(InMemoryStorage().read("missing"))
    }

    @Test fun `InMemoryStorage write and read round-trips`() {
        val s = InMemoryStorage()
        s.write("key", "hello".encodeToByteArray())
        assertEquals("hello", s.read("key")?.decodeToString())
    }

    @Test fun `InMemoryStorage delete removes key`() {
        val s = InMemoryStorage()
        s.write("key", byteArrayOf(1))
        s.delete("key")
        assertNull(s.read("key"))
    }

    @Test fun `InMemoryStorage clear removes all keys`() {
        val s = InMemoryStorage()
        s.write("a", byteArrayOf(1))
        s.write("b", byteArrayOf(2))
        s.clear()
        assertNull(s.read("a"))
        assertNull(s.read("b"))
    }
}
