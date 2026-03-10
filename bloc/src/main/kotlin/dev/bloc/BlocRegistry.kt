package dev.bloc

import kotlin.reflect.KClass

/**
 * A centralised, type-safe registry for [StateEmitter] instances (both [Bloc] and [Cubit]).
 *
 * `BlocRegistry` is the service locator that backs [BlocProvider][dev.bloc.compose.BlocProvider].
 * You rarely interact with it directly; instead, wrap your composables with `BlocProvider` and
 * resolve blocs where you need them.
 *
 * ## Overview
 *
 * ```kotlin
 * // Registration — done automatically by BlocProvider
 * BlocProvider(listOf(CounterBloc(), AuthBloc())) {
 *     AppContent()
 * }
 *
 * // Resolution — anywhere in the composable tree
 * val counterBloc = BlocRegistry.resolve(CounterBloc::class)
 * ```
 *
 * ## Error handling
 *
 * [resolve] throws an [IllegalStateException] with a descriptive message if the requested
 * type was not registered. This fail-fast behaviour catches configuration mistakes at
 * development time.
 *
 * ## Thread safety
 *
 * All operations are expected to run on the main thread, consistent with the rest of the
 * Bloc library.
 */
object BlocRegistry {

    // Internal store — KClass key → StateEmitter instance
    @PublishedApi
    internal val store = LinkedHashMap<KClass<*>, StateEmitter<*>>()

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /**
     * Registers a list of [StateEmitter] instances, replacing any previous registrations.
     *
     * Called internally by `BlocProvider`; you rarely need to call this directly.
     */
    fun register(blocs: List<StateEmitter<*>>) {
        store.clear()
        blocs.forEach { store[it::class] = it }
    }

    /** Convenience overload accepting varargs. */
    fun register(vararg blocs: StateEmitter<*>) = register(blocs.toList())

    // -------------------------------------------------------------------------
    // Resolution
    // -------------------------------------------------------------------------

    /**
     * Resolves a [StateEmitter] by its concrete [KClass].
     *
     * ```kotlin
     * val counterBloc = BlocRegistry.resolve(CounterBloc::class)
     * ```
     *
     * @throws IllegalStateException if no emitter of [type] is registered.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : StateEmitter<*>> resolve(type: KClass<T>): T {
        return store[type] as? T ?: error(
            buildString {
                appendLine("'${type.simpleName}' has not been registered in BlocProvider.")
                appendLine()
                val registered = store.keys.joinToString { it.simpleName ?: "?" }
                appendLine("Currently registered: [${registered.ifEmpty { "none" }}]")
                appendLine()
                appendLine("Wrap your composable with BlocProvider:")
                appendLine()
                appendLine("    BlocProvider(listOf(${type.simpleName}(...))) {")
                appendLine("        YourContent()")
                append("    }")
            }
        )
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Closes all registered emitters and clears the store.
     *
     * Called automatically by `BlocProvider`'s `DisposableEffect` when the composable
     * leaves the composition.
     */
    fun closeAll() {
        store.values.forEach { it.close() }
        store.clear()
    }

    // -------------------------------------------------------------------------
    // Hydration utilities
    // -------------------------------------------------------------------------

    /**
     * Calls [AnyHydratedBloc.resetToInitialState] on every registered [HydratedBloc],
     * then wipes the entire [HydratedBloc.storage].
     *
     * Use this as a "clean-slate" action — equivalent to uninstalling and reinstalling
     * the app, applied immediately without a restart:
     *
     * ```kotlin
     * // In a settings or debug screen
     * BlocRegistry.resetAllHydrated()
     * ```
     */
    fun resetAllHydrated() {
        store.values
            .filterIsInstance<AnyHydratedBloc>()
            .forEach { it.resetToInitialState() }
        HydratedBloc.clearAll()
    }
}
