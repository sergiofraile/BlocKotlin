package dev.bloc

import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

/**
 * Tests for [BlocRegistry] — type-safe Bloc/Cubit resolution.
 *
 * Mirrors the iOS BlocRegistryTests.
 */
class BlocRegistryTest {

    @get:Rule val dispatcher = MainDispatcherRule()

    @After fun tearDown() {
        BlocRegistry.closeAll()
    }

    // ── Registration + resolution ─────────────────────────────────────────────

    @Test fun `resolve returns the exact registered Bloc instance`() {
        val bloc = CounterBloc()
        BlocRegistry.register(bloc)

        val resolved = BlocRegistry.resolve(CounterBloc::class)
        assertSame(bloc, resolved)
        bloc.close()
    }

    @Test fun `resolve returns the exact registered Cubit instance`() {
        val cubit = CounterCubit()
        BlocRegistry.register(cubit)

        val resolved = BlocRegistry.resolve(CounterCubit::class)
        assertSame(cubit, resolved)
        cubit.close()
    }

    @Test fun `registry can hold multiple distinct types simultaneously`() {
        val bloc  = CounterBloc()
        val cubit = CounterCubit()
        BlocRegistry.register(bloc, cubit)

        assertSame(bloc,  BlocRegistry.resolve(CounterBloc::class))
        assertSame(cubit, BlocRegistry.resolve(CounterCubit::class))
        bloc.close()
        cubit.close()
    }

    // ── Error handling ────────────────────────────────────────────────────────

    @Test(expected = IllegalStateException::class)
    fun `resolve throws IllegalStateException for an unregistered type`() {
        BlocRegistry.closeAll()
        BlocRegistry.resolve(CounterBloc::class)
    }

    // ── closeAll ──────────────────────────────────────────────────────────────

    @Test fun `closeAll marks all registered blocs as closed`() {
        val bloc  = CounterBloc()
        val cubit = CounterCubit()
        BlocRegistry.register(bloc, cubit)

        BlocRegistry.closeAll()

        assert(bloc.isClosed)  { "Bloc should be closed after closeAll" }
        assert(cubit.isClosed) { "Cubit should be closed after closeAll" }
    }

    @Test fun `resolve throws after closeAll clears the store`() {
        val bloc = CounterBloc()
        BlocRegistry.register(bloc)
        BlocRegistry.closeAll()

        try {
            BlocRegistry.resolve(CounterBloc::class)
            throw AssertionError("Expected IllegalStateException")
        } catch (_: IllegalStateException) { /* pass */ }
    }
}
