package dev.bloc.sample.examples.lorcana

sealed interface LorcanaEvent {
    data object         Clear        : LorcanaEvent
    data object         FetchAll     : LorcanaEvent
    data object         LoadNextPage : LorcanaEvent
    data class          Search(val query: String) : LorcanaEvent
}
