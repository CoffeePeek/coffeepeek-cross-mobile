# Android debug runtime packaging checks

Both debug app variants automatically run `VerifyApkRuntimeClassesTask` after
assembly. The task follows each variant's APK artifact provider and reads DEX
class definitions for the startup composition classes. Merely finding a class
descriptor in a DEX string/type table is insufficient: a missing class may still
be referenced by another class, which is exactly what produces a startup
`NoClassDefFoundError`.

The check is limited to unminified debug variants. Release R8 can legitimately
rename or inline these classes, so the same name-based check must not be applied
to release APKs.

## Incident and recovery

On 2026-10-08, `CheckInApiService` and its generated classes existed in the Kotlin
compiler output and runtime JAR, but 14 of those files were missing from
`modules/network/build/intermediates/runtime_library_classes_dir/debug/`.
The freshly assembled Direct APK also lacked the service's DEX definition,
despite a successful build. The library dependency was already present in the
application's common source set; changing Koin registration or adding a duplicate
dependency would not address that discrepancy.

Rebuilding the generated network/app outputs without build-cache reuse restored
the missing class:

```shell
./gradlew --no-daemon :modules:network:clean :composeApp:clean \
  :composeApp:assembleDirectDebug :composeApp:assemblePlayDebug --no-build-cache
```

This removes generated build outputs, not sources, application data or the global
Gradle cache. Use the current variant APK paths under `direct/debug` or
`play/debug`; an older `apk/debug/composeApp-debug.apk` is not the current flavored
artifact. Do not uninstall the app or clear its data as a packaging workaround.

After the rebuild, DEX inspection confirmed the service definition and an
in-place installation on the Pixel 7 from the crash log successfully launched
the application. The same process remained alive and had no new crash-buffer
entry. This startup smoke check does not verify every authenticated feature.

The new build guard catches this symptom in future local and CI debug assemblies.
It detects incomplete packaging; it does not establish which upstream
incremental-build/cache component produced the stale output.

## Local backend verification

On 2026-10-09, the old Railway URL from the setup template returned
`404 Application not found`. The repository's existing iOS configuration already
named `https://api.coffeepeek.by/`; public shop-list and city requests to that
address returned successful JSON envelopes and HTTP 200. Local Android
`local.properties` now uses that address and remains ignored/untracked.

Both generated Android BuildConfig variants contain the new address. Direct/Play
assembly, runtime-class checks, all debug unit tests, application tests and lint
passed after rebuilding. The Direct APK was updated in place on the Pixel 7,
started successfully and had no new crash-buffer entry. Device HTTP logs show
requests to the new API, but response/rendering and authenticated flows are not
yet confirmed by that startup check. No production writes or account resets
were performed; Google Sign-In remains disabled without a Web Client ID.
