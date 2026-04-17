package dev.bloc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.Serializable
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.time.Duration.Companion.milliseconds

// ─────────────────────────────────────────────────────────────────────────────
// MainDispatcherRule
// ─────────────────────────────────────────────────────────────────────────────

/**
 * JUnit 4 rule that replaces [Dispatchers.Main] with [testDispatcher] for the duration
 * of every test. Required because Cubit and Bloc create their scopes with
 * `Dispatchers.Main.immediate`, which is unavailable in pure JVM unit tests.
 *
 * Pass in a [TestCoroutineScheduler] to share virtual time across the rule and [runTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val scheduler: TestCoroutineScheduler = TestCoroutineScheduler(),
) : TestWatcher() {
    val testDispatcher = UnconfinedTestDispatcher(scheduler)

    override fun starting(description: Description) = Dispatchers.setMain(testDispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

// ─────────────────────────────────────────────────────────────────────────────
// Test Cubit
// ─────────────────────────────────────────────────────────────────────────────

class CounterCubit : Cubit<Int>(0) {
    fun increment() = emit(state + 1)
    fun decrement() = emit(state - 1)
    fun reset()     = emit(0)
}

// ─────────────────────────────────────────────────────────────────────────────
// Test Bloc — events
// ─────────────────────────────────────────────────────────────────────────────

sealed interface TestEvent {
    data object Increment                        : TestEvent
    data object Decrement                        : TestEvent
    data class  IncrementBy(val amount: Int)     : TestEvent
    data class  SlowIncrement(val delayMs: Long) : TestEvent
}

// ─────────────────────────────────────────────────────────────────────────────
// Test Bloc — sequential (default)
// ─────────────────────────────────────────────────────────────────────────────

class CounterBloc : Bloc<Int, TestEvent>(0) {
    val capturedTransitions = mutableListOf<Transition<TestEvent, Int>>()
    val capturedEvents      = mutableListOf<TestEvent>()
    val capturedChanges     = mutableListOf<Change<Int>>()

    init {
        on<TestEvent.Increment>     { _, emit -> emit(state + 1) }
        on<TestEvent.Decrement>     { _, emit -> emit(state - 1) }
        on<TestEvent.IncrementBy>   { ev, emit -> emit(state + ev.amount) }
        on<TestEvent.SlowIncrement> { ev, emit -> kotlinx.coroutines.delay(ev.delayMs); emit(state + 1) }
    }

    override fun onEvent(event: TestEvent) {
        super.onEvent(event)
        capturedEvents += event
    }
    override fun onTransition(transition: Transition<TestEvent, Int>) {
        super.onTransition(transition)
        capturedTransitions += transition
    }
    override fun onChange(change: Change<Int>) {
        super.onChange(change)
        capturedChanges += change
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Transformer-specific Blocs
// ─────────────────────────────────────────────────────────────────────────────

class DroppableBloc : Bloc<Int, TestEvent>(0) {
    init {
        on<TestEvent.SlowIncrement>(
            transformer = EventTransformer.Droppable,
        ) { ev, emit -> kotlinx.coroutines.delay(ev.delayMs); emit(state + 1) }
    }
}

class RestartableBloc : Bloc<Int, TestEvent>(0) {
    init {
        on<TestEvent.SlowIncrement>(
            transformer = EventTransformer.Restartable,
        ) { ev, emit -> kotlinx.coroutines.delay(ev.delayMs); emit(state + 1) }
    }
}

class DebounceBloc : Bloc<Int, TestEvent>(0) {
    init {
        on<TestEvent.Increment>(
            transformer = EventTransformer.Debounce(200.milliseconds),
        ) { _, emit -> emit(state + 1) }
    }
}

class ConcurrentBloc : Bloc<Int, TestEvent>(0) {
    var handlerStartCount = 0

    init {
        on<TestEvent.SlowIncrement>(
            transformer = EventTransformer.Concurrent,
        ) { ev, emit ->
            handlerStartCount++
            kotlinx.coroutines.delay(ev.delayMs)
            emit(state + 1)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HydratedBloc test helpers
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class PersistState(val count: Int = 0)

sealed interface PersistEvent {
    data class Set(val count: Int) : PersistEvent
}

class PersistBloc(
    storage: HydratedStorage = InMemoryStorage(),
) : HydratedBloc<PersistState, PersistEvent>(
    initialState    = PersistState(),
    serializer      = PersistState.serializer(),
    storageKeyParam = "test_persist_bloc",
    storage         = storage,
) {
    init {
        on<PersistEvent.Set> { ev, emit -> emit(PersistState(ev.count)) }
    }
}
