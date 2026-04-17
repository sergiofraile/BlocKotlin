package dev.bloc

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Base interface implemented by both [Bloc] and [Cubit].
 *
 * Pure Kotlin / KMP-ready — no Android framework imports.
 */
interface StateEmitter<S : Any> {
    /** The current state snapshot. */
    val state: S

    /** Hot [StateFlow] that replays the latest state to new collectors. */
    val stateFlow: StateFlow<S>

    /** Hot [SharedFlow] of errors surfaced via `addError()`. */
    val errorsFlow: SharedFlow<Throwable>

    /** `true` after [close] has been called. Further emissions become no-ops. */
    val isClosed: Boolean

    /**
     * Closes the emitter: cancels all coroutines and marks it as closed.
     * Idempotent — safe to call multiple times.
     */
    fun close()
}
