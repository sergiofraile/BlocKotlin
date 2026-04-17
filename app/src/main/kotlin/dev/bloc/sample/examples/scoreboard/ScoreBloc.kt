package dev.bloc.sample.examples.scoreboard

import dev.bloc.Bloc

/**
 * A simple score tracker that demonstrates BlocListener, BlocBuilder, and BlocConsumer.
 *
 * State is a plain Int (the current score). Two events:
 * - AddPoint: increments score by one.
 * - Reset: returns score to zero.
 *
 * ## BlocListener demo
 * ScoreScreen wraps content with a BlocListener that fires only at every 5-point milestone.
 * The listener is a side effect — it shows a milestone banner without rebuilding UI.
 *
 * ## BlocConsumer demo
 * The Tier badge uses BlocConsumer with buildWhen (rebuild only at tier boundaries every 10 pts)
 * AND listenWhen (fire a pulse animation at those same moments) — two behaviours, one component.
 */
class ScoreBloc : Bloc<Int, ScoreEvent>(initialState = 0) {
    init {
        on<ScoreEvent.AddPoint> { _, emit -> emit(state + 1) }
        on<ScoreEvent.Reset>    { _, emit -> emit(0) }
    }
}
