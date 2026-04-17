package dev.bloc.sample.examples.formulaone

import dev.bloc.Bloc
import kotlinx.coroutines.launch

class FormulaOneBloc : Bloc<FormulaOneState, FormulaOneEvent>(FormulaOneState.Initial) {

    private val networkService = FormulaOneNetworkService()

    init {
        on<FormulaOneEvent.Clear> { _, emit ->
            emit(FormulaOneState.Initial)
        }
        on<FormulaOneEvent.LoadChampionship> { _, emit ->
            emit(FormulaOneState.Loading)
            scope.launch {
                try {
                    val drivers = networkService.fetchDriversChampionship()
                    emit(FormulaOneState.Loaded(drivers))
                } catch (e: Exception) {
                    addError(e)
                    emit(FormulaOneState.Error(e.message ?: "Unknown error"))
                }
            }
        }
    }
}
