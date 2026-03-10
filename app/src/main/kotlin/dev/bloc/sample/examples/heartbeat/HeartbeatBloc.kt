package dev.bloc.sample.examples.heartbeat

import dev.bloc.Bloc
import dev.bloc.Change
import dev.bloc.sample.examples.calculator.LogEntry
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A Bloc that runs an async one-second ticker to demonstrate **scoped lifecycle management**.
 *
 * ## Why this example exists
 *
 * Most Blocs in this app are registered once in BlocProvider and live for the entire session.
 * HeartbeatBloc is different — it is NOT registered globally. Instead, the screen creates it
 * directly, starts it on appearance, and calls close() on disappear. This is the correct
 * pattern for Blocs that are scoped to a single screen.
 *
 * ## What to watch
 *
 * 1. Navigate to Heartbeat → onCreate fires, ticker starts.
 * 2. Watch ticks accumulate in the lifecycle log every second.
 * 3. Navigate away → close() fires → onClose fires and the ticker is cancelled immediately.
 * 4. Return → a brand-new HeartbeatBloc is created, starting from zero.
 */
class HeartbeatBloc : Bloc<HeartbeatState, HeartbeatEvent>(HeartbeatState.initial) {

    private val _logFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logFlow: StateFlow<List<LogEntry>> = _logFlow.asStateFlow()

    private val _closedFlow = MutableStateFlow(false)
    val closedFlow: StateFlow<Boolean> = _closedFlow.asStateFlow()

    private var tickerJob: Job? = null

    init {
        on<HeartbeatEvent.Start> { _, emit ->
            emit(HeartbeatState(tickCount = 0, isRunning = true))
            tickerJob = scope.launch {
                while (!isClosed) {
                    delay(1_000L)
                    if (!isClosed) send(HeartbeatEvent.Tick)
                }
            }
        }
        on<HeartbeatEvent.Tick> { _, emit ->
            emit(state.copy(tickCount = state.tickCount + 1))
        }
    }

    fun clearLog() { _logFlow.value = emptyList() }

    private var pendingEventEntry: LogEntry? = null

    private fun appendEntries(vararg entries: LogEntry) {
        val updated = _logFlow.value + entries
        _logFlow.value = if (updated.size > 200) updated.drop(entries.size) else updated
    }

    override fun onEvent(event: HeartbeatEvent) {
        super.onEvent(event)
        // Stash the event entry so it can be batched with the paired onChange into a single update.
        pendingEventEntry = when (event) {
            HeartbeatEvent.Start -> LogEntry(kind = LogEntry.Kind.EVENT, message = "start — ticker launched")
            HeartbeatEvent.Tick  -> LogEntry(kind = LogEntry.Kind.EVENT, message = "tick #${state.tickCount + 1}")
        }
    }

    override fun onChange(change: Change<HeartbeatState>) {
        super.onChange(change)
        val changeEntry = if (change.nextState.tickCount == 0) {
            LogEntry(kind = LogEntry.Kind.CHANGE, message = "session started")
        } else {
            LogEntry(kind = LogEntry.Kind.CHANGE, message = "${change.currentState.formattedDuration} → ${change.nextState.formattedDuration}")
        }
        // Flush the stashed event entry together with the change entry in one single update
        // so the list only recomposes once per tick instead of twice.
        val event = pendingEventEntry
        pendingEventEntry = null
        if (event != null) appendEntries(event, changeEntry) else appendEntries(changeEntry)
    }

    override fun onClose() {
        super.onClose()
        tickerJob?.cancel()
        tickerJob = null
        _closedFlow.value = true
        appendEntries(
            LogEntry(
                kind    = LogEntry.Kind.CLOSE,
                message = "Bloc closed after ${state.tickCount} tick${if (state.tickCount == 1) "" else "s"} — ticker cancelled",
            )
        )
    }
}
