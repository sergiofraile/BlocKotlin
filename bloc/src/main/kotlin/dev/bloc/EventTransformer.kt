package dev.bloc

import kotlin.time.Duration

/**
 * Controls how a new event of the same type is handled while a previous handler is
 * still active or pending.
 *
 * Attach a transformer when registering a handler in a [Bloc]:
 * ```kotlin
 * on<SearchEvent.Query>(transformer = EventTransformer.Debounce(300.milliseconds)) { event, emit ->
 *     performSearch(event.query, emit)
 * }
 * ```
 *
 * | Transformer | Behaviour | Best for |
 * |---|---|---|
 * | [Sequential] | Default. Processes events one at a time in order. | Counters, toggles |
 * | [Concurrent] | Each event gets its own coroutine, all run in parallel. | Independent fire-and-forget ops |
 * | [Droppable] | Ignores new events while the previous handler coroutine is active. | Expensive one-shot ops |
 * | [Restartable] | Cancels the running handler and starts fresh with the new event. | "Load latest" patterns |
 * | [Debounce] | Waits for a quiet period; each new event resets the timer. | Live search |
 * | [Throttle] | Fires immediately, then ignores events for the duration. | Scroll-triggered pagination |
 */
sealed class EventTransformer {

    /** Process events one at a time in arrival order. This is the default. */
    data object Sequential : EventTransformer()

    /** Run each event handler in its own coroutine concurrently. */
    data object Concurrent : EventTransformer()

    /**
     * Silently drop incoming events while a previous handler coroutine is still running.
     * The first event fires immediately; duplicates are discarded until the job completes.
     */
    data object Droppable : EventTransformer()

    /**
     * Cancel the currently running handler coroutine and start a fresh one for the new event.
     * Ideal for "fetch latest" patterns where only the most recent request matters.
     */
    data object Restartable : EventTransformer()

    /**
     * Wait for [duration] of silence before invoking the handler.
     * Each new event resets the countdown. The handler is called only once after the
     * quiet period expires.
     *
     * @param duration The silence window (e.g. `300.milliseconds`).
     */
    data class Debounce(val duration: Duration) : EventTransformer()

    /**
     * Invoke the handler immediately for the first event, then suppress further events
     * for [duration].
     *
     * @param duration The suppression window after the first firing (e.g. `1.seconds`).
     */
    data class Throttle(val duration: Duration) : EventTransformer()
}
