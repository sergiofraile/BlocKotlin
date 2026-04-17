package dev.bloc.sample

import android.util.Log
import dev.bloc.Bloc
import dev.bloc.BlocObserver
import dev.bloc.Change
import dev.bloc.StateEmitter
import dev.bloc.Transition

/**
 * Application-wide [BlocObserver] that routes every Bloc and Cubit lifecycle event to
 * **Logcat** using structured tags and emoji prefixes, mirroring the Pulse integration
 * in the iOS counterpart.
 *
 * Registered once at app startup — every Bloc and Cubit is then logged automatically
 * with no per-Bloc boilerplate:
 *
 * ```kotlin
 * class App : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         BlocObserver.shared = AppBlocObserver()
 *     }
 * }
 * ```
 *
 * ## Logcat filtering
 *
 * All entries use the `"Bloc"` tag. Use the following filters in Logcat to focus on
 * specific event types:
 *
 * | Filter | Matches |
 * |---|---|
 * | `tag:Bloc` | Everything |
 * | `tag:Bloc level:debug` | Events only |
 * | `tag:Bloc level:info` | Changes + Transitions |
 * | `tag:Bloc level:error` | Errors only |
 *
 * Alternatively, filter by the bloc name that appears in each message, e.g.:
 * ```
 * tag:Bloc CounterBloc
 * ```
 *
 * ## Sample output
 *
 * ```
 * I/Bloc: 🚀 CounterBloc  initialState=CounterState(count=0)
 * D/Bloc: 📨 CounterBloc  event=CounterEvent.Increment
 * I/Bloc: ➡️ CounterBloc  Transition(current=0, event=Increment, next=1)
 * I/Bloc: 🔄 CounterBloc  Change(current=0, next=1)
 * I/Bloc: 🔒 CounterBloc  closed
 * E/Bloc: ❌ CounterBloc  java.lang.RuntimeException: something went wrong
 * ```
 */
class AppBlocObserver : BlocObserver() {

    private val tag = "Bloc"

    override fun <S : Any> onCreate(emitter: StateEmitter<S>) {
        super.onCreate(emitter)
        Log.i(tag, "🚀 ${emitter.name}  initialState=${emitter.state}")
    }

    override fun <S : Any, E : Any> onEvent(bloc: Bloc<S, E>, event: E) {
        super.onEvent(bloc, event)
        Log.d(tag, "📨 ${bloc.name}  event=$event")
    }

    override fun <S : Any, E : Any> onTransition(bloc: Bloc<S, E>, transition: Transition<E, S>) {
        super.onTransition(bloc, transition)
        Log.i(tag, "➡️ ${bloc.name}  $transition")
    }

    override fun <S : Any> onChange(emitter: StateEmitter<S>, change: Change<S>) {
        super.onChange(emitter, change)
        Log.i(tag, "🔄 ${emitter.name}  $change")
    }

    override fun <S : Any> onError(emitter: StateEmitter<S>, error: Throwable) {
        super.onError(emitter, error)
        Log.e(tag, "❌ ${emitter.name}  $error")
    }

    override fun <S : Any> onClose(emitter: StateEmitter<S>) {
        super.onClose(emitter)
        Log.i(tag, "🔒 ${emitter.name}  closed")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private val StateEmitter<*>.name: String
        get() = this::class.simpleName ?: "UnknownBloc"
}
