package dev.bloc.sample.examples.calculator

import dev.bloc.Bloc
import dev.bloc.Change
import dev.bloc.Transition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A Bloc that implements a four-function calculator.
 *
 * This example is designed to showcase the four lifecycle hooks available alongside
 * [Change] and [Transition]:
 *
 * - [onEvent] — fires before every event is processed
 * - [onTransition] — fires when state changes synchronously within a handler
 * - [onChange] — fires on every state emission
 * - [onError] — fires when [addError] is called (e.g. divide by zero)
 *
 * All four hooks append entries to [logFlow], which the [CalculatorScreen] displays in
 * real time so you can watch the Bloc's internals as you tap buttons.
 */
class CalculatorBloc : Bloc<CalculatorState, CalculatorEvent>(CalculatorState.initial) {

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle log
    // ─────────────────────────────────────────────────────────────────────────

    private val _logFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logFlow: StateFlow<List<LogEntry>> = _logFlow.asStateFlow()

    private val _closedFlow = MutableStateFlow(false)
    val closedFlow: StateFlow<Boolean> = _closedFlow.asStateFlow()

    private fun appendLog(kind: LogEntry.Kind, message: String) {
        val updated = _logFlow.value + LogEntry(kind = kind, message = message)
        _logFlow.value = if (updated.size > 200) updated.drop(1) else updated
    }

    fun clearLog() { _logFlow.value = emptyList() }

    // ─────────────────────────────────────────────────────────────────────────
    // Event handlers
    // ─────────────────────────────────────────────────────────────────────────

    init {
        on<CalculatorEvent.Digit>      { ev, emit -> handleDigit(ev.d, emit) }
        on<CalculatorEvent.Op>         { ev, emit -> handleOperation(ev.op, emit) }
        on<CalculatorEvent.Equals>     { _, emit  -> handleEquals(emit) }
        on<CalculatorEvent.Clear>      { _, emit  -> emit(CalculatorState.initial) }
        on<CalculatorEvent.Delete>     { _, emit  -> handleDelete(emit) }
        on<CalculatorEvent.Decimal>    { _, emit  -> handleDecimal(emit) }
        on<CalculatorEvent.ToggleSign> { _, emit  -> handleToggleSign(emit) }
        on<CalculatorEvent.Percentage> { _, emit  -> handlePercentage(emit) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle hooks — demonstrating all four
    // ─────────────────────────────────────────────────────────────────────────

    override fun onEvent(event: CalculatorEvent) {
        super.onEvent(event)
        appendLog(LogEntry.Kind.EVENT, event.label())
    }

    override fun onChange(change: Change<CalculatorState>) {
        super.onChange(change)
        appendLog(
            LogEntry.Kind.CHANGE,
            "${change.currentState.displayValue} → ${change.nextState.displayValue}",
        )
    }

    override fun onTransition(transition: Transition<CalculatorEvent, CalculatorState>) {
        super.onTransition(transition)
        appendLog(
            LogEntry.Kind.TRANSITION,
            "${transition.currentState.displayValue} — ${transition.event.label()} → ${transition.nextState.displayValue}",
        )
    }

    override fun onError(error: Throwable) {
        super.onError(error)
        appendLog(LogEntry.Kind.ERROR, error.message ?: "$error")
    }

    override fun onClose() {
        super.onClose()
        _closedFlow.value = true
        appendLog(LogEntry.Kind.CLOSE, "Bloc closed — send() and emit() are now no-ops")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private handlers
    // ─────────────────────────────────────────────────────────────────────────

    private fun handleDigit(digit: Int, emit: (CalculatorState) -> Unit) {
        var s = state
        if (s.isNewEntry || s.displayValue == "0") {
            s = s.copy(displayValue = if (digit == 0) "0" else "$digit", isNewEntry = false)
        } else {
            val digits = s.displayValue.replace("-", "")
            if (digits.length >= 9) return
            s = s.copy(displayValue = s.displayValue + digit)
        }
        emit(s.copy(hasError = false))
    }

    private fun handleOperation(op: Operation, emit: (CalculatorState) -> Unit) {
        var s = state
        // Chain: if there's a pending op and the user hasn't started a new operand yet,
        // evaluate the existing expression before storing the new operation.
        if (s.pendingOperation != null && !s.isNewEntry && s.storedValue != null) {
            s = evaluate(s, s.storedValue!!, s.currentDouble, s.pendingOperation!!)
            if (s.hasError) { emit(s); return }
        }
        emit(s.copy(storedValue = s.currentDouble, pendingOperation = op, isNewEntry = true))
    }

    private fun handleEquals(emit: (CalculatorState) -> Unit) {
        val op     = state.pendingOperation ?: return
        val stored = state.storedValue      ?: return
        var s = evaluate(state, stored, state.currentDouble, op)
        emit(s.copy(pendingOperation = null, storedValue = null, isNewEntry = true))
    }

    private fun handleDelete(emit: (CalculatorState) -> Unit) {
        var s = state
        if (s.hasError) { emit(CalculatorState.initial); return }
        val next = if (s.displayValue.length > 1) s.displayValue.dropLast(1) else "0"
        emit(s.copy(displayValue = if (next == "-") "0" else next))
    }

    private fun handleDecimal(emit: (CalculatorState) -> Unit) {
        var s = state
        if (s.isNewEntry) {
            emit(s.copy(displayValue = "0.", isNewEntry = false))
        } else if (!s.displayValue.contains('.')) {
            emit(s.copy(displayValue = s.displayValue + "."))
        }
    }

    private fun handleToggleSign(emit: (CalculatorState) -> Unit) {
        val s = state
        if (s.displayValue == "0" || s.hasError) return
        val next = if (s.displayValue.startsWith("-"))
            s.displayValue.drop(1) else "-${s.displayValue}"
        emit(s.copy(displayValue = next))
    }

    private fun handlePercentage(emit: (CalculatorState) -> Unit) {
        val value = state.displayValue.toDoubleOrNull() ?: return
        emit(state.copy(displayValue = formatResult(value / 100.0)))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Evaluation
    // ─────────────────────────────────────────────────────────────────────────

    private fun evaluate(
        s:       CalculatorState,
        stored:  Double,
        current: Double,
        op:      Operation,
    ): CalculatorState = when (op) {
        Operation.Add      -> s.copy(displayValue = formatResult(stored + current))
        Operation.Subtract -> s.copy(displayValue = formatResult(stored - current))
        Operation.Multiply -> s.copy(displayValue = formatResult(stored * current))
        Operation.Divide   -> {
            if (current == 0.0) {
                addError(ArithmeticException("Division by zero"))
                s.copy(
                    displayValue     = "Error",
                    hasError         = true,
                    pendingOperation = null,
                    storedValue      = null,
                    isNewEntry       = true,
                )
            } else {
                s.copy(displayValue = formatResult(stored / current))
            }
        }
    }

    private fun formatResult(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Error"
        return if (value % 1.0 == 0.0 && kotlin.math.abs(value) < 1e10)
            value.toLong().toString()
        else
            "%.9g".format(value)
    }
}
