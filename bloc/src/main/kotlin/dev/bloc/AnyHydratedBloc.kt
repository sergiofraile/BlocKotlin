package dev.bloc

/**
 * Type-erased interface for [HydratedBloc] operations that need to work
 * without knowledge of the concrete state and event type parameters.
 *
 * Used internally by [BlocRegistry.resetAllHydrated] to reset every registered
 * HydratedBloc without needing to cast to `HydratedBloc<*, *>`.
 */
interface AnyHydratedBloc {
    /** Deletes the persisted state without changing the current in-memory state. */
    fun clearStoredState()

    /**
     * Deletes the persisted state and immediately emits `initialState`,
     * resetting both storage and the live UI in one call.
     */
    fun resetToInitialState()
}
