package dev.bloc.sample.examples.heartbeat

sealed interface HeartbeatEvent {
    data object Start : HeartbeatEvent
    data object Tick  : HeartbeatEvent
}
