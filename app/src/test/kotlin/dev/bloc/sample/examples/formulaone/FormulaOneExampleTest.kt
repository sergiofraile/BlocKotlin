package dev.bloc.sample.examples.formulaone

import dev.bloc.Bloc
import dev.bloc.sample.MainDispatcherRule
import dev.bloc.sample.examples.formulaone.models.Driver
import dev.bloc.sample.examples.formulaone.models.DriverChampionship
import dev.bloc.sample.examples.formulaone.models.Team
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests for the Formula One example — async Bloc with sealed-interface states.
 *
 * The real [FormulaOneBloc] hardcodes [FormulaOneNetworkService]. These tests introduce an
 * injectable service interface so the full async path (Loading → Loaded / Error) can be
 * exercised without real network calls — the recommended pattern for any Bloc that
 * performs I/O.
 *
 * Mirrors the iOS FormulaOneExampleTests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FormulaOneExampleTest {

    @get:Rule val dispatcher = MainDispatcherRule()

    // ─────────────────────────────────────────────────────────────────────────
    // Testable variant with injectable service
    // ─────────────────────────────────────────────────────────────────────────

    /** Minimal service contract used only within this test file. */
    private interface F1Service {
        suspend fun fetchDrivers(): List<DriverChampionship>
    }

    /** Builds a testable Bloc variant that delegates fetching to an injected [F1Service]. */
    private fun makeBloc(service: F1Service): Bloc<FormulaOneState, FormulaOneEvent> =
        object : Bloc<FormulaOneState, FormulaOneEvent>(FormulaOneState.Initial) {
            init {
                on<FormulaOneEvent.Clear> { _, emit ->
                    emit(FormulaOneState.Initial)
                }
                on<FormulaOneEvent.LoadChampionship> { _, emit ->
                    emit(FormulaOneState.Loading)
                    scope.launch {
                        try {
                            emit(FormulaOneState.Loaded(service.fetchDrivers()))
                        } catch (e: Exception) {
                            addError(e)
                            emit(FormulaOneState.Error(e.message ?: "Unknown error"))
                        }
                    }
                }
            }
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Mock helpers
    // ─────────────────────────────────────────────────────────────────────────

    private val mockDrivers = listOf(
        DriverChampionship(
            classificationId = 1, position = 1, points = 437, wins = 12,
            driver = Driver("Max", "Verstappen", "NLD", "1997-09-30", 1, "VER"),
            team   = Team("Red Bull Racing", "NLD"),
        ),
        DriverChampionship(
            classificationId = 2, position = 2, points = 374, wins = 7,
            driver = Driver("Lando", "Norris", "GBR", "1999-11-13", 4, "NOR"),
            team   = Team("McLaren", "GBR"),
        ),
        DriverChampionship(
            classificationId = 3, position = 3, points = 356, wins = 6,
            driver = Driver("Charles", "Leclerc", "MCO", "1997-10-16", 16, "LEC"),
            team   = Team("Ferrari", "ITA"),
        ),
    )

    private fun successService() = object : F1Service {
        override suspend fun fetchDrivers() = mockDrivers
    }

    private fun failingService() = object : F1Service {
        override suspend fun fetchDrivers(): List<DriverChampionship> =
            throw Exception("Network error: server down")
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test fun `FormulaOneBloc starts in the initial state`() {
        val bloc = makeBloc(successService())
        assertTrue(bloc.state is FormulaOneState.Initial)
        bloc.close()
    }

    // ── Clear ─────────────────────────────────────────────────────────────────

    @Test fun `clear event resets state to initial from any other state`() {
        val bloc = makeBloc(successService())
        bloc.send(FormulaOneEvent.LoadChampionship)   // → Loading
        bloc.send(FormulaOneEvent.Clear)              // → Initial
        assertTrue(bloc.state is FormulaOneState.Initial)
        bloc.close()
    }

    // ── loadChampionship (success) ────────────────────────────────────────────

    @Test fun `loadChampionship transitions to Loaded with mock driver data`() = runTest(dispatcher.testDispatcher) {
        val bloc = makeBloc(successService())
        bloc.send(FormulaOneEvent.LoadChampionship)
        advanceUntilIdle()

        val loaded = bloc.state as? FormulaOneState.Loaded
        assertEquals(mockDrivers, loaded?.drivers)
        bloc.close()
    }

    @Test fun `loaded drivers are in the correct order`() = runTest(dispatcher.testDispatcher) {
        val bloc = makeBloc(successService())
        bloc.send(FormulaOneEvent.LoadChampionship)
        advanceUntilIdle()

        val loaded = bloc.state as FormulaOneState.Loaded
        assertEquals(1, loaded.drivers[0].position)
        assertEquals(2, loaded.drivers[1].position)
        assertEquals(3, loaded.drivers[2].position)
        bloc.close()
    }

    // ── loadChampionship (failure) ────────────────────────────────────────────

    @Test fun `loadChampionship emits Error state when service throws`() = runTest(dispatcher.testDispatcher) {
        val bloc = makeBloc(failingService())
        bloc.send(FormulaOneEvent.LoadChampionship)
        advanceUntilIdle()

        assertTrue(bloc.state is FormulaOneState.Error)
        bloc.close()
    }

    @Test fun `loadChampionship publishes on errorsFlow when service throws`() = runTest(dispatcher.testDispatcher) {
        val bloc = makeBloc(failingService())
        val errors = mutableListOf<Throwable>()
        val job = launch { bloc.errorsFlow.toList(errors) }

        bloc.send(FormulaOneEvent.LoadChampionship)
        advanceUntilIdle()

        assertEquals(1, errors.size)
        job.cancel()
        bloc.close()
    }

    @Test fun `clear after error resets to initial state`() = runTest(dispatcher.testDispatcher) {
        val bloc = makeBloc(failingService())
        bloc.send(FormulaOneEvent.LoadChampionship)
        advanceUntilIdle()
        assertTrue(bloc.state is FormulaOneState.Error)

        bloc.send(FormulaOneEvent.Clear)
        assertTrue(bloc.state is FormulaOneState.Initial)
        bloc.close()
    }
}
