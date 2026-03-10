package dev.bloc.sample.examples.formulaone

sealed interface FormulaOneEvent {
    data object LoadChampionship : FormulaOneEvent
    data object Clear            : FormulaOneEvent
}
