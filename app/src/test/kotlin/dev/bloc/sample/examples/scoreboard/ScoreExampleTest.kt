package dev.bloc.sample.examples.scoreboard

import dev.bloc.sample.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for [ScoreBloc] — the BlocListener / BlocBuilder(buildWhen) / BlocConsumer showcase.
 *
 * ScoreBloc intentionally has no UI dependency; the business logic — milestone detection
 * and tier boundaries — is exercised by collecting stateFlow directly, exactly as the
 * Compose components (BlocListener + BlocBuilder) would observe it in the real app.
 *
 * Mirrors the iOS ScoreExampleTests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScoreExampleTest {

    @get:Rule val dispatcher = MainDispatcherRule()

    private lateinit var bloc: ScoreBloc

    @Before fun setUp()    { bloc = ScoreBloc() }
    @After  fun tearDown() { bloc.close() }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test fun `ScoreBloc starts at zero`() {
        assertEquals(0, bloc.state)
    }

    // ── AddPoint ──────────────────────────────────────────────────────────────

    @Test fun `addPoint increments the score by one each time`() {
        bloc.send(ScoreEvent.AddPoint)
        bloc.send(ScoreEvent.AddPoint)
        bloc.send(ScoreEvent.AddPoint)
        assertEquals(3, bloc.state)
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    @Test fun `reset returns the score to zero from any value`() {
        repeat(7) { bloc.send(ScoreEvent.AddPoint) }
        bloc.send(ScoreEvent.Reset)
        assertEquals(0, bloc.state)
    }

    // ── Milestone detection (BlocListener pattern) ────────────────────────────

    /**
     * Mirrors the BlocListener milestone logic from the real ScoreScreen:
     * a side effect fires at every 5-point boundary (5, 10, 15, …).
     */
    @Test fun `Milestone detection - every 5th point triggers a side effect`() = runTest(dispatcher.testDispatcher) {
        val milestones = mutableListOf<Int>()
        val job = launch {
            var previous = 0
            bloc.stateFlow.collect { score ->
                if (score > 0 && score % 5 == 0 && score != previous) {
                    milestones.add(score)
                    previous = score
                }
            }
        }

        repeat(12) { bloc.send(ScoreEvent.AddPoint) }
        job.cancel()

        assertEquals(listOf(5, 10), milestones)
    }

    // ── Tier badge (BlocBuilder buildWhen pattern) ────────────────────────────

    /**
     * Mirrors the BlocBuilder(buildWhen) tier-badge logic from the real ScoreScreen:
     * the badge only redraws when the score crosses a 10-point tier boundary.
     */
    @Test fun `Tier badge only redraws at 10-point tier boundaries`() = runTest(dispatcher.testDispatcher) {
        val tierChanges = mutableListOf<Int>()
        val job = launch {
            var previousTier = 0
            bloc.stateFlow.collect { score ->
                val tier = score / 10
                if (tier != previousTier) {
                    previousTier = tier
                    tierChanges.add(score)
                }
            }
        }

        repeat(25) { bloc.send(ScoreEvent.AddPoint) }
        job.cancel()

        assertEquals(listOf(10, 20), tierChanges)
    }

    // ── stateFlow continuity ──────────────────────────────────────────────────

    @Test fun `stateFlow emits every score change in arrival order`() = runTest(dispatcher.testDispatcher) {
        val states = mutableListOf<Int>()
        val job = launch { bloc.stateFlow.toList(states) }

        bloc.send(ScoreEvent.AddPoint)
        bloc.send(ScoreEvent.AddPoint)
        bloc.send(ScoreEvent.Reset)

        job.cancel()
        assertEquals(listOf(0, 1, 2, 0), states)
    }

    @Test fun `Score increments correctly for many consecutive addPoint events`() {
        repeat(100) { bloc.send(ScoreEvent.AddPoint) }
        assertEquals(100, bloc.state)
    }
}
