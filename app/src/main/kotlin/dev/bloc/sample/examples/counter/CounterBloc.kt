package dev.bloc.sample.examples.counter

import dev.bloc.HydratedBloc
import kotlinx.serialization.serializer

/**
 * A simple counter that demonstrates [HydratedBloc] state persistence.
 *
 * The current count survives app restarts — it is automatically saved to
 * SharedPreferences on every increment/decrement and rehydrated when the
 * bloc is next instantiated. [Int] is serializable out of the box via
 * kotlinx.serialization, so no extra setup is needed.
 *
 * - To reset the counter and clear storage immediately, call [resetToInitialState].
 * - To wipe storage without affecting the running session, call [clearStoredState].
 */
class CounterBloc : HydratedBloc<Int, CounterEvent>(
    initialState = 0,
    serializer = serializer(),
    storageKeyParam = "counter",
) {
    init {
        on<CounterEvent.Increment> { _, emit -> emit(state + 1) }
        on<CounterEvent.Decrement> { _, emit -> emit(state - 1) }
        on<CounterEvent.Reset>     { _, emit -> emit(0) }
    }
}
