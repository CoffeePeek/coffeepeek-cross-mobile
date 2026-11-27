#!/usr/bin/env bash
set -euo pipefail

case "${1:-}" in
  unit)
    tasks=(testDebugUnitTest :composeApp:testDirectDebugUnitTest :composeApp:testPlayDebugUnitTest)
    ;;
  lint)
    tasks=(lintDebug :composeApp:lintDirectDebug :composeApp:lintPlayDebug)
    ;;
  *)
    echo "Usage: bash tools/ci/run_android_checks.sh unit|lint" >&2
    exit 64
    ;;
esac

# SIGQUIT asks every JVM in timeout's process group for a thread dump. A stuck
# build still fails; never infer success from the last task banner or report.
# Keep the build cache, but exclude configuration-cache finalization from checks.
exec timeout --verbose --signal=QUIT --kill-after=30s "${ANDROID_CHECK_TIMEOUT:-25m}" \
  ./gradlew --no-daemon --no-configuration-cache --max-workers=6 \
  --console=plain --info --stacktrace \
  '-Dorg.gradle.jvmargs=-Xmx6g -Dfile.encoding=UTF-8' "${tasks[@]}"
