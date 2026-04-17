package dev.bloc.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import dev.bloc.BlocRegistry
import dev.bloc.StateEmitter

/**
 * A composable that registers [StateEmitter] instances (Bloc or Cubit) with [BlocRegistry]
 * and makes them available to any descendant composable.
 *
 * `BlocProvider` is the entry point for the Bloc pattern in Compose. It handles the full
 * lifecycle of the provided blocs: they are registered when the composable enters
 * composition and closed + deregistered when it leaves.
 *
 * ## Overview
 *
 * Wrap your root composable (or any subtree) with `BlocProvider`:
 *
 * ```kotlin
 * setContent {
 *     BlocProvider(listOf(
 *         CounterBloc(),
 *         AuthBloc(authService = LiveAuthService()),
 *     )) {
 *         AppNavigation()
 *     }
 * }
 * ```
 *
 * ## Dependency injection
 *
 * `BlocProvider` is the ideal place to inject dependencies into your blocs:
 *
 * ```kotlin
 * BlocProvider(listOf(
 *     UserBloc(userService = LiveUserService()),
 *     AnalyticsBloc(tracker = FirebaseTracker()),
 * )) {
 *     MainScreen()
 * }
 * ```
 *
 * For Compose previews and tests, swap in mock implementations:
 *
 * ```kotlin
 * @Preview
 * @Composable
 * fun CounterPreview() {
 *     BlocProvider(listOf(CounterBloc())) {
 *         CounterScreen()
 *     }
 * }
 * ```
 *
 * ## Accessing blocs
 *
 * Resolve blocs anywhere in the descendant tree via [BlocRegistry.resolve]:
 *
 * ```kotlin
 * @Composable
 * fun CounterScreen() {
 *     val bloc = remember { BlocRegistry.resolve(CounterBloc::class) }
 *     val state by bloc.stateFlow.collectAsState()
 *     Text("Count: ${state.count}")
 * }
 * ```
 *
 * @param blocs The list of [StateEmitter] instances to register.
 * @param content The composable subtree that can resolve registered blocs.
 */
@Composable
fun BlocProvider(
    blocs: List<StateEmitter<*>>,
    content: @Composable () -> Unit,
) {
    DisposableEffect(Unit) {
        BlocRegistry.register(blocs)
        onDispose {
            BlocRegistry.closeAll()
        }
    }
    content()
}
