package dev.bloc

/**
 * Represents a state transition: the state before and after a single [emit] call.
 *
 * Produced by every [Cubit.emit] and [Bloc.emit] invocation and forwarded to
 * [BlocObserver.onChange] and the emitter's own [Cubit.onChange] / [Bloc.onChange] hook.
 */
data class Change<S : Any>(
    val currentState: S,
    val nextState: S,
) {
    override fun toString(): String =
        "Change { currentState: $currentState, nextState: $nextState }"
}
