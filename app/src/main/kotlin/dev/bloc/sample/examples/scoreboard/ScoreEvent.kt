package dev.bloc.sample.examples.scoreboard

sealed interface ScoreEvent {
    data object AddPoint : ScoreEvent
    data object Reset    : ScoreEvent
}
