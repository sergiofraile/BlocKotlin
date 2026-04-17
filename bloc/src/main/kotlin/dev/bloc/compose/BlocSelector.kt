package dev.bloc.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.bloc.BlocRegistry
import dev.bloc.StateEmitter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass

/**
 * A composable that derives a value from a [StateEmitter]'s state and rebuilds [content]
 * **only** when that derived value changes.
 *
 * `BlocSelector` is the most targeted rebuild primitive in the Bloc library. Where
 * [BlocBuilder] rebuilds on every emission and [BlocBuilder] with `buildWhen` rebuilds
 * on approved transitions, `BlocSelector` projects the state down to a single [V] via
 * a `selector` closure and uses `equals` equality to suppress redundant rebuilds.
 *
 * ## Overview
 *
 * ```kotlin
 * // Only recomposes when isLoading flips — card list updates are ignored
 * BlocSelector(
 *     bloc = lorcanaBloc,
 *     selector = { it.isLoading },
 * ) { isLoading ->
 *     if (isLoading) CircularProgressIndicator()
 * }
 * ```
 *
 * ## Composing multiple selectors
 *
 * Derive multiple fields at once with an `Equatable` data class:
 *
 * ```kotlin
 * data class PaginationStatus(
 *     val isLoadingMore: Boolean,
 *     val hasMorePages: Boolean,
 *     val cardCount: Int,
 * )
 *
 * BlocSelector(
 *     bloc = lorcanaBloc,
 *     selector = { state ->
 *         PaginationStatus(
 *             isLoadingMore = state.isLoadingMore,
 *             hasMorePages  = state.hasMorePages,
 *             cardCount     = state.cards.size,
 *         )
 *     },
 * ) { status ->
 *     PaginationFooter(status)
 * }
 * ```
 *
 * ## When to use `BlocSelector`
 *
 * | Need | Use |
 * |---|---|
 * | Full state, rebuild on every emission | [BlocBuilder] |
 * | Rebuild at discrete thresholds | [BlocBuilder] with `buildWhen` |
 * | Rebuild only when a derived value changes | **`BlocSelector`** |
 *
 * @param bloc The [StateEmitter] to observe.
 * @param selector A function that projects the full state down to a single [V].
 *   Called on every emission; [content] recomposes only when the result changes
 *   under `==`. Ensure [V] has a correct `equals` implementation.
 * @param content A composable receiving the derived value. The closure receives [V],
 *   not the full state, preventing accidental subscriptions to other fields.
 */
@Composable
fun <B : StateEmitter<S>, S : Any, V : Any> BlocSelector(
    bloc: B,
    selector: (state: S) -> V,
    content: @Composable (value: V) -> Unit,
) {
    // Build a derived flow that only emits when the selector's result changes.
    val derivedFlow = remember(bloc, selector) {
        bloc.stateFlow.map(selector).distinctUntilChanged()
    }
    val selectedValue by derivedFlow.collectAsState(initial = selector(bloc.state))
    content(selectedValue)
}

/**
 * Overload that resolves the bloc from [BlocRegistry] by [KClass].
 *
 * ```kotlin
 * BlocSelector(
 *     blocClass = LorcanaBloc::class,
 *     selector  = { it.isLoading },
 * ) { isLoading ->
 *     if (isLoading) CircularProgressIndicator()
 * }
 * ```
 */
@Composable
fun <B : StateEmitter<S>, S : Any, V : Any> BlocSelector(
    blocClass: KClass<B>,
    selector: (state: S) -> V,
    content: @Composable (value: V) -> Unit,
) {
    BlocSelector(
        bloc = BlocRegistry.resolve(blocClass),
        selector = selector,
        content = content,
    )
}
