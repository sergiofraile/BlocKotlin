package dev.bloc.sample.examples.lorcana

import dev.bloc.sample.examples.lorcana.models.LorcanaCard
import dev.bloc.sample.examples.lorcana.models.LorcanaError

data class LorcanaState(
    val cards: List<LorcanaCard>    = emptyList(),
    val searchQuery: String         = "",
    val currentPage: Int            = 1,
    val hasMorePages: Boolean       = true,
    val isLoading: Boolean          = false,
    val isLoadingMore: Boolean      = false,
    val error: LorcanaError?        = null,
) {
    val isSearching: Boolean get() = searchQuery.isNotEmpty()

    companion object {
        val initial = LorcanaState()
    }
}
