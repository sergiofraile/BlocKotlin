package dev.bloc.sample.examples.heartbeat

data class HeartbeatState(
    val tickCount: Int,
    val isRunning: Boolean,
) {
    val formattedDuration: String
        get() = "%02d:%02d".format(tickCount / 60, tickCount % 60)

    companion object {
        val initial = HeartbeatState(tickCount = 0, isRunning = false)
    }
}
