package dev.bloc.sample.examples.suvs

sealed interface SUVEvent {
    data class  Login(val username: String, val password: String) : SUVEvent
    data object Logout : SUVEvent
    data object RefreshInstances : SUVEvent
    data class  ExtendInstance(val instanceId: String, val hours: Int = 2) : SUVEvent
}
