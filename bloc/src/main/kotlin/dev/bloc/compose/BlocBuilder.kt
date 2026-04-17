package dev.bloc.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.bloc.BlocRegistry
import dev.bloc.StateEmitter
import kotlinx.coroutines.flow.drop
import kotlin.reflect.KClass

// ---------------------------------------------------------------------------
// BlocBuilder — rebuilds on every state change
// ---------------------------------------------------------------------------

/**
 * A composable that subscribes to a [StateEmitter]'s state stream and passes the current
 * state to [content] on every emission.
 *
 * Use `BlocBuilder` as a concise way to bind bloc state to UI. To restrict rebuilds to
 * specific transitions, use the [buildWhen] overload instead.
 *
 * ## Usage
 *
 * ```kotlin
 * val bloc = remember { BlocRegistry.resolve(CounterBloc::class) }
 *
 * BlocBuilder(bloc) { state ->
 *     Text("Count: ${state.count}")
 *     Button(onClick = { bloc.add(CounterEvent.Increment) }) { Text("+") }
 * }
 * ```
 *
 * ## Resolving from the registry
 *
 * ```kotlin
 * BlocBuilder(CounterBloc::class) { state ->
 *     Text("Count: ${state.count}")
 * }
 * ```
 *
 * @param bloc The [StateEmitter] to observe.
 * @param content A composable receiving the latest state snapshot on every emission.
 */
@Composable
fun <B : StateEmitter<S>, S : Any> BlocBuilder(
    bloc: B,
    content: @Composable (state: S) -> Unit,
) {
    // collectAsState drives recomposition on every emission.
    val state by bloc.stateFlow.collectAsState()
    content(state)
}

/**
 * Overload that resolves the bloc from [BlocRegistry] by [KClass].
 *
 * ```kotlin
 * BlocBuilder(CounterBloc::class) { state -> ... }
 * ```
 */
@Composable
fun <B : StateEmitter<S>, S : Any> BlocBuilder(
    blocClass: KClass<B>,
    content: @Composable (state: S) -> Unit,
) {
    BlocBuilder(bloc = BlocRegistry.resolve(blocClass), content = content)
}

// ---------------------------------------------------------------------------
// BlocBuilder with buildWhen — filtered rebuilds
// ---------------------------------------------------------------------------

/**
 * A composable that rebuilds [content] only when [buildWhen] approves a state transition.
 *
 * Unlike the plain [BlocBuilder], this overload passes a **state snapshot** to [content]
 * and updates that snapshot only when `buildWhen(previous, current)` returns `true`.
 * This prevents UI sections from recomposing on every emission when only a specific field
 * or threshold matters.
 *
 * ## Usage
 *
 * ```kotlin
 * // Tier badge only redraws when the score crosses a tier boundary
 * BlocBuilder(
 *     bloc = scoreBloc,
 *     buildWhen = { old, new -> old / 10 != new / 10 },
 * ) { state ->
 *     TierBadge(tier = tierFor(state))
 * }
 * ```
 *
 * The "previous" value passed to [buildWhen] is the **last displayed** state — what was
 * last rendered — not the last emitted state. This mirrors the iOS `BlocBuilderWhen`
 * behaviour.
 *
 * @param bloc The [StateEmitter] to observe.
 * @param buildWhen Predicate `(previous, current) -> Boolean`. Return `true` to approve
 *   a rebuild. The previous value is the last state that was approved and rendered.
 * @param content A composable receiving the last approved state snapshot.
 */
@Composable
fun <B : StateEmitter<S>, S : Any> BlocBuilder(
    bloc: B,
    buildWhen: (previous: S, current: S) -> Boolean,
    content: @Composable (state: S) -> Unit,
) {
    var displayedState by remember(bloc) { mutableStateOf(bloc.state) }

    LaunchedEffect(bloc) {
        // drop(1) skips the StateFlow's initial emission; displayedState already holds it.
        bloc.stateFlow.drop(1).collect { newState ->
            if (buildWhen(displayedState, newState)) {
                displayedState = newState
            }
        }
    }

    content(displayedState)
}

/**
 * Overload that resolves the bloc from [BlocRegistry] by [KClass].
 */
@Composable
fun <B : StateEmitter<S>, S : Any> BlocBuilder(
    blocClass: KClass<B>,
    buildWhen: (previous: S, current: S) -> Boolean,
    content: @Composable (state: S) -> Unit,
) {
    BlocBuilder(
        bloc = BlocRegistry.resolve(blocClass),
        buildWhen = buildWhen,
        content = content,
    )
}

