# bloc

A Kotlin Bloc state-management library for Android (and KMP-ready), mirroring the API of the Swift Bloc library.

The library implements the **BLoC (Business Logic Component)** pattern popularised by [bloclibrary.dev](https://bloclibrary.dev). State flows in one direction: events in → state out. All business logic lives in a `Bloc` or `Cubit`; composables observe state via lightweight reactive wrappers.

---

## Core Concepts

| Class / Interface | Description |
|-------------------|-------------|
| `StateEmitter<S>` | Common interface — exposes `state`, `stateFlow`, `errorsFlow`, `isClosed` |
| `Cubit<S>` | Simplest unit — emits state via direct method calls, no events |
| `Bloc<S, E>` | Full event-driven state machine — registers `on<EventType>` handlers |
| `HydratedBloc<S, E>` | `Bloc` subclass that persists and restores state automatically |
| `BlocObserver` | Global singleton for lifecycle hook observation |
| `EventTransformer` | Controls concurrency of event handlers (Sequential, Debounce, …) |

---

## Installation

### Via Maven Central (recommended)

`mavenCentral()` is already present in every Android project's `settings.gradle.kts`, so no repository change is needed. Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.sergiofraile:bloc:1.1.0")
}
```

Replace `1.1.0` with the [latest version on Maven Central](https://central.sonatype.com/artifact/io.github.sergiofraile/bloc).

### Local (monorepo / development)

If you have cloned the repository and are working within it:

```kotlin
dependencies {
    implementation(project(":bloc"))
}
```

---

## Cubit

`Cubit` is the simplest state-management primitive. Business logic lives in plain methods that call `emit(newState)`.

```kotlin
class CounterCubit : Cubit<Int>(initialState = 0) {
    fun increment() = emit(state + 1)
    fun decrement() = emit(state - 1)
    fun reset()     = emit(0)
}
```

### Observing a Cubit in Compose

```kotlin
@Composable
fun CounterView(cubit: CounterCubit) {
    val count by cubit.stateFlow.collectAsState()
    Text("Count: $count")
    Button(onClick = { cubit.increment() }) { Text("+") }
}
```

---

## Bloc

`Bloc` adds an **event layer** on top of `Cubit`. Each event type is registered once in `init` via `on<EventType>`.

```kotlin
// 1. Events
sealed interface CounterEvent {
    data object Increment : CounterEvent
    data object Decrement : CounterEvent
}

// 2. Bloc
class CounterBloc : Bloc<Int, CounterEvent>(initialState = 0) {
    init {
        on<CounterEvent.Increment> { _, emit -> emit(state + 1) }
        on<CounterEvent.Decrement> { _, emit -> emit(state - 1) }
    }
}

// 3. Dispatch
counterBloc.send(CounterEvent.Increment)
```

---

## HydratedBloc

`HydratedBloc` automatically persists state to `SharedPreferences` (or any `HydratedStorage`) and restores it on the next launch.

```kotlin
class CounterBloc : HydratedBloc<Int, CounterEvent>(
    initialState    = 0,
    serializer      = serializer(),           // kotlinx.serialization
    storageKeyParam = "counter",              // unique key per instance
) {
    init {
        on<CounterEvent.Increment> { _, emit -> emit(state + 1) }
    }
}
```

### Initializing storage

Call once in `Application.onCreate()`:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        HydratedBloc.storage = SharedPreferencesStorage(this)
    }
}
```

### Clearing persisted state

```kotlin
counterBloc.clearStoredState()   // clears this instance's key
HydratedBloc.clearAll()          // clears all registered HydratedBlocs
```

---

## BlocObserver

`BlocObserver` is a global hook for observing every Bloc/Cubit lifecycle event in one place — ideal for analytics or logging.

