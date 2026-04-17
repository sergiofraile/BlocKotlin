package dev.bloc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A lightweight state-management class that exposes **direct method calls** instead of events.
 *
 * Use [Cubit] when you don't need an event audit trail or [EventTransformer] strategies.
 * It is shorter, easier to read, and equally testable.
 *
 * ```kotlin
 * class CounterCubit : Cubit<Int>(initialState = 0) {
 *     fun increment() = emit(state + 1)
 *     fun decrement() = emit(state - 1)
 *     fun reset()     = emit(0)
 * }
 * ```
 *
 * ### Cubit vs Bloc
 * | | Cubit | Bloc |
 * |---|---|---|
 * | **API style** | Direct method calls | Dispatched events |
 * | **Audit trail** | No explicit event log | Full event history via [Bloc.eventsFlow] |
 * | **Transformers** | Not applicable | Debounce, Restartable, Droppable, … |
 * | **Observer hooks** | onCreate, onChange, onError, onClose | All of the above + onEvent, onTransition |
 * | **Best for** | Simple, well-understood state logic | Complex flows, analytics, rate-limiting |
 *
 * > **Rule of thumb:** Start with a [Cubit]. Upgrade to a [Bloc] when you need an event log
 * > or an [EventTransformer] strategy.
 *
 * All methods are **main-thread safe** — the internal [CoroutineScope] runs on
 * [Dispatchers.Main]. Override [scope] to change the dispatcher (e.g. for tests).
 */
abstract class Cubit<S : Any>(initialState: S) : StateEmitter<S> {

    /** Coroutine scope tied to this Cubit's lifetime. Cancelled when [close] is called. */
    protected open val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _stateFlow = MutableStateFlow(initialState)
    override val stateFlow: StateFlow<S> = _stateFlow.asStateFlow()

    /** The current state snapshot. Updated synchronously on the main thread. */
    override val state: S get() = _stateFlow.value

    private val _errorsFlow = MutableSharedFlow<Throwable>(extraBufferCapacity = 64)
    override val errorsFlow: SharedFlow<Throwable> = _errorsFlow.asSharedFlow()

    private var _isClosed = false
    override val isClosed: Boolean get() = _isClosed

    init {
        @Suppress("LeakingThis")
        BlocObserver.shared.onCreate(this)
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Emits a new [state].
     *
     * If [nextState] equals the current state the call is a no-op (deduplication).
     * After emitting, [onChange] and [BlocObserver.onChange] are called.
     *
     * Must be called from the main thread (or the scope's dispatcher).
     */
    protected fun emit(nextState: S) {
        if (isClosed) return
        if (nextState == _stateFlow.value) return
        val change = Change(currentState = _stateFlow.value, nextState = nextState)
        _stateFlow.value = nextState
        onChange(change)
    }

    /**
     * Signals an error without encoding it into the state type.
     * Broadcasts to [errorsFlow], then calls [onError] and [BlocObserver.onError].
     */
    protected fun addError(error: Throwable) {
        onError(error)
        _errorsFlow.tryEmit(error)
    }

    /**
     * Closes this Cubit, cancels the coroutine scope, and marks it as closed.
     * After calling [close], [emit] and [addError] become no-ops.
     */
    override fun close() {
        if (_isClosed) return
        _isClosed = true
        onClose()
        scope.cancel()
    }

    // -------------------------------------------------------------------------
    // Lifecycle hooks — override in subclasses; always call super
    // -------------------------------------------------------------------------

    /**
     * Called after every [emit]. Contains the [Change] with the previous and next states.
     *
     * Always call `super.onChange(change)` to forward to [BlocObserver.onChange].
     */
    open fun onChange(change: Change<S>) {
        BlocObserver.shared.onChange(this, change)
    }

    /**
     * Called whenever [addError] is invoked.
     *
     * Always call `super.onError(error)` to forward to [BlocObserver.onError].
     */
    open fun onError(error: Throwable) {
        BlocObserver.shared.onError(this, error)
    }

    /**
     * Called when [close] completes. Use this to cancel any background coroutines
     * or flush pending resources.
     *
     * Always call `super.onClose()` to forward to [BlocObserver.onClose].
     */
    open fun onClose() {
        BlocObserver.shared.onClose(this)
    }
}
