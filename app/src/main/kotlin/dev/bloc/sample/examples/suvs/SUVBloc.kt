package dev.bloc.sample.examples.suvs

import dev.bloc.Bloc
import dev.bloc.sample.BlocSampleApp
import kotlinx.coroutines.launch

/**
 * Manages the SUV instance lifecycle.
 *
 * Demonstrates:
 * - Protocol-based dependency injection (SUVRepositoryProtocol)
 * - Auth flow state machine (Initial → Authenticating → Loaded)
 * - Repository pattern for data access
 * - Complex multi-state transitions
 *
 * The default repository uses the real Suvify API when `assets/suvify.properties`
 * contains a valid `suvify_key`; it falls back to [MockSUVRepository] otherwise.
 */
class SUVBloc(
    private val repository: SUVRepositoryProtocol = defaultRepository(),
) : Bloc<SUVState, SUVEvent>(SUVState.Initial) {

    init {
        on<SUVEvent.Login> { event, emit ->
            emit(SUVState.Authenticating)
            scope.launch {
                try {
                    val user = repository.login(event.username, event.password)
                    emit(SUVState.Authenticated(user))
                    emit(SUVState.LoadingInstances(user))
                    try {
                        val instances = repository.fetchInstances(user.userName, user.token)
                        emit(SUVState.Loaded(user, instances))
                    } catch (e: Exception) {
                        addError(e)
                        emit(SUVState.Error(user, e.message ?: "Failed to load instances"))
                    }
                } catch (e: Exception) {
                    addError(e)
                    emit(SUVState.AuthError(e.message ?: "Authentication failed"))
                }
            }
        }

        on<SUVEvent.Logout> { _, emit ->
            emit(SUVState.Initial)
        }

        on<SUVEvent.RefreshInstances> { _, emit ->
            val user = state.currentUser ?: return@on
            emit(SUVState.LoadingInstances(user))
            scope.launch {
                try {
                    val instances = repository.fetchInstances(user.userName, user.token)
                    emit(SUVState.Loaded(user, instances))
                } catch (e: Exception) {
                    addError(e)
                    emit(SUVState.Error(user, e.message ?: "Failed to refresh"))
                }
            }
        }

        on<SUVEvent.ExtendInstance> { event, emit ->
            val loaded = state as? SUVState.Loaded ?: return@on
            emit(loaded.copy(extendingId = event.instanceId))
            scope.launch {
                try {
                    repository.extendInstance(event.instanceId, event.hours, loaded.user.token)
                    val instances = repository.fetchInstances(loaded.user.userName, loaded.user.token)
                    emit(SUVState.Loaded(loaded.user, instances))
                } catch (e: Exception) {
                    addError(e)
                    emit(SUVState.Loaded(loaded.user, loaded.instances))
                }
            }
        }
    }

    private companion object {
        fun defaultRepository(): SUVRepositoryProtocol {
            val app = BlocSampleApp.instance ?: return MockSUVRepository()
            return SUVRepository(app)
        }
    }
}
