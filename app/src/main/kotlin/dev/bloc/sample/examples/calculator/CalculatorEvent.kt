package dev.bloc.sample.examples.calculator

enum class Operation(val symbol: String) {
    Add("+"), Subtract("−"), Multiply("×"), Divide("÷")
}

sealed interface CalculatorEvent {
    data class  Digit(val d: Int)        : CalculatorEvent
    data class  Op(val op: Operation)    : CalculatorEvent
    data object Equals                   : CalculatorEvent
    data object Clear                    : CalculatorEvent
    data object Delete                   : CalculatorEvent
    data object Decimal                  : CalculatorEvent
    data object ToggleSign               : CalculatorEvent
    data object Percentage               : CalculatorEvent
}

fun CalculatorEvent.label(): String = when (this) {
    is CalculatorEvent.Digit       -> "digit($d)"
    is CalculatorEvent.Op          -> "operation(${op.symbol})"
    CalculatorEvent.Equals         -> "equals"
    CalculatorEvent.Clear          -> "clear"
    CalculatorEvent.Delete         -> "delete"
    CalculatorEvent.Decimal        -> "decimal"
    CalculatorEvent.ToggleSign     -> "toggleSign"
    CalculatorEvent.Percentage     -> "percentage"
}
