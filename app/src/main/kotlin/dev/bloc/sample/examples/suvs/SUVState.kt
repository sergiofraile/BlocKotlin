package dev.bloc.sample.examples.suvs

import dev.bloc.sample.examples.suvs.models.SuvActiveDirectoryUser
import dev.bloc.sample.examples.suvs.models.SuvInstance

sealed interface SUVState {
    data object Initial : SUVState
    data object Authenticating : SUVState
    data class  AuthError(val message: String) : SUVState
    data class  Authenticated(val user: SuvActiveDirectoryUser) : SUVState
    data class  LoadingInstances(val user: SuvActiveDirectoryUser) : SUVState
    data class  Loaded(
        val user: SuvActiveDirectoryUser,
        val instances: List<SuvInstance>,
        val extendingId: String? = null,
    ) : SUVState
    data class  Error(val user: SuvActiveDirectoryUser, val message: String) : SUVState
}

val SUVState.currentUser: SuvActiveDirectoryUser? get() = when (this) {
    is SUVState.Authenticated    -> user
    is SUVState.LoadingInstances -> user
    is SUVState.Loaded           -> user
    is SUVState.Error            -> user
    else                         -> null
}

val SUVState.isAuthenticated: Boolean get() = currentUser != null
