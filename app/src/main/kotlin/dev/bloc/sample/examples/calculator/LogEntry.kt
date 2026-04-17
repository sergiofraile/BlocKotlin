package dev.bloc.sample.examples.calculator

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * A single entry in the [CalculatorBloc] lifecycle log.
 *
 * The log captures every [Kind] of lifecycle event — onEvent, onChange, onTransition,
 * onError, onClose — so the UI can display the Bloc's internals in real time.
 */
data class LogEntry(
    val id:        UUID   = UUID.randomUUID(),
    val timestamp: Long   = System.currentTimeMillis(),
    val kind:      Kind,
    val message:   String,
) {
    val timestampFormatted: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))

    enum class Kind(
        val label: String,
        val color: Color,
        val icon:  String,
    ) {
        EVENT(
            label = "EVENT",
            color = Color(0xFF4CAF50),
            icon  = "→",
        ),
        CHANGE(
            label = "CHANGE",
            color = Color(0xFF00BCD4),
            icon  = "⇄",
        ),
        TRANSITION(
            label = "TRANSITION",
            color = Color(0xFF9C27B0),
            icon  = "⇌",
        ),
        ERROR(
            label = "ERROR",
            color = Color(0xFFF44336),
            icon  = "⚠",
        ),
        CLOSE(
            label = "CLOSE",
            color = Color(0xFFFF9800),
            icon  = "✕",
        ),
    }
}
