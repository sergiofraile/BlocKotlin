package dev.bloc.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.bloc.BlocRegistry
import dev.bloc.StateEmitter
import kotlinx.coroutines.flow.drop
import kotlin.reflect.KClass

/**
 * A composable that combines [BlocListener] and [BlocBuilder] with `buildWhen` into a
 * single, declarative component.
 *
 * Use `BlocConsumer` when a state transition needs **both** a side effect (navigation,
 * banner, sound) **and** a conditional UI rebuild — each gated by its own independent
 * predicate, driven by a single Flow subscription.
 *
 * ## When to use BlocConsumer vs BlocListener + BlocBuilder
 *
 * | Need | Prefer |
 * |---|---|
 * | Side effect only | [BlocListener] |
 * | UI rebuild only | [BlocBuilder] |
 * | Side effect + controlled rebuild | **`BlocConsumer`** |
 *
 * ## Usage
 *
 * ```kotlin
 * // Show a "tier up" animation AND update the tier badge — each gated by the same
 * // predicate, driven by a single subscription.
 * BlocConsumer(
 *     bloc       = scoreBloc,
 *     listenWhen = { old, new -> tierFor(old) != tierFor(new) },
 *     listener   = { _ -> triggerTierAnimation() },
 *     buildWhen  = { old, new -> tierFor(old) != tierFor(new) },
 * ) { state ->
 *     TierBadge(tier = tierFor(state))
 * }
 * ```
 *
 * Both predicates receive `(previous, current)` states and are evaluated on the same
 * emission. You can use **different** predicates for each:
 *
 * ```kotlin
 * // The side effect fires every 5 pts; the UI rebuilds every 10 pts.
 * BlocConsumer(
 *     bloc       = scoreBloc,
 *     listenWhen = { _, new -> new.score % 5 == 0 },
 *     listener   = { _ -> playChime() },
 *     buildWhen  = { old, new -> old.score / 10 != new.score / 10 },
 * ) { state ->
 *     TierBadge(tier = tierFor(state.score))
 * }
 * ```
 *
 * ## Content closure
 *
 * [content] receives a **state snapshot** (`S`), not the live bloc. This ensures
 * [buildWhen] fully controls when the composable recomposes. To dispatch events from
 * inside [content], resolve the bloc directly:
 *
 * ```kotlin
 * BlocConsumer(myBloc, ...) { state ->
 *     val bloc = remember { BlocRegistry.resolve(MyBloc::class) }
 *     Text("${state}")
 *     Button(onClick = { bloc.add(MyEvent.DoSomething) }) { Text("Go") }
 * }
 * ```
 *
 * ## Default behaviour
 *
 * Both [listenWhen] and [buildWhen] default to `null`, meaning they always trigger —
 * equivalent to [BlocListener] and [BlocBuilder] respectively.
 *
 * @param bloc The [StateEmitter] to observe.
 * @param listenWhen Optional predicate `(previous, current) -> Boolean`. The [listener]
 *   is called only when this returns `true`. `previous` is the last **emitted** state
 *   (independent of what was last rendered).
 * @param listener Side-effect callback invoked with the new state. Not called for the
 *   initial state.
 * @param buildWhen Optional predicate `(previous, current) -> Boolean`. The [content]
 *   snapshot is updated — triggering recomposition — only when this returns `true`.
 *   `previous` is the last **displayed** state (last state that was rendered).
 * @param content A composable receiving the last approved state snapshot.
 */
@Composable
fun <B : StateEmitter<S>, S : Any> BlocConsumer(
    bloc: B,
    listenWhen: ((previous: S, current: S) -> Boolean)? = null,
    listener: (state: S) -> Unit,
    buildWhen: ((previous: S, current: S) -> Boolean)? = null,
    content: @Composable (state: S) -> Unit,
) {
    var displayedState by remember(bloc) { mutableStateOf(bloc.state) }

    LaunchedEffect(bloc) {
        // Separate cursor for the listener — tracks last *emitted* state.
        var listenPrevious = bloc.state

        // drop(1) skips the StateFlow's initial emission; we only want changes.
        bloc.stateFlow.drop(1).collect { newState ->

            // ── Listener side ──────────────────────────────────────────────
            val shouldListen = listenWhen?.invoke(listenPrevious, newState) ?: true
            // Always advance the listener cursor so the predicate sees every transition.
            listenPrevious = newState
            if (shouldListen) listener(newState)

            // ── Builder side ───────────────────────────────────────────────
            // "previous" for buildWhen is the last *displayed* state, not the last
            // emitted — this matches the iOS BlocConsumer/BlocBuilderWhen behaviour.
            if (buildWhen?.invoke(displayedState, newState) ?: true) {
                displayedState = newState
            }
        }
    }

    content(displayedState)
}

/**
 * Overload that resolves the bloc from [BlocRegistry] by [KClass].
 *
 * ```kotlin
 * BlocConsumer(
 *     blocClass  = ScoreBloc::class,
 *     listenWhen = { old, new -> tierFor(old) != tierFor(new) },
 *     listener   = { _ -> triggerTierAnimation() },
 *     buildWhen  = { old, new -> old / 10 != new / 10 },
 * ) { state ->
 *     TierBadge(tier = tierFor(state))
 * }
 * ```
 */
@Composable
fun <B : StateEmitter<S>, S : Any> BlocConsumer(
    blocClass: KClass<B>,
    listenWhen: ((previous: S, current: S) -> Boolean)? = null,
    listener: (state: S) -> Unit,
    buildWhen: ((previous: S, current: S) -> Boolean)? = null,
    content: @Composable (state: S) -> Unit,
) {
    BlocConsumer(
        bloc = BlocRegistry.resolve(blocClass),
        listenWhen = listenWhen,
        listener = listener,
        buildWhen = buildWhen,
        content = content,
    )
}
