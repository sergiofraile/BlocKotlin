package dev.bloc.sample.examples.stopwatch

import dev.bloc.Cubit
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A stopwatch [Cubit] that manages elapsed time via **direct method calls** — no events.
 *
 * This is the canonical Cubit example. Instead of dispatching events through a handler
 * registry (as a Bloc would), callers invoke methods directly:
 *
 * ```kotlin
 * cubit.start()
 * cubit.pause()
 * cubit.reset()
 * ```
 *
 * The Cubit owns its async tick loop — no events, no transformers needed.
 *
 * ## Cubit vs Bloc for this use-case
 * A Bloc would require a `StopwatchEvent` sealed class (.start, .pause, .reset) and the
 * same internal coroutine management written inside event handlers. The Cubit version is
 * shorter, more readable, and equally testable — ideal for simple, method-driven state.
 */
class StopwatchCubit : Cubit<StopwatchState>(StopwatchState.initial) {

    private var tickJob: Job? = null

    /** Starts the stopwatch. No-op if already running. */
    fun start() {
        if (state.isRunning) return
        emit(state.copy(isRunning = true))
        tickJob = scope.launch {
            while (true) {
                delay(10L)
                if (!state.isRunning) break
                emit(state.copy(elapsed = state.elapsed + 0.01))
            }
        }
    }

    /** Pauses the stopwatch, preserving elapsed time. */
    fun pause() {
        if (!state.isRunning) return
        tickJob?.cancel()
        tickJob = null
        emit(state.copy(isRunning = false))
    }

    /** Resets the stopwatch to zero and stops it. */
    fun reset() {
        tickJob?.cancel()
        tickJob = null
        emit(StopwatchState.initial)
    }

    override fun onClose() {
        super.onClose()
        tickJob?.cancel()
    }
}
