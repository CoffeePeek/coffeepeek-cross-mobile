# Offline Android Sandbox

Sandbox is a separate application, not a switch inside the production app.
Its package is `com.coffeepeek.sandbox`, its launcher label is **CoffeePeek Sandbox**
and its icon is purple. Production remains `com.coffeepeek` with its existing data.

For choosing between Sandbox and live Debug, Android Studio launch steps and
explicit-device commands, see [Android run modes](ANDROID_RUN_MODES.md). Sandbox
is the default for safe development tests; ordinary Debug is not a fake backend.

Build/select `directSandbox` in Android Studio, or run:

```shell
./gradlew :composeApp:assembleDirectSandbox --max-workers=6
```

APK: `composeApp/build/outputs/apk/direct/sandbox/composeApp-direct-sandbox.apk`.
`playSandbox` is also available; both delivery variants have the same Sandbox
package, so install only the preferred one. Normal debug/release IDs are unchanged.

## Application coverage

- The actual `AppContent`/`Navigator`, session-aware tabs and Android feature
  bindings, not a calendar-only host or a copy of the app screens.
- Local fixtures for discovery/search, shop detail/menu/check-ins, coffee list/
  detail, roasters, Community feed, profile/public activity, favorites and the moderation lists/submissions of Contributions.
- Home CRUD/uploads and Shop creation, favorites add/remove and profile name/
  biography edits use actual repositories and local HTTP state. Private or
  pending visits are not fabricated as public feed entries.
- Coffee: three synthetic coffees (Ethiopia fruity/floral/light with two offers, Kenya fruity/light
  out of stock, Brazil nutty/medium) plus taste/roast filter groups. Search honours query, selected
  taste/roast and "available only"; detail offers have seller links (blocked by the external-app
  guard) and taste-based "similar" coffees. Empty/reset mode returns no coffees or groups.
- Author blocking: a second synthetic author (**Sandbox Friend**) has one public
  visit with a protected-style photo (original plus thumb/feed/detail variants).
  `GET/PUT/DELETE /api/v1/me/blocks` follow the real client contract (cursor pages,
  idempotent writes, self → 400, unknown → 404). Blocking hides that visit from the
  feed and Shop cards while server aggregates (`checkInCount`) stay unchanged;
  `GET /api/Users/sandbox-friend` returns `authorId`/`isBlockedByViewer`. The
  protected photo URL supports ETag revalidation and returns 404 (even with a valid
  validator) while the author is blocked. Write-error/500/offline scenarios apply.
- Tap the movable **SANDBOX** handle for normal/delayed/offline/500, write/upload
  errors, empty/populated reset, theme and local-account/guest controls. Drag the
  handle out of the way of real UI controls. Close a form before changing fixtures.
- Main content and session are in memory; process death/reset restores fixtures.
  Preferences use Sandbox's own Android storage, isolated by its package ID.

## Safety boundary

- Variant-only `SandboxApplication`/`SandboxActivity`, not production
  `CoffeePeekApplication.onCreate`. Reuse the existing Koin definitions with
  explicitly replaced HTTP, session, auth and image ports. No real
  credential store, realtime or analytics installation. The root hosts the update prompt
  (`AppUpdatePrompt`) like `App.kt`, but its installer never downloads or installs.
- Manifest removes INTERNET, CAMERA, location, storage, phone and APK-install permissions; production app links
  and MainActivity are absent. Provider authorities use the Sandbox package.
- API URL is `.invalid`; OAuth/analytics configuration is blank/disabled.
- All three legacy API/plain/upload clients have MockEngine only; feature
  factories receive these same clients. Kamel's default independent HTTP fetcher
  is excluded; both ordinary URL and protected-image requests use the mock client.
  Foreign hosts and unimplemented endpoints
  fail closed; no fallback engine, real token, login or backend deployment.
- Sandbox Kotlin/MockEngine dependencies are variant-only. Normal debug APK
  verification rejects Sandbox/mock-engine DEX definitions. Sandbox assemblies
  verify manifest isolation and required packaged runtime classes automatically.
- Backend contract tests compile that exact transport source as test-only input
  in the ordinary Direct/Play unit tests, without shipping it in live APKs.

## Deliberate limits

The shared photo-picker port returns **synthetic local PNGs**. Camera/gallery launch,
URI decoding are not exercised; no private photos are bundled. Drink cutouts/ML
extraction were retired with develop `fc85f8e5`; Sandbox does not recreate them.
Ordinary photo reservation and upload requests do
exercise the real Home/Shop data layer, but upload bytes stay in memory.

Blocking fixtures cover one author and the contracts above; they do not model
incoming (two-way) blocks, follow removal, pagination beyond one author or the real
server's revision semantics beyond a change counter. Local login is not tied to
block state (blocks reset with the fixtures).

Root destinations are the existing application routes. Route availability does
not imply every remote contract is emulated: moderation submissions, password
recovery email delivery, follow/account-deletion lifecycle and update delivery
are not complete fixtures yet; unsupported calls fail rather than pretending to
save. Local email/password login does not store or transmit either field. OAuth
is disabled. External browsers, map providers and share targets are blocked;
the map has an inline background plus local markers, not real geographic tiles.
Synthetic success/error responses are not evidence of server contract acceptance,
production persistence, permissions, live analytics, moderation or iOS readiness.

Keep test credentials, private images and device logs out of this folder/repository.
Verification/build identities and outstanding acceptance checks are recorded in
`composeApp/SANDBOX_PLAN.md`. The earlier calendar-only checkpoint remains
recorded in `feature/home-check-in/MIGRATION_PLAN.md`.

Historical calendar-only check on 2026-10-18: Pixel 7, Direct Sandbox 1.0.650-sandbox, alongside the
unchanged ordinary CoffeePeek 1.0.648. Local create (original + cutout), edit,
delete, history invalidation, calendar indicators, photo viewer/Back, light/dark,
offline/500 and failed save/upload were exercised. This is manual fake-transport
acceptance, not a run against the live API. Each Android unit variant passes
441 cases, including nine Sandbox transport cases (also cancellation); build
logic passes four DEX-scanner cases. IDE source inspection requires Gradle Sync
to import the new variant/source set; no clean IDE inspection is claimed yet.

## Contributions fixtures

`SandboxContributionsBackend` serves `ModerationShops`, `ModerationRoasters` and `ShopChangeRequests` (list `/mine` with status, paging and, for changes,
shop and section filters; create/submit; read and edit of a change request) plus the equipment, bean and shop-tag catalogs the forms read. Submissions live in RAM
and are cleared by Reset. Choose the fixture with the controls row «Мои вклады» or at launch with `--es sandbox_contributions <mode>`:
`Seeded` (default: shops, roasters and changes in every status, a rejected reason, an approved shop that is not published yet), `Paged` (45 more approved items),
`Empty`, `ListFailure` (every list answers 500), `SubmitFailure` (every create, submit and edit is refused with a server message).
Not covered: moderation decisions (a submission stays pending), uploaded photo keys are not checked against the reservation, no real map or location.

