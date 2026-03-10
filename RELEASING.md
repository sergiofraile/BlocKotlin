# Releasing BlocKotlin

This document describes the steps to cut a new release of the `:bloc` library.

---

## Prerequisites

- You are on the `main` branch with a clean working tree.
- All CI checks are passing.
- The four GitHub secrets are set (`SONATYPE_USERNAME`, `SONATYPE_PASSWORD`, `GPG_SIGNING_KEY`, `GPG_SIGNING_PASSWORD`).

---

## Steps

### 1. Bump the version

Edit `bloc/build.gradle.kts` and update the `version` inside the `mavenPublishing` block:

```kotlin
coordinates(
    groupId    = "io.github.sergiofraile",
    artifactId = "bloc",
    version    = "1.1.0",   // ← new version
)
```

Follow [Semantic Versioning](https://semver.org):

| Change | Bump |
|--------|------|
| Breaking API change | Major (`2.0.0`) |
| New backwards-compatible feature | Minor (`1.1.0`) |
| Bug fix | Patch (`1.0.1`) |

### 2. Update CHANGELOG.md

Move items from `[Unreleased]` into a new dated section:

```markdown
## [1.1.0] — YYYY-MM-DD

### Added
- ...

### Fixed
- ...
```

Add a comparison link at the bottom:

```markdown
[1.1.0]: https://github.com/sergiofraile/BlocKotlin/compare/v1.0.0...v1.1.0
```

### 3. Regenerate the API dump

If you added or changed any public API, regenerate the binary compatibility snapshot:

```bash
./gradlew :bloc:apiDump
```

Commit the updated `bloc/api/bloc.api` file along with the other changes.

### 4. Commit

```bash
git add .
git commit -m "chore: release v1.1.0"
git push origin main
```

### 5. Tag and push

```bash
git tag v1.1.0
git push origin v1.1.0
```

The GitHub Actions **release** workflow fires automatically, runs tests, signs the artifacts, publishes to Maven Central, and creates a GitHub Release with the AAR attached.

### 6. Verify

- Check the Actions tab to confirm the workflow succeeded.
- Visit [central.sonatype.com](https://central.sonatype.com/artifact/io.github.sergiofraile/bloc) to confirm the new version is live (can take up to 30 min to appear in search).
- Update any documentation that references the previous version number.
