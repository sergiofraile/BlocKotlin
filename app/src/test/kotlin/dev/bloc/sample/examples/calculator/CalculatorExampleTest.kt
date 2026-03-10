package dev.bloc.sample.examples.calculator

import dev.bloc.sample.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for [CalculatorBloc] — the lifecycle hooks showcase.
 *
 * CalculatorBloc demonstrates all four hooks (onEvent, onChange, onTransition, onError)
 * by logging them in real time. These tests verify both the arithmetic logic and the
 * error/lifecycle behaviour, mirroring the iOS CalculatorExampleTests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorExampleTest {

    @get:Rule val dispatcher = MainDispatcherRule()

    private lateinit var bloc: CalculatorBloc

    @Before fun setUp()    { bloc = CalculatorBloc() }
    @After  fun tearDown() { bloc.close() }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test fun `CalculatorBloc starts showing zero`() {
        assertEquals("0", bloc.state.displayValue)
        assertFalse(bloc.state.hasError)
    }

    // ── Digit entry ───────────────────────────────────────────────────────────

    @Test fun `Typing digits builds the display string`() {
        bloc.send(CalculatorEvent.Digit(4))
        bloc.send(CalculatorEvent.Digit(2))
        assertEquals("42", bloc.state.displayValue)
    }

    @Test fun `Leading zero is replaced when a non-zero digit is typed`() {
        bloc.send(CalculatorEvent.Digit(7))
        assertEquals("7", bloc.state.displayValue)
    }

    // ── Arithmetic ────────────────────────────────────────────────────────────

    @Test fun `Addition - 3 + 4 = 7`() {
        bloc.send(CalculatorEvent.Digit(3))
        bloc.send(CalculatorEvent.Op(Operation.Add))
        bloc.send(CalculatorEvent.Digit(4))
        bloc.send(CalculatorEvent.Equals)
        assertEquals("7", bloc.state.displayValue)
    }

    @Test fun `Subtraction - 10 minus 3 = 7`() {
        bloc.send(CalculatorEvent.Digit(1))
        bloc.send(CalculatorEvent.Digit(0))
        bloc.send(CalculatorEvent.Op(Operation.Subtract))
        bloc.send(CalculatorEvent.Digit(3))
        bloc.send(CalculatorEvent.Equals)
        assertEquals("7", bloc.state.displayValue)
    }

    @Test fun `Multiplication - 6 x 7 = 42`() {
        bloc.send(CalculatorEvent.Digit(6))
        bloc.send(CalculatorEvent.Op(Operation.Multiply))
        bloc.send(CalculatorEvent.Digit(7))
        bloc.send(CalculatorEvent.Equals)
        assertEquals("42", bloc.state.displayValue)
    }

    @Test fun `Division - 20 divided by 4 = 5`() {
        bloc.send(CalculatorEvent.Digit(2))
        bloc.send(CalculatorEvent.Digit(0))
        bloc.send(CalculatorEvent.Op(Operation.Divide))
        bloc.send(CalculatorEvent.Digit(4))
        bloc.send(CalculatorEvent.Equals)
        assertEquals("5", bloc.state.displayValue)
    }

    // ── Error handling ────────────────────────────────────────────────────────

    @Test fun `Division by zero shows Error and emits on errorsFlow`() = runTest(dispatcher.testDispatcher) {
        val errors = mutableListOf<Throwable>()
        val job = launch { bloc.errorsFlow.toList(errors) }

        bloc.send(CalculatorEvent.Digit(9))
        bloc.send(CalculatorEvent.Op(Operation.Divide))
        bloc.send(CalculatorEvent.Digit(0))
        bloc.send(CalculatorEvent.Equals)

        job.cancel()
        assertEquals("Error", bloc.state.displayValue)
        assertTrue(bloc.state.hasError)
        assertEquals(1, errors.size)
        assertTrue(errors.first() is ArithmeticException)
    }

    // ── Clear ─────────────────────────────────────────────────────────────────

    @Test fun `Clear resets the display to zero`() {
        bloc.send(CalculatorEvent.Digit(9))
        bloc.send(CalculatorEvent.Digit(9))
        bloc.send(CalculatorEvent.Clear)
        assertEquals("0", bloc.state.displayValue)
        assertFalse(bloc.state.hasError)
    }

    @Test fun `Clear after an error resets to zero`() {
        bloc.send(CalculatorEvent.Digit(5))
        bloc.send(CalculatorEvent.Op(Operation.Divide))
        bloc.send(CalculatorEvent.Digit(0))
        bloc.send(CalculatorEvent.Equals)
        assertTrue(bloc.state.hasError)

        bloc.send(CalculatorEvent.Clear)
        assertEquals("0", bloc.state.displayValue)
        assertFalse(bloc.state.hasError)
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Test fun `Delete removes the last digit`() {
        bloc.send(CalculatorEvent.Digit(4))
        bloc.send(CalculatorEvent.Digit(2))
        bloc.send(CalculatorEvent.Delete)
        assertEquals("4", bloc.state.displayValue)
    }

    @Test fun `Delete on a single digit returns zero`() {
        bloc.send(CalculatorEvent.Digit(7))
        bloc.send(CalculatorEvent.Delete)
        assertEquals("0", bloc.state.displayValue)
    }

    // ── Toggle sign ───────────────────────────────────────────────────────────

    @Test fun `ToggleSign negates then restores the displayed value`() {
        bloc.send(CalculatorEvent.Digit(5))
        bloc.send(CalculatorEvent.ToggleSign)
        assertEquals("-5", bloc.state.displayValue)
        bloc.send(CalculatorEvent.ToggleSign)
        assertEquals("5", bloc.state.displayValue)
    }

    @Test fun `ToggleSign on zero is a no-op`() {
        bloc.send(CalculatorEvent.ToggleSign)
        assertEquals("0", bloc.state.displayValue)
    }

    // ── Decimal ───────────────────────────────────────────────────────────────

    @Test fun `Decimal appends a dot to the current display`() {
        bloc.send(CalculatorEvent.Digit(3))
        bloc.send(CalculatorEvent.Decimal)
        bloc.send(CalculatorEvent.Digit(5))
        assertEquals("3.5", bloc.state.displayValue)
    }

    // ── Lifecycle log ─────────────────────────────────────────────────────────

    @Test fun `logFlow receives entries for each event and transition`() {
        bloc.send(CalculatorEvent.Digit(3))
        bloc.send(CalculatorEvent.Op(Operation.Add))
        bloc.send(CalculatorEvent.Digit(4))
        bloc.send(CalculatorEvent.Equals)
        assertTrue("Expected at least one log entry", bloc.logFlow.value.isNotEmpty())
    }

    @Test fun `clearLog empties the log without affecting state`() {
        bloc.send(CalculatorEvent.Digit(1))
        bloc.clearLog()
        assertTrue(bloc.logFlow.value.isEmpty())
        assertEquals("1", bloc.state.displayValue)
    }

    @Test fun `closedFlow becomes true when close is called`() {
        bloc.close()
        assertTrue(bloc.closedFlow.value)
    }
}
