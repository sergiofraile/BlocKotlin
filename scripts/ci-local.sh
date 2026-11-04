#!/usr/bin/env bash
# Local CI: runs the checks in .github/workflows/ on this machine, in the same
# order and with the same flags, so a push can be checked while GitHub Actions
# is unavailable. .githooks/pre-push runs the fast mode.
#
#   scripts/ci-local.sh          fast mode
#   scripts/ci-local.sh --full   fast mode plus the slow jobs
#
# Steps stop at the first failure, as a CI job does. A step whose tooling is
# missing here prints SKIPPED (reason) instead of failing. No step may change
# a tracked file; the script fails if one does.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

FULL=0
for arg in "$@"; do
  case "$arg" in
    --full) FULL=1 ;;
    -h|--help) sed -n '2,11p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "unknown argument: $arg" >&2; exit 2 ;;
  esac
done

START_ALL=$SECONDS
tree_state() { { git diff HEAD --no-ext-diff 2>/dev/null; git status --porcelain --untracked-files=no; } | sha1sum; }
TREE_BEFORE="$(tree_state)"

# step "name" command [args...]: runs the command in a subshell with errexit.
step() {
  local name="$1"; shift
  local t0=$SECONDS rc
  echo "==> $name"
  set +e
  ( set -e; "$@" )
  rc=$?
  set -e
  if [ "$rc" -ne 0 ]; then
    echo "    FAIL  $name ($((SECONDS - t0))s, exit $rc)"
    echo "local CI: FAILED at '$name' after $((SECONDS - START_ALL))s" >&2
    exit 1
  fi
  echo "    PASS  $name ($((SECONDS - t0))s)"
}

skip() { echo "==> $1"; echo "    SKIPPED ($2)"; }
have() { command -v "$1" >/dev/null 2>&1; }

finish() {
  if [ "$(tree_state)" != "$TREE_BEFORE" ]; then
    echo "local CI: FAILED, a step changed tracked files (see git status)" >&2
    exit 1
  fi
  echo "local CI: PASSED ($([ "$FULL" = 1 ] && echo full || echo fast) mode, $((SECONDS - START_ALL))s)"
}

# ---------------------------------------------------------------------------
# Mirrors ci.yml (jobs "Unit tests" and "Build sample app") in fast mode.
# --full adds the build steps of docs.yml (Dokka HTML) and release.yml
# (release AAR). Publishing, Pages upload and the GitHub release are not run.
# CI uses JDK 17; locally the JDK on PATH (or JAVA_HOME) is used.

if ! have java && [ -z "${JAVA_HOME:-}" ]; then
  echo "local CI: no JDK found" >&2
  skip "All Gradle steps" "no JDK (java not on PATH, JAVA_HOME unset)"
  finish
  exit 0
fi
if [ -z "${ANDROID_HOME:-}${ANDROID_SDK_ROOT:-}" ] && [ ! -f local.properties ]; then
  skip "All Gradle steps" "no Android SDK (set ANDROID_HOME or local.properties)"
  finish
  exit 0
fi

# ci.yml, job "Unit tests"
step "Run bloc unit tests" ./gradlew :bloc:test
step "Run bloc lint" ./gradlew :bloc:lintDebug

# ci.yml, job "Build sample app"
step "Assemble debug APK" ./gradlew :app:assembleDebug

if [ "$FULL" = 1 ]; then
  # docs.yml, job "Generate API docs" (output stays in bloc/build/)
  step "Generate Dokka HTML docs" ./gradlew :bloc:dokkaGeneratePublicationHtml
  # release.yml: tests already ran above; publishing needs secrets and is skipped
  skip "Publish to Maven Central" "release-only, needs signing secrets"
  step "Build release AAR" ./gradlew :bloc:assembleRelease
fi

finish
