package dev.bloc.sample.examples.calculator

/**
 * The state managed by [CalculatorBloc].
 *
 * A single immutable value that captures the full state of the calculator at any
 * instant — what's on the display, the stored left-hand operand, the pending binary
 * operation, whether the next digit should replace the display, and whether there is
 * an active error condition.
 */
data class CalculatorState(
    val displayValue:     String     = "0",
    val storedValue:      Double?    = null,
    val pendingOperation: Operation? = null,
    val isNewEntry:       Boolean    = false,
    val hasError:         Boolean    = false,
) {
    val currentDouble: Double get() = displayValue.toDoubleOrNull() ?: 0.0

    companion object {
        val initial = CalculatorState()
    }
}
