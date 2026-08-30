# Module BlocKotlin

![BlocKotlin](images/banner.png)

A Kotlin implementation of the [Bloc pattern](https://bloclibrary.dev/) for building Android
applications in a consistent, testable, and understandable way, using Jetpack Compose.

Inspired by the [Dart Bloc library](https://bloclibrary.dev) — a Kotlin port of the
[bloc](https://pub.dev/packages/bloc) package originally created by Felix Angelov for Flutter/Dart.
The same event-driven state-management pattern, brought natively to Android with Kotlin and
Jetpack Compose. See the [BlocSwift](https://github.com/sergiofraile/BlocSwift) project for the
iOS/Swift counterpart.

## Getting started

```kotlin
dependencies {
    implementation("io.github.sergiofraile:bloc:1.1.2")
}
```

```kotlin
import dev.bloc.Bloc

class CounterBloc : Bloc<Int, CounterEvent>(initialState = 0) {
    init {
        on<Increment> { _, emit -> emit(state + 1) }
        on<Decrement> { _, emit -> emit(state - 1) }
    }
}
```

Full guide, core concepts, Compose integration, and examples are in the
[README on GitHub](https://github.com/sergiofraile/BlocKotlin).

## Key types

| Type | Purpose |
|------|---------|
| `Cubit` | Minimal state container — emit new state directly via functions |
| `Bloc` | Event-driven state container — map incoming events to state transitions |
| `HydratedBloc` | `Bloc` that persists and restores its state across process death |
| `BlocProvider` / `blocState()` | Compose integration for scoping and observing blocs |
| `BlocObserver` | Global hook into every bloc's lifecycle, events, and errors |
| `EventTransformer` | Control event concurrency (sequential, droppable, restartable, …) |
