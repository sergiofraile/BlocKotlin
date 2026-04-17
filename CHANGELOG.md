# Changelog

All notable changes to the `:bloc` library are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

---

## [1.0.0] — 2026-03-10

> First release published to [Maven Central](https://central.sonatype.com/artifact/io.github.sergiofraile/bloc).

### Added

- `Cubit<S>` — lightweight state emitter driven by direct method calls.
- `Bloc<S, E>` — event-driven state machine with typed event handlers registered via `on<EventType>`.
- `HydratedBloc<S, E>` — `Bloc` subclass with automatic state persistence and restoration using `kotlinx.serialization`.
- `HydratedStorage` interface with `InMemoryStorage` (for tests) and `SharedPreferencesStorage` (Android) implementations.
- `BlocObserver` — global singleton for observing `onCreate`, `onEvent`, `onChange`, `onTransition`, `onError`, and `onClose` lifecycle hooks.
- `EventTransformer` — sealed class controlling handler concurrency: `Sequential`, `Concurrent`, `Droppable`, `Restartable`, `Debounce(duration)`, `Throttle(duration)`.
- `BlocRegistry` — type-safe service locator backing `BlocProvider`.
- Compose integration: `BlocBuilder`, `BlocListener`, `BlocSelector`, `BlocConsumer`, `BlocProvider`.
- Full unit test suite covering `Cubit`, `Bloc`, `HydratedBloc`, `BlocObserver`, `BlocRegistry`, and all `EventTransformer` strategies.
- Sample app (`:app`) with 7 interactive examples: Counter, Stopwatch, Calculator, Heartbeat, Score Board, Formula One, Lorcana.
- Apache 2.0 license.

[Unreleased]: https://github.com/sergiofraile/BlocKotlin/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/sergiofraile/BlocKotlin/releases/tag/v1.0.0
