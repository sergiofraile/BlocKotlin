package dev.bloc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Business Logic Component — the core state-management class.
 *
 * A [Bloc] receives typed **events**, routes each one to a registered handler via [on],
 * and emits new states through [emit]. Every state change is observable via [stateFlow].
 *
 * ```kotlin
 * // 1. Define events
 * sealed class CounterEvent {
 *     data object Increment : CounterEvent()
 *     data object Decrement : CounterEvent()
 * }
 *
 * // 2. Create the Bloc
 * class CounterBloc : Bloc<Int, CounterEvent>(initialState = 0) {
 *     init {
 *         on<CounterEvent.Increment> { _, emit -> emit(state + 1) }
 *         on<CounterEvent.Decrement> { _, emit -> emit(state - 1) }
 *     }
 * }
 *
 * // 3. Send events
 * counterBloc.send(CounterEvent.Increment)
 * ```
 *
 * ### Handler signature
 * ```kotlin
 * typealias Handler<E, S> = suspend (event: E, emit: (S) -> Unit) -> Unit
 * ```
 *
 * @param S State type — must implement [equals] (data class or primitive recommended).
 * @param E Event type — sealed class / interface recommended.
 */
abstract class Bloc<S : Any, E : Any>(initialState: S) : StateEmitter<S> {

    /** Coroutine scope tied to this Bloc's lifetime. Cancelled when [close] is called. */
    protected open val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _stateFlow = MutableStateFlow(initialState)
    override val stateFlow: StateFlow<S> = _stateFlow.asStateFlow()

    /** The current state snapshot. */
    override val state: S get() = _stateFlow.value

    private val _errorsFlow = MutableSharedFlow<Throwable>(extraBufferCapacity = 64)
    override val errorsFlow: SharedFlow<Throwable> = _errorsFlow.asSharedFlow()

    private val _eventsFlow = MutableSharedFlow<E>(extraBufferCapacity = 64)

    /** Hot stream of every dispatched event, in arrival order. */
    val eventsFlow: SharedFlow<E> = _eventsFlow.asSharedFlow()

    private var _isClosed = false
    override val isClosed: Boolean get() = _isClosed

    // Tracks the event currently being processed (for onTransition).
    private var currentEvent: E? = null

    // Per-handler-key active jobs (used by Droppable / Restartable / Debounce / Throttle).
    private val activeJobs = mutableMapOf<String, Job>()
    // Throttle guard timestamps.
    private val throttleTimestamps = mutableMapOf<String, Long>()

    // Registered handlers: predicate → (transformer, suspend handler)
    private data class HandlerEntry<E : Any, S : Any>(
        val predicate: (E) -> Boolean,
        val transformer: EventTransformer,
        val handler: suspend (E, (S) -> Unit) -> Unit,
    )

    private val handlers = mutableListOf<HandlerEntry<E, S>>()

    init {
        @Suppress("LeakingThis")
        BlocObserver.shared.onCreate(this)
    }

    // -------------------------------------------------------------------------
    // Handler registration
    // -------------------------------------------------------------------------

    /**
     * Registers a handler for a specific event **type** [T].
     *
     * All events whose runtime type is exactly [T] will be routed to [handler].
     *
     * ```kotlin
     * on<CounterEvent.Increment> { event, emit ->
     *     emit(state + 1)
     * }
     * ```
     */
    protected inline fun <reified T : E> on(
        transformer: EventTransformer = EventTransformer.Sequential,
        noinline handler: suspend (T, (S) -> Unit) -> Unit,
    ) {
        on(
            predicate = { it is T },
            transformer = transformer,
            handler = { event, emit -> handler(event as T, emit) },
        )
    }

    /**
     * Registers a handler for events matching an arbitrary [predicate].
     *
     * Useful when you need to match events with associated values regardless of
     * their specific payload:
     * ```kotlin
     * on(
     *     predicate = { it is SearchEvent.Query },
     *     transformer = EventTransformer.Debounce(300.milliseconds),
     * ) { event, emit ->
     *     performSearch((event as SearchEvent.Query).query, emit)
     * }
     * ```
     */
    protected fun on(
        predicate: (E) -> Boolean,
        transformer: EventTransformer = EventTransformer.Sequential,
        handler: suspend (E, (S) -> Unit) -> Unit,
    ) {
        handlers.add(HandlerEntry(predicate, transformer, handler))
    }

    // -------------------------------------------------------------------------
    // Event dispatch
    // -------------------------------------------------------------------------

