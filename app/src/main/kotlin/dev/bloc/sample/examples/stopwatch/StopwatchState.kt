package dev.bloc.sample.examples.stopwatch

/**
 * The state managed by [StopwatchCubit].
 *
 * [elapsed] is stored as a [Double] of **seconds** with centisecond precision.
 * All display helpers are pure computed properties so the Cubit never needs
 * to format strings — that responsibility stays in the UI layer.
 */
data class StopwatchState(
    val elapsed: Double,
    val isRunning: Boolean,
) {
    private val totalCentiseconds: Int get() = (elapsed * 100).toInt()

    val minutes:      Int get() = totalCentiseconds / 6_000
    val seconds:      Int get() = (totalCentiseconds % 6_000) / 100
    val centiseconds: Int get() = totalCentiseconds % 100

    val minutesDisplay:      String get() = "%02d".format(minutes)
    val secondsDisplay:      String get() = "%02d".format(seconds)
    val centisecondsDisplay: String get() = "%02d".format(centiseconds)

    companion object {
        val initial = StopwatchState(elapsed = 0.0, isRunning = false)
    }
}
