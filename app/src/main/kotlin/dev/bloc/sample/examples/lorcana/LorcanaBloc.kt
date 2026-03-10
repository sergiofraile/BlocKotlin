package dev.bloc.sample.examples.lorcana

import dev.bloc.Bloc
import dev.bloc.EventTransformer
import dev.bloc.sample.examples.lorcana.models.LorcanaCard
import dev.bloc.sample.examples.lorcana.models.LorcanaError
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Bloc for managing Lorcana card browsing and search.
 *
 * The Search event is handled with a Debounce(300ms) transformer, which means the
 * network call is only triggered after 300 ms of silence. This replaces manual
 * task-cancellation debounce in the view layer.
 *
 * Demonstrates:
 * - EventTransformer.Debounce for search-as-you-type
 * - Infinite scroll via LoadNextPage
 * - BlocSelector for lightweight footer rebuild
 */
class LorcanaBloc(
    private val networkService: LorcanaNetworkServiceProtocol = LorcanaNetworkService(),
) : Bloc<LorcanaState, LorcanaEvent>(LorcanaState.initial) {

    private val pageSize = 100

    init {
        on<LorcanaEvent.Clear> { _, emit ->
            emit(LorcanaState.initial)
        }

        on<LorcanaEvent.FetchAll> { _, emit ->
            emit(state.copy(isLoading = true, error = null, searchQuery = "", currentPage = 1, cards = emptyList()))
            scope.launch {
                try {
                    val cards = networkService.fetchAllCards(page = 1, pageSize = pageSize)
                    emit(state.copy(cards = cards, isLoading = false, hasMorePages = cards.size == pageSize))
                } catch (e: Exception) {
                    addError(e)
                    emit(state.copy(isLoading = false, error = LorcanaError(e.message ?: "Network error")))
                }
            }
        }

        on<LorcanaEvent.LoadNextPage> { _, emit ->
            if (state.isLoadingMore || state.isLoading || !state.hasMorePages) return@on
            val nextPage = state.currentPage + 1
            emit(state.copy(isLoadingMore = true))
            scope.launch {
                try {
                    val cards = if (state.isSearching) {
                        networkService.searchCards(state.searchQuery, nextPage, pageSize)
                    } else {
                        networkService.fetchAllCards(nextPage, pageSize)
                    }
                    emit(state.copy(
                        cards         = state.cards + cards,
                        currentPage   = nextPage,
                        isLoadingMore = false,
                        hasMorePages  = cards.size == pageSize,
                    ))
                } catch (e: Exception) {
                    addError(e)
                    emit(state.copy(isLoadingMore = false, error = LorcanaError(e.message ?: "Network error")))
                }
            }
        }

        // Debounce search — fires only after 300 ms of silence between keystrokes.
        on<LorcanaEvent.Search>(transformer = EventTransformer.Debounce(300.milliseconds)) { event, emit ->
            val query = event.query
            if (query.length < 3) return@on
            emit(state.copy(isLoading = true, error = null, searchQuery = query, currentPage = 1, cards = emptyList()))
            scope.launch {
                try {
                    val cards = networkService.searchCards(query, 1, pageSize)
                    emit(state.copy(cards = cards, isLoading = false, hasMorePages = cards.size == pageSize))
                } catch (e: Exception) {
                    addError(e)
                    emit(state.copy(isLoading = false, error = LorcanaError(e.message ?: "Network error")))
                }
            }
        }
    }
}
