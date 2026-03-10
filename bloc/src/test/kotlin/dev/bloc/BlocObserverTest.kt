package dev.bloc

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Verifies that [BlocObserver.shared] receives every lifecycle notification from
 * every Bloc and Cubit in the app — mirroring the iOS BlocObserverTests.
 */
class BlocObserverTest {

    @get:Rule val dispatcher = MainDispatcherRule()

    private lateinit var observer: TrackingBlocObserver
    private lateinit var originalObserver: BlocObserver

    @Before fun setUp() {
        originalObserver = BlocObserver.shared
        observer = TrackingBlocObserver()
        BlocObserver.shared = observer
    }

    @After fun tearDown() {
        BlocObserver.shared = originalObserver
    }

    // ── onCreate ──────────────────────────────────────────────────────────────

    @Test fun `onCreate fires when a Bloc is created`() {
        val before = observer.createdCount
        CounterBloc().also { it.close() }
        assertEquals(before + 1, observer.createdCount)
    }

    @Test fun `onCreate fires when a Cubit is created`() {
        val before = observer.createdCount
        CounterCubit().also { it.close() }
        assertEquals(before + 1, observer.createdCount)
    }

    // ── onEvent ───────────────────────────────────────────────────────────────

    @Test fun `onEvent fires once for each send`() {
        val bloc = CounterBloc()
        val before = observer.eventCount
        bloc.send(TestEvent.Increment)
        bloc.send(TestEvent.Decrement)
        assertEquals(before + 2, observer.eventCount)
        bloc.close()
    }

    @Test fun `onEvent does not fire for Cubit method calls`() {
        val cubit = CounterCubit()
        val before = observer.eventCount
        cubit.increment()
        assertEquals(before, observer.eventCount)
        cubit.close()
    }

    // ── onChange ──────────────────────────────────────────────────────────────

    @Test fun `onChange fires for every Bloc state change`() {
        val bloc = CounterBloc()
        val before = observer.changeCount
        bloc.send(TestEvent.Increment)
        assertEquals(before + 1, observer.changeCount)
        bloc.close()
    }

    @Test fun `onChange fires for every Cubit state change`() {
        val cubit = CounterCubit()
        val before = observer.changeCount
        cubit.increment()
        cubit.increment()
        assertEquals(before + 2, observer.changeCount)
        cubit.close()
    }

    // ── onTransition ──────────────────────────────────────────────────────────

    @Test fun `onTransition fires for synchronous Bloc emit`() {
        val bloc = CounterBloc()
        val before = observer.transitionCount
        bloc.send(TestEvent.Increment)
        assertEquals(before + 1, observer.transitionCount)
        bloc.close()
    }

    @Test fun `onTransition does not fire for Cubit emit`() {
        val cubit = CounterCubit()
        val before = observer.transitionCount
        cubit.increment()
        assertEquals(before, observer.transitionCount)
        cubit.close()
    }

    // ── onError ───────────────────────────────────────────────────────────────

    @Test fun `onError fires when Bloc calls addError`() {
        val bloc = CounterBloc()
        val before = observer.errorCount
        bloc.addError(RuntimeException("test"))
        assertEquals(before + 1, observer.errorCount)
        bloc.close()
    }

    @Test fun `onError fires when Cubit calls addError via subclass`() {
        val cubit = object : Cubit<Int>(0) {
            fun fireError(t: Throwable) = addError(t)
        }
        val before = observer.errorCount
        cubit.fireError(RuntimeException("test"))
        assertEquals(before + 1, observer.errorCount)
        cubit.close()
    }

    // ── onClose ───────────────────────────────────────────────────────────────

    @Test fun `onClose fires when Bloc is closed`() {
        val bloc = CounterBloc()
        val before = observer.closeCount
        bloc.close()
        assertEquals(before + 1, observer.closeCount)
    }

    @Test fun `onClose fires when Cubit is closed`() {
        val cubit = CounterCubit()
        val before = observer.closeCount
        cubit.close()
        assertEquals(before + 1, observer.closeCount)
    }

    @Test fun `onClose fires only once even when close is called multiple times`() {
        val bloc = CounterBloc()
        val before = observer.closeCount
        bloc.close()
        bloc.close()
        assertEquals(before + 1, observer.closeCount)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tracking observer — records every lifecycle call
// ─────────────────────────────────────────────────────────────────────────────

private class TrackingBlocObserver : BlocObserver() {
    var createdCount    = 0
    var eventCount      = 0
    var changeCount     = 0
    var transitionCount = 0
    var errorCount      = 0
    var closeCount      = 0

    override fun <S : Any> onCreate(emitter: StateEmitter<S>)                                      { createdCount++ }
    override fun <S : Any, E : Any> onEvent(bloc: Bloc<S, E>, event: E)                            { eventCount++ }
    override fun <S : Any> onChange(emitter: StateEmitter<S>, change: Change<S>)                   { changeCount++ }
    override fun <S : Any, E : Any> onTransition(bloc: Bloc<S, E>, transition: Transition<E, S>)   { transitionCount++ }
    override fun <S : Any> onError(emitter: StateEmitter<S>, error: Throwable)                     { errorCount++ }
    override fun <S : Any> onClose(emitter: StateEmitter<S>)                                       { closeCount++ }
}
