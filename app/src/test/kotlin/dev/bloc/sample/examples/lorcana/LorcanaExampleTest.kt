package dev.bloc.sample.examples.lorcana

import dev.bloc.sample.MainDispatcherRule
import dev.bloc.sample.examples.lorcana.models.LorcanaCard
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests for [LorcanaBloc] — the debounced search / infinite scroll / BlocSelector showcase.
 *
 * The Search event uses [dev.bloc.EventTransformer.Debounce] (300 ms). Virtual-time control via
 * [advanceTimeBy] lets these tests exercise the debounce window deterministically,
 * just as the iOS LorcanaExampleTests do with Task.sleep.
 *
 * Mirrors the iOS LorcanaExampleTests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LorcanaExampleTest {

    @get:Rule val dispatcher = MainDispatcherRule()

    // ─────────────────────────────────────────────────────────────────────────
    // Mock service
    // ─────────────────────────────────────────────────────────────────────────

    private fun card(name: String, set: String = "The First Chapter") =
        LorcanaCard(name = name, setName = set)

    private val sampleCards = listOf("Elsa", "Mickey", "Simba", "Moana", "Ariel").map { card(it) }

    private inner class MockService(
        val cards: List<LorcanaCard> = sampleCards,
        val error: Exception? = null,
    ) : LorcanaNetworkServiceProtocol {
        override suspend fun fetchAllCards(page: Int, pageSize: Int): List<LorcanaCard> {
            error?.let { throw it }
            return cards
        }
        override suspend fun searchCards(query: String, page: Int, pageSize: Int): List<LorcanaCard> {
            error?.let { throw it }
            return cards.filter { it.name.contains(query, ignoreCase = true) }
        }
        override suspend fun fetchCardsFromSet(setName: String, page: Int, pageSize: Int): List<LorcanaCard> {
            error?.let { throw it }
            return cards.filter { it.setName == setName }
        }
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test fun `LorcanaBloc starts in the empty initial state`() {
        val bloc = LorcanaBloc(MockService())
        assertTrue(bloc.state.cards.isEmpty())
        assertFalse(bloc.state.isLoading)
        assertNull(bloc.state.error)
        bloc.close()
    }

    // ── FetchAll ──────────────────────────────────────────────────────────────

    @Test fun `fetchAllCards populates cards when service succeeds`() = runTest(dispatcher.testDispatcher) {
        val bloc = LorcanaBloc(MockService())
        bloc.send(LorcanaEvent.FetchAll)
        advanceUntilIdle()

        assertEquals(sampleCards, bloc.state.cards)
        assertFalse(bloc.state.isLoading)
        assertNull(bloc.state.error)
        bloc.close()
    }

    @Test fun `fetchAllCards sets error state when service throws`() = runTest(dispatcher.testDispatcher) {
        val bloc = LorcanaBloc(MockService(error = Exception("Network offline")))
        bloc.send(LorcanaEvent.FetchAll)
        advanceUntilIdle()

        assertNotNull(bloc.state.error)
        assertFalse(bloc.state.isLoading)
        bloc.close()
    }

    // ── Clear ─────────────────────────────────────────────────────────────────

    @Test fun `clear event resets state to initial`() = runTest(dispatcher.testDispatcher) {
        val bloc = LorcanaBloc(MockService())
        bloc.send(LorcanaEvent.FetchAll)
        advanceUntilIdle()
        assertTrue(bloc.state.cards.isNotEmpty())

        bloc.send(LorcanaEvent.Clear)
        assertTrue(bloc.state.cards.isEmpty())
        assertEquals("", bloc.state.searchQuery)
        assertNull(bloc.state.error)
        bloc.close()
    }

    // ── Search (debounced) ────────────────────────────────────────────────────

    @Test fun `search query shorter than 3 characters is silently ignored`() = runTest(dispatcher.testDispatcher) {
        val bloc = LorcanaBloc(MockService())
        bloc.send(LorcanaEvent.Search("El"))
        advanceTimeBy(400L)
        advanceUntilIdle()

        assertTrue("Short queries should not trigger a search", bloc.state.cards.isEmpty())
        bloc.close()
    }

    @Test fun `search fires after the 300ms debounce window settles`() = runTest(dispatcher.testDispatcher) {
        val bloc = LorcanaBloc(MockService())
        bloc.send(LorcanaEvent.Search("Elsa"))
        advanceTimeBy(400L)   // 400ms > 300ms debounce
        advanceUntilIdle()

        assertEquals("Elsa", bloc.state.searchQuery)
        assertTrue(bloc.state.cards.all { it.name.contains("Elsa", ignoreCase = true) })
        bloc.close()
    }

    @Test fun `rapid search keystrokes result in only the last query firing`() = runTest(dispatcher.testDispatcher) {
        val bloc = LorcanaBloc(MockService())

        bloc.send(LorcanaEvent.Search("Els"))    // starts 300ms timer
        advanceTimeBy(100L)
        bloc.send(LorcanaEvent.Search("Elsa"))   // resets timer
        advanceTimeBy(100L)
        bloc.send(LorcanaEvent.Search("Mickey")) // resets timer again
        advanceTimeBy(400L)                       // let the last debounce settle
        advanceUntilIdle()

        assertEquals("Mickey", bloc.state.searchQuery)
        assertTrue(bloc.state.cards.all { it.name.contains("Mickey", ignoreCase = true) })
        bloc.close()
    }

    @Test fun `search that matches multiple cards returns all matching results`() = runTest(dispatcher.testDispatcher) {
        val cards = listOf(
            card("Elsa - Snow Queen"),
            card("Mickey Mouse"),
            card("Elsa - Ice Queen"),
        )
        val bloc = LorcanaBloc(MockService(cards = cards))
        bloc.send(LorcanaEvent.Search("Elsa"))
        advanceTimeBy(400L)
        advanceUntilIdle()

        assertEquals(2, bloc.state.cards.size)
        assertTrue(bloc.state.cards.all { it.name.contains("Elsa", ignoreCase = true) })
        bloc.close()
    }

    // ── Pagination guard ──────────────────────────────────────────────────────

    @Test fun `loadNextPage is a no-op when hasMorePages is false`() = runTest(dispatcher.testDispatcher) {
        val bloc = LorcanaBloc(MockService())
        bloc.send(LorcanaEvent.FetchAll)
        advanceUntilIdle()

        // Our mock returns fewer cards than pageSize (100), so hasMorePages = false.
        assertFalse(bloc.state.hasMorePages)

        val countBefore = bloc.state.cards.size
        bloc.send(LorcanaEvent.LoadNextPage)
        advanceUntilIdle()

        assertEquals(countBefore, bloc.state.cards.size)
        bloc.close()
    }

    @Test fun `loadNextPage is a no-op while already loading`() = runTest(dispatcher.testDispatcher) {
        val bloc = LorcanaBloc(MockService())

        // Trigger a fetch to set isLoading = true (hasn't advanced yet).
        bloc.send(LorcanaEvent.FetchAll)

        // A concurrent LoadNextPage while isLoading is true should be dropped.
        bloc.send(LorcanaEvent.LoadNextPage)
        advanceUntilIdle()

        // The main FetchAll result should still arrive correctly.
        assertFalse(bloc.state.isLoading)
        bloc.close()
    }
}