    /**
     * Dispatches [event] to the first registered handler whose predicate matches.
     *
     * Calls [onEvent] and [BlocObserver.onEvent] before routing.
     */
    fun send(event: E) {
        if (isClosed) return
        onEvent(event)
        _eventsFlow.tryEmit(event)

        val entry = handlers.firstOrNull { it.predicate(event) } ?: return
        val key = entry.predicate.toString()

        dispatch(event, entry.transformer, key, entry.handler)
    }

    private fun dispatch(
        event: E,
        transformer: EventTransformer,
        key: String,
        handler: suspend (E, (S) -> Unit) -> Unit,
    ) {
        when (transformer) {
            EventTransformer.Sequential -> {
                currentEvent = event
                scope.launch {
                    handler(event, ::emit)
                }.invokeOnCompletion { currentEvent = null }
                // For true sequential (one-at-a-time), we use a simple launch.
                // Handlers that emit synchronously will fire onTransition correctly.
            }

            EventTransformer.Concurrent -> {
                scope.launch { handler(event, ::emit) }
            }

            EventTransformer.Droppable -> {
                val existing = activeJobs[key]
                if (existing != null && existing.isActive) return
                activeJobs[key] = scope.launch {
                    handler(event, ::emit)
                }.also { it.invokeOnCompletion { activeJobs.remove(key) } }
            }

            EventTransformer.Restartable -> {
                activeJobs[key]?.cancel()
                activeJobs[key] = scope.launch {
                    handler(event, ::emit)
                }.also { it.invokeOnCompletion { activeJobs.remove(key) } }
            }

            is EventTransformer.Debounce -> {
                activeJobs[key]?.cancel()
                activeJobs[key] = scope.launch {
                    delay(transformer.duration)
                    handler(event, ::emit)
                    activeJobs.remove(key)
                }
            }

            is EventTransformer.Throttle -> {
                val now = System.currentTimeMillis()
                val last = throttleTimestamps[key] ?: 0L
                if (now - last < transformer.duration.inWholeMilliseconds) return
                throttleTimestamps[key] = now
                scope.launch { handler(event, ::emit) }
            }
        }
    }

    // -------------------------------------------------------------------------
    // State emission
    // -------------------------------------------------------------------------

    /**
     * Emits a new [nextState].
     *
     * If [nextState] equals the current state the call is a no-op.
     * Calls [onTransition] (when a current event is set), then [onChange],
     * forwarding both to [BlocObserver].
     *
     * Marked `open` so that [HydratedBloc] can intercept emissions for persistence.
     */
    open fun emit(nextState: S) {
        if (isClosed) return
        if (nextState == _stateFlow.value) return
        val change = Change(currentState = _stateFlow.value, nextState = nextState)
        currentEvent?.let { event ->
            val transition = Transition(currentState = _stateFlow.value, event = event, nextState = nextState)
            onTransition(transition)
        }
        _stateFlow.value = nextState
        onChange(change)
    }

    /**
     * Signals an error without encoding it into the state type.
     * Broadcasts to [errorsFlow], then calls [onError] and [BlocObserver.onError].
     */
    fun addError(error: Throwable) {
        onError(error)
        _errorsFlow.tryEmit(error)
    }

    /**
     * Closes this Bloc: cancels the scope, cancels all active jobs, and marks it closed.
     * After calling [close], [send] and [emit] become no-ops.
     */
    override fun close() {
        if (_isClosed) return
        _isClosed = true
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        onClose()
        scope.cancel()
    }

    // -------------------------------------------------------------------------
    // Lifecycle hooks — override in subclasses; always call super
    // -------------------------------------------------------------------------

    /**
     * Called immediately before an event is dispatched to its handler.
     * Always call `super.onEvent(event)` to forward to [BlocObserver.onEvent].
     */
    open fun onEvent(event: E) {
        BlocObserver.shared.onEvent(this, event)
    }

    /**
     * Called after every [emit].
     * Always call `super.onChange(change)` to forward to [BlocObserver.onChange].
     */
    open fun onChange(change: Change<S>) {
        BlocObserver.shared.onChange(this, change)
    }

    /**
     * Called after a synchronous [emit] that has an active event context.
     * Always call `super.onTransition(transition)` to forward to [BlocObserver.onTransition].
     */
    open fun onTransition(transition: Transition<E, S>) {
        BlocObserver.shared.onTransition(this, transition)
    }

    /**
     * Called whenever [addError] is invoked.
     * Always call `super.onError(error)` to forward to [BlocObserver.onError].
     */
    open fun onError(error: Throwable) {
        BlocObserver.shared.onError(this, error)
    }

    /**
     * Called when [close] completes. Cancel any background coroutines here.
     * Always call `super.onClose()` to forward to [BlocObserver.onClose].
     */
    open fun onClose() {
        BlocObserver.shared.onClose(this)
    }
}
