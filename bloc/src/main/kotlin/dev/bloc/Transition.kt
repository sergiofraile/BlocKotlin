package dev.bloc

/**
 * A [Change] that also carries the [event] that triggered it.
 *
 * Only produced when [Bloc.emit] is called **synchronously** inside an event handler
 * (i.e. without suspending first). Async emissions reach [Bloc.onChange] but not
 * [Bloc.onTransition].
 *
 * Forwarded to [BlocObserver.onTransition] and the Bloc's own [Bloc.onTransition] hook.
 */
data class Transition<E : Any, S : Any>(
    val currentState: S,
    val event: E,
    val nextState: S,
) {
    override fun toString(): String =
        "Transition { currentState: $currentState, event: $event, nextState: $nextState }"
}
