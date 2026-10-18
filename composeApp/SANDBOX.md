# Offline Android Sandbox

Sandbox is a separate application, not a switch inside the production app.
Its package is `com.coffeepeek.sandbox`, its launcher label is **CoffeePeek Sandbox**
and its icon is purple. Production remains `com.coffeepeek` with its existing data.

Build/select `directSandbox` in Android Studio, or run:

```shell
./gradlew :composeApp:assembleDirectSandbox --max-workers=6
```

APK: `composeApp/build/outputs/apk/direct/sandbox/composeApp-direct-sandbox.apk`.
`playSandbox` is also available; both delivery variants have the same Sandbox
package, so install only the preferred one. Normal debug/release IDs are unchanged.

## Included

- Real migrated combined-history and Home detail/create/edit/delete entries,
  ViewModels, validation, DTO mapping, data repositories and Kotlin Result contracts.
- App-owned manual composition; actual Ktor requests use local `MockEngine`
  gateway/upload clients, not fake UI state or a second feature implementation.
- Seeded Home rows, several preparations on one day, rows without photos, local
  original/cutout fixtures, and a read-only shop visit in combined history.
- Controls for normal/delayed/offline/500 responses, write/upload errors,
  empty/populated reset and light/dark theme. Close the form to change scenarios.
- Creation, editing and deletion affect only in-memory fixtures. Data is reset
  after process death, or with the reset buttons. The real server/account is never used.

## Safety boundary

- Generic `android.app.Application` and `SandboxActivity`, no production root
  Application, Koin graph, session store, realtime, analytics installation or updater.
- Manifest removes INTERNET, CAMERA, location, storage, phone and APK-install permissions; production app links
  and MainActivity are absent. Provider authorities use the Sandbox package.
- API URL is `.invalid`; OAuth/analytics configuration is blank/disabled.
- Both clients have MockEngine only. Foreign hosts and unimplemented endpoints
  fail closed; no fallback engine, real token, login or backend deployment.
- Sandbox Kotlin/MockEngine dependencies are variant-only. Normal debug APK
  verification rejects Sandbox/mock-engine DEX definitions. Sandbox assemblies
  verify manifest isolation and required packaged runtime classes automatically.
- Backend contract tests compile that exact transport source as test-only input
  in the ordinary Direct/Play unit tests, without shipping it in live APKs.

## Deliberate limits

The photo-source buttons return **synthetic local PNGs**. Camera/gallery launch,
URI decoding and ML extraction are not exercised; the banner states this. No
private photos are bundled. Original/cutout reservation and upload requests do
exercise the real Home data layer, but upload bytes stay in memory.

This first host covers Home and combined history, not the whole application.
Shop creation/navigation, auth, profile and other business features are not wired;
unsupported navigation explains the limit instead of opening a live screen.
Synthetic success/error responses are not evidence of server contract acceptance,
production persistence, permissions, live analytics, moderation or iOS readiness.

Keep test credentials, private images and device logs out of this folder/repository.
Verification/build identities and outstanding acceptance checks are recorded in
`feature/home-check-in/MIGRATION_PLAN.md`.

Verified on 2026-10-18: Pixel 7, Direct Sandbox 1.0.650-sandbox, alongside the
unchanged ordinary CoffeePeek 1.0.648. Local create (original + cutout), edit,
delete, history invalidation, calendar indicators, photo viewer/Back, light/dark,
offline/500 and failed save/upload were exercised. This is manual fake-transport
acceptance, not a run against the live API. Each Android unit variant passes
441 cases, including nine Sandbox transport cases (also cancellation); build
logic passes four DEX-scanner cases. IDE source inspection requires Gradle Sync
to import the new variant/source set; no clean IDE inspection is claimed yet.
