# Contributing to BlocKotlin

Thank you for your interest in contributing! This document covers everything you need to get started.

---

## Table of contents

- [Code of conduct](#code-of-conduct)
- [Reporting bugs](#reporting-bugs)
- [Requesting features](#requesting-features)
- [Development setup](#development-setup)
- [Project structure](#project-structure)
- [Making changes](#making-changes)
- [Coding conventions](#coding-conventions)
- [Running tests](#running-tests)
- [Submitting a pull request](#submitting-a-pull-request)
- [Versioning](#versioning)

---

## Code of conduct

This project follows the [Contributor Covenant Code of Conduct](https://www.contributor-covenant.org/version/2/1/code_of_conduct/).
By participating you agree to uphold it.

---

## Reporting bugs

Use the **Bug report** issue template on GitHub. Please include:
- The library version, Kotlin version, and Android API level
- Minimal reproduction steps
- Actual vs expected behaviour
- Any relevant Logcat or stack trace output

---

## Requesting features

Use the **Feature request** issue template. Describe the problem you are solving, your proposed API, and any alternatives you considered. Features that maintain API parity with [BlocSwift](https://github.com/sergiofraile/BlocSwift) are prioritised.

---

## Development setup

### Requirements

| Tool | Version |
|------|---------|
| Android Studio | Meerkat (2024.3.1+) |
| JDK | 17+ |
| Kotlin | 2.0.21 |
| AGP | 9.0.1 |
| Gradle | 9.2.1 |

### Clone and open

```bash
git clone https://github.com/sergiofraile/BlocKotlin
cd BlocKotlin
```

Open in Android Studio → **File** → **Open** → select the root `BlocKotlin` folder → **Sync Project with Gradle Files**.

---

## Project structure

```
BlocKotlin/
├── bloc/   # ← library module — this is what you publish
└── app/    # ← sample app — used to validate the library in a real UI
```

All library changes go in `:bloc`. The `:app` module exists solely to demonstrate and manually test the library.

---

## Making changes

1. **Fork** the repository and create a branch from `main`:
   ```bash
   git checkout -b fix/my-bug-fix
   # or
   git checkout -b feat/my-new-feature
   ```

2. **Write your code** in `:bloc`. If you are adding or changing public API, update KDoc accordingly.

3. **Add or update tests** in `bloc/src/test/`.

4. **Update `CHANGELOG.md`** under the `[Unreleased]` section.

5. **Open a pull request** against `main` using the provided template.

---

## Coding conventions

- Follow the [Kotlin official code style](https://kotlinlang.org/docs/coding-conventions.html) (`kotlin.code.style=official` is set in `gradle.properties`).
- All public classes, functions, and properties must have KDoc.
- Prefer `data class` / `data object` for state and event types.
- Keep the core classes (`Cubit`, `Bloc`, `HydratedBloc`, `BlocObserver`, `EventTransformer`) free of Android-specific imports — they are KMP-ready.
- Android-specific code belongs in `SharedPreferencesStorage` or the `compose/` package.

---

## Running tests

```bash
# All unit tests in :bloc
./gradlew :bloc:test

# With a live test report (opens in browser)
./gradlew :bloc:test && open bloc/build/reports/tests/testDebugUnitTest/index.html
```

Tests use `UnconfinedTestDispatcher` from `kotlinx-coroutines-test`. Do not use `runBlocking` in new tests — always use `runTest`.

---

## Submitting a pull request

- Keep PRs focused: one fix or feature per PR.
- All CI checks must pass (`./gradlew :bloc:test` and `./gradlew :app:assembleDebug`).
- A maintainer will review and merge once it is approved.
- Squash-merge is preferred to keep the commit history clean.

---

## Versioning

BlocKotlin follows [Semantic Versioning](https://semver.org):

| Change | Version bump |
|--------|-------------|
| Breaking API change | Major (`2.0.0`) |
| New backwards-compatible feature | Minor (`1.1.0`) |
| Bug fix | Patch (`1.0.1`) |

Releases are tagged as `v1.0.0` and automatically published via the GitHub Actions release workflow.
