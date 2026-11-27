# Android packaged runtime checks

Reviewed 2026-11-27 against `VerifyApkRuntimeClassesTask`, its unit tests and `composeApp/build.gradle.kts`.

Direct/Play Debug assembly finalizers inspect the actual APK's DEX class definitions for required startup composition classes. A descriptor in a string/type table is only a reference, not a class definition; successful compilation alone does not exclude `NoClassDefFoundError` at startup. Sandbox assembly checks its required runtime classes plus manifest isolation. Ordinary Debug rejects Sandbox/mock-engine definitions.

Name-based startup checks apply to unminified variants. Release R8 may rename/inline classes, so this exact check is not applied to Release.

## Diagnose a failure

1. Record source SHA, variant, APK path, original failure and missing class definition.
2. Trace the class through compiler outputs, runtime JAR, library intermediates and final DEX. Confirm the dependency/source set before changing DI or duplicating dependencies.
3. If evidence points to stale generated output, rebuild only affected module/app outputs without cache reuse. For example, when network outputs are implicated:

```shell
./gradlew :core:network:clean :composeApp:clean :composeApp:assembleDirectDebug --no-build-cache --max-workers=6
```

Run sequentially. This cleans generated outputs; do not delete global caches, uninstall, clear user data or downgrade an installed APK as a shortcut. Recheck the final APK definition and report the first failure alongside a successful retry. A stale-output symptom does not establish the upstream cache/packaging root cause.

For an actual packaging/DI crash, a non-destructive startup smoke test on the agreed device remains necessary after compatible in-place installation. Follow [Android run modes](../composeApp/ANDROID_RUN_MODES.md). Startup survival does not prove authentication or all feature flows.

## Artifact identity

- Debug: `composeApp/build/outputs/apk/direct/debug/composeApp-direct-debug.apk` or the corresponding `play/debug` path.
- Sandbox: `composeApp/build/outputs/apk/direct/sandbox/composeApp-direct-sandbox.apk`.
- An old unflavored `apk/debug/composeApp-debug.apk` is not the current artifact.

The historical packaging incident and its device result are retained in Git history. This document states the current guard and diagnostic procedure, without reusing an old successful run as fresh acceptance.