```kotlin
class AppBlocObserver : BlocObserver() {
    override fun <S : Any> onCreate(bloc: StateEmitter<S>) {
        Log.d("Bloc", "onCreate ${bloc::class.simpleName}")
    }
    override fun <S : Any, E : Any> onEvent(bloc: Bloc<S, E>, event: E) {
        Log.d("Bloc", "onEvent ${bloc::class.simpleName} — $event")
    }
    override fun <S : Any> onChange(bloc: StateEmitter<S>, change: Change<S>) {
        Log.d("Bloc", "onChange ${bloc::class.simpleName} — $change")
    }
    override fun <S : Any, E : Any> onTransition(bloc: Bloc<S, E>, transition: Transition<S, E>) {
        Log.d("Bloc", "onTransition ${bloc::class.simpleName} — $transition")
    }
    override fun <S : Any> onError(bloc: StateEmitter<S>, error: Throwable) {
        Log.e("Bloc", "onError ${bloc::class.simpleName}", error)
    }
    override fun <S : Any> onClose(bloc: StateEmitter<S>) {
        Log.d("Bloc", "onClose ${bloc::class.simpleName}")
    }
}

// Set in Application.onCreate()
BlocObserver.shared = AppBlocObserver()
```

> **Android debugging tip:** Filter Logcat by tag `BlocObserver` to see a real-time stream of all state changes across every Bloc and Cubit in your app — the equivalent of Pulse on iOS.

---

## EventTransformer

`EventTransformer` controls how concurrent events of the same type are handled.

| Strategy | Behaviour |
|----------|-----------|
| `Sequential` (default) | Events are queued and processed one at a time |
| `Concurrent` | All events run in parallel coroutines |
| `Droppable` | New events are dropped while a handler is active |
| `Restartable` | Active handler is cancelled; new event starts fresh |
| `Debounce(duration)` | Handler fires only after `duration` of silence |
| `Throttle(duration)` | At most one handler per `duration` |

```kotlin
// Debounce search — fires only after 300 ms of inactivity
on<SearchEvent>(transformer = EventTransformer.Debounce(300.milliseconds)) { event, emit ->
    val results = repository.search(event.query)
    emit(state.copy(results = results))
}

// Droppable — ignores taps while a network call is running
on<SubmitEvent>(transformer = EventTransformer.Droppable) { _, emit ->
    emit(state.copy(isLoading = true))
    val result = api.submit()
    emit(state.copy(isLoading = false, result = result))
}
```

---

## Compose Integration

### BlocBuilder

Rebuilds its content on every state change.

```kotlin
BlocBuilder(bloc = counterBloc) { state ->
    Text("Count: $state")
}
```

With `buildWhen` to restrict rebuilds:

```kotlin
BlocBuilder(
    bloc      = scoreBloc,
    buildWhen = { old, new -> old / 10 != new / 10 },  // only at tier boundaries
) { state ->
    TierBadge(tier = tierOf(state))
}
```

---

### BlocListener

Fires side effects on state changes **without** causing the content to rebuild.

```kotlin
BlocListener(
    bloc       = scoreBloc,
    listenWhen = { _, new -> new > 0 && new % 5 == 0 },
    listener   = { score -> showMilestoneBanner("🎯 $score points!") },
) {
    ScoreContent()
}
```

---

### BlocSelector

Rebuilds only when a **derived value** extracted from state changes. The most targeted rebuild primitive.

```kotlin
// Rebuilds only when isLoadingMore flips — not on every card append
BlocSelector(
    bloc     = lorcanaBloc,
    selector = { it.isLoadingMore },
) { isLoadingMore ->
    if (isLoadingMore) CircularProgressIndicator()
}

// Combine fields into an equatable snapshot
BlocSelector(
    bloc     = lorcanaBloc,
    selector = { PaginationSummary(hasMore = it.hasMorePages, count = it.cards.size) },
) { summary ->
    if (!summary.hasMore) Text("You've seen all ${summary.count} cards!")
}
```

---

### BlocConsumer

Combines `BlocListener` and `BlocBuilder` with independent `listenWhen`/`buildWhen` predicates into a **single subscription**.

