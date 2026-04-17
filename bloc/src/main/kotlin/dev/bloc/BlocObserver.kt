package dev.bloc

/**
 * Global observer that receives lifecycle notifications from **every** [Bloc] and [Cubit]
 * in the app.
 *
 * Set a custom subclass once at app startup, before any Blocs are created:
 * ```kotlin
 * class App : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         BlocObserver.shared = AppBlocObserver()
 *     }
 * }
 * ```
 *
 * ### Hook summary
 * | Hook | When it fires | Emitter type |
 * |---|---|---|
 * | [onCreate] | Emitter is initialised | Bloc + Cubit |
 * | [onEvent] | Before an event is dispatched | Bloc only |
 * | [onChange] | After every `emit()` | Bloc + Cubit |
 * | [onTransition] | After a **synchronous** `emit()` with event context | Bloc only |
 * | [onError] | When `addError()` is called | Bloc + Cubit |
 * | [onClose] | When `close()` is called | Bloc + Cubit |
 *
 * Always call `super` in every override so the chain remains intact as the library grows.
 */
open class BlocObserver {

    /** Called immediately after a [Cubit] or [Bloc] is constructed. */
    open fun <S : Any> onCreate(emitter: StateEmitter<S>) {}

    /**
     * Called immediately before an event is dispatched to its handler.
     * Only fires for [Bloc] subclasses, not [Cubit].
     */
    open fun <S : Any, E : Any> onEvent(bloc: Bloc<S, E>, event: E) {}

    /**
     * Called after every [Cubit.emit] / [Bloc.emit], with a [Change] containing
     * the previous and next states.
     */
    open fun <S : Any> onChange(emitter: StateEmitter<S>, change: Change<S>) {}

    /**
     * Called after a **synchronous** [Bloc.emit] that was triggered by an event handler.
     * Async emissions (those inside a launched coroutine) reach [onChange] but not here.
     */
    open fun <S : Any, E : Any> onTransition(bloc: Bloc<S, E>, transition: Transition<E, S>) {}

    /** Called whenever [Cubit.addError] / [Bloc.addError] is invoked. */
    open fun <S : Any> onError(emitter: StateEmitter<S>, error: Throwable) {}

    /** Called when [Cubit.close] / [Bloc.close] completes. */
    open fun <S : Any> onClose(emitter: StateEmitter<S>) {}

    companion object {
        /** The active global observer. Replace at app startup before creating any Blocs. */
        var shared: BlocObserver = BlocObserver()
    }
}
