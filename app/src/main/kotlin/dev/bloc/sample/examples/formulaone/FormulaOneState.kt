package dev.bloc.sample.examples.formulaone

import dev.bloc.sample.examples.formulaone.models.DriverChampionship

sealed interface FormulaOneState {
    data object Initial : FormulaOneState
    data object Loading : FormulaOneState
    data class  Loaded(val drivers: List<DriverChampionship>) : FormulaOneState
    data class  Error(val message: String) : FormulaOneState
}