Use when a state transition must trigger **both** a side effect and a conditional UI rebuild, driven by the same Flow emission.

```kotlin
BlocConsumer(
    bloc       = scoreBloc,
    listenWhen = { old, new -> tierOf(old) != tierOf(new) },
    listener   = { _ -> triggerTierPulseAnimation() },   // side effect
    buildWhen  = { old, new -> tierOf(old) != tierOf(new) },
) { state ->
    TierBadge(tier = tierOf(state))                      // rebuilds at tier change
}
```

The two predicates are **independent**: `listenWhen` tracks the last *emitted* state; `buildWhen` tracks the last *rendered* state. They can differ:

```kotlin
BlocConsumer(
    bloc       = scoreBloc,
    listenWhen = { _, new -> new % 5 == 0 },      // fires every 5 pts
    listener   = { _ -> playChime() },
    buildWhen  = { old, new -> old / 10 != new / 10 },  // rebuilds every 10 pts
) { state ->
    TierBadge(tier = tierOf(state))
}
```

---

### BlocProvider / BlocRegistry

Register Blocs globally so they can be resolved anywhere without prop-drilling.

```kotlin
// In your root composable
BlocProvider(counterBloc) {
    BlocProvider(scoreBloc) {
        AppContent()
    }
}

// Resolve anywhere
val counterBloc = BlocRegistry.resolve(CounterBloc::class)
```

---

## Lifecycle Management

### Global Blocs (BlocProvider)

Register in your root composable with `BlocProvider`. The Bloc lives as long as the composable is in the composition.

### Scoped Blocs (screen-level)

Create with `remember`, start with `LaunchedEffect`, and close with `DisposableEffect`:

```kotlin
@Composable
fun HeartbeatScreen() {
    val bloc = remember { HeartbeatBloc() }

    LaunchedEffect(bloc) { bloc.send(HeartbeatEvent.Start) }
    DisposableEffect(bloc) { onDispose { bloc.close() } }

    // UI ...
}
```

Navigate away → `onDispose` fires → `bloc.close()` is called → `onClose` fires → ticker is cancelled.

Navigate back → a fresh `HeartbeatBloc` is created from zero.

---

## Testing

Add to your test module:

```kotlin
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
```

Use `UnconfinedTestDispatcher` for synchronous bloc testing:

```kotlin
@get:Rule val mainDispatcherRule = MainDispatcherRule()

@Test
fun `increment updates state`() = runTest {
    val bloc = CounterBloc()
    bloc.send(CounterEvent.Increment)
    assertEquals(1, bloc.state)
    bloc.close()
}
```

---

## iOS Counterpart

This library mirrors the Swift Bloc library. The API is intentionally parallel:

| Kotlin | Swift |
|--------|-------|
| `Cubit<S>` | `Cubit<State>` |
| `Bloc<S, E>` | `Bloc<State, Event>` |
| `HydratedBloc<S, E>` | `HydratedBloc<State, Event>` |
| `BlocObserver` | `BlocObserver` |
| `EventTransformer.Debounce` | `EventTransformer.debounce` |
| `BlocBuilder { state -> }` | `BlocBuilder { bloc in }` |
| `BlocListener(listenWhen:listener:)` | `BlocListener(listenWhen:) { state in }` |
| `BlocSelector(selector:)` | `BlocSelector(selector:)` |
| `BlocConsumer(listenWhen:buildWhen:)` | `BlocConsumer(listenWhen:buildWhen:)` |
| Logcat + `AppBlocObserver` | Pulse + `AppBlocObserver` |

The `:bloc` module uses only Kotlin stdlib and Jetpack Compose (for the `compose/` integration layer). The core `Cubit`, `Bloc`, `HydratedBloc`, `BlocObserver`, and `EventTransformer` classes have no Android-specific dependencies and are ready for Kotlin Multiplatform extraction.
