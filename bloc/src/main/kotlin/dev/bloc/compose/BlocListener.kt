package dev.bloc.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import dev.bloc.BlocRegistry
import dev.bloc.StateEmitter
import kotlinx.coroutines.flow.drop
import kotlin.reflect.KClass

/**
 * A composable that reacts to [StateEmitter] state changes as **side effects**, without
 * causing [content] to rebuild.
 *
 * Use `BlocListener` whenever a state change should trigger a side effect —
 * showing a snackbar, playing a sound, navigating to another screen — but must **not**
 * cause the surrounding UI to recompose.
 *
 * ## Overview
 *
 * ```kotlin
 * BlocListener(
 *     bloc = scoreBloc,
 *     listenWhen = { _, new -> new > 0 && new % 5 == 0 },
 *     listener = { state -> showMilestoneBanner("🎯 ${state} points!") },
 * ) {
 *     ScoreView()
 * }
 * ```
 *
 * ## `listenWhen`
 *
 * Provide a `listenWhen` predicate to restrict the listener to specific transitions.
 * Both `previous` and `current` states are available:
 *
 * ```kotlin
 * BlocListener(
 *     bloc = authBloc,
 *     listenWhen = { prev, curr -> prev.isAuthenticated != curr.isAuthenticated },
 *     listener = { state ->
 *         if (state.isAuthenticated) navController.navigate("home")
 *         else navController.navigate("login")
 *     },
 * ) {
 *     LoginForm()
 * }
 * ```
 *
 * When `listenWhen` is omitted, [listener] is called on **every** state change.
 *
 * ## Side-effect only
 *
 * The [listener] closure runs on the main thread. It is safe to perform Compose state
 * mutations inside it (e.g. updating a `mutableStateOf` to show a dialog).
 *
 * [listener] is **not** called for the initial state — only for subsequent changes.
 *
 * ## Combining with BlocBuilder
 *
 * Nest a [BlocBuilder] inside [content] when you need both selective rebuilds and
 * side effects:
 *
 * ```kotlin
 * BlocListener(
 *     bloc = authBloc,
 *     listenWhen = { prev, curr -> prev.isAuthenticated != curr.isAuthenticated },
 *     listener = { state -> if (state.isAuthenticated) navController.navigate("home") },
 * ) {
 *     BlocBuilder(authBloc) { state -> LoginForm(state) }
 * }
 * ```
 *
 * @param bloc The [StateEmitter] to observe.
 * @param listenWhen Optional predicate `(previous, current) -> Boolean`. Called on every
 *   emission; the [listener] fires only when this returns `true`. The `previous` value
 *   is the last **emitted** state, regardless of whether the listener fired.
 * @param listener Side-effect callback invoked with the new state. Never invoked for
 *   the initial state.
 * @param content The composable subtree to render. Never recomposed in response to state
 *   changes from this listener.
 */
@Composable
fun <B : StateEmitter<S>, S : Any> BlocListener(
    bloc: B,
    listenWhen: ((previous: S, current: S) -> Boolean)? = null,
    listener: (state: S) -> Unit,
    content: @Composable () -> Unit = {},
) {
    LaunchedEffect(bloc) {
        var previous = bloc.state
        // drop(1) skips the StateFlow's initial emission so the listener is not
        // called for the state that existed when this composable entered composition.
        bloc.stateFlow.drop(1).collect { newState ->
            val shouldListen = listenWhen?.invoke(previous, newState) ?: true
            // Always advance the cursor, regardless of whether the listener fired.
            previous = newState
            if (shouldListen) listener(newState)
        }
    }

    content()
}

/**
 * Overload that resolves the bloc from [BlocRegistry] by [KClass].
 *
 * ```kotlin
 * BlocListener(
 *     blocClass = ScoreBloc::class,
 *     listenWhen = { _, new -> new.score % 10 == 0 },
 *     listener = { playChime() },
 * ) {
 *     ScoreContent()
 * }
 * ```
 */
@Composable
fun <B : StateEmitter<S>, S : Any> BlocListener(
    blocClass: KClass<B>,
    listenWhen: ((previous: S, current: S) -> Boolean)? = null,
    listener: (state: S) -> Unit,
    content: @Composable () -> Unit = {},
) {
    BlocListener(
        bloc = BlocRegistry.resolve(blocClass),
        listenWhen = listenWhen,
        listener = listener,
        content = content,
    )
}
