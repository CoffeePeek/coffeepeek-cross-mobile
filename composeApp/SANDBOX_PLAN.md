# Application-wide offline Sandbox checkpoint

## Develop baseline reconciliation — 2026-10-19

Inherit Home `c165d8fc`, main `dfb00e63` and approved develop `fc85f8e5`.
Keep the full actual application graph/isolated clients; do not restore the
retired calendar-only navigator/Environment/photo viewer when resolving the
parent. Remove ML extractor/Shop processor overrides and synthetic drink cutouts
from bootstrap, seeds and fake responses. Ordinary local media/uploads, photo-less
visit counts, session scenarios, root navigation and network isolation remain.
The actual migrated Home entries no longer require an extraction port.

Verification: app unit suites, Direct Debug/Sandbox assemblies and their packaging/
isolation checks plus app instrumentation compilation. This synchronization does
not claim a new manual full-app walkthrough, author-block fake contract support,
live backend or iOS acceptance. Parent Home/history UI evidence is recorded in
their owning plans. No ordinary CoffeePeek or Sandbox package is installed here.

Independent #101 reconciliation verification: **534 app Direct unit results**,
zero failures/errors/skips; Direct Debug and full Direct Sandbox APKs, their
packaged DEX/isolation guards and app Direct instrumentation compilation pass
(1m55s), first run successful. Retired calendar-only host files stay deleted.
Local log: `coffeepeek-develop-sandbox-build-2026-10-19.log`. This build evidence
does not repeat or extend the historical manual full-app acceptance below.

Date: 2026-10-18. Branch `feature/sandbox-application`, child of Home checkpoint
1873b05 / PR #99. The Home PR remains focused; the application-wide host has its
own review boundary. No main/develop merge or new Gradle business module.

## Main synchronization follow-up — 2026-10-19

Main 28917758 was propagated through the migration stack, preserving analytics-off
Sandbox composition. Its new uncached image client now uses the injected API
transport factory as well: API, plain, upload and image clients must all be
MockEngine-backed here. The existing transport regression checks all four engines
and reads the synthetic image through the image client. Manifest/DEX isolation
still applies. No real app installation, device check or network acceptance is
implied by assembly. Incoming author-blocking flows and protected-photo lifecycle
scenarios need explicit fake fixtures/acceptance; unsupported routes still fail
closed, never fall through to a server. Earlier walkthrough evidence remains
dated to its recorded build, not proof of all new-main flows.

## Plan and ownership

1. [x] Reuse the actual root navigation/tab graph and Android feature bindings.
   Remove the old independent Home/calendar Sandbox navigator, not product UI.
2. [x] Add optional client factories to legacy HTTP composition; production
   defaults and request/auth semantics remain unchanged. Replace all API/plain/
   upload engines, session/auth and image fetching in Sandbox only. The earlier
   fake extraction override is retired with the accepted develop baseline.
3. [x] Seed discovery, coffee, Shop, roaster, profile, feed and favorite fixtures;
   share mutations with history. Preserve private/pending feed exclusion.
4. [x] Inject synthetic photo selection throughout the app and an offline map
   style. Block external activities instead of escaping into live services.
5. [x] Preserve no-INTERNET manifest and normal-APK forbidden-class guards.
   Verify both delivery flavors and Native compilation compatibility.
6. [x] Complete the scoped physical Pixel 7 navigation/CRUD/error/theme/guest
   walkthrough below. This is not certification of every destination or remote
   contract; unsupported and release gates remain explicit.
7. [x] Publish draft child PR #101 to #99, retaining explicit unsupported-contract
   and final-device gates. Runtime checkpoint: 994cb846; nothing merged.

## Coverage and limits

See [SANDBOX.md](SANDBOX.md) for build/run, navigation, controls and exclusions.
All content is synthetic. The actual app graph and screen implementations are
used, but no production Application startup, realtime, analytics or real account
is used. Local login fields are ignored, not stored/transmitted. Preferences
use the separate app sandbox; fake backend/session content resets on process death.
Unsupported operations fail closed, never fall through to the real server.
Moderation submission/recovery/follow/deletion/update lifecycles and OAuth are
not certified by browseable routes; real tiles/camera/ML are intentionally absent.

## Verification evidence

- Initial implementation compile failed on a malformed fixture JSON-builder
  closure; fixed without changing a product assertion.
- First physical application-wide launch exposed a separate default image HTTP
  fetcher. Android's missing INTERNET permission blocked its request and the
  Sandbox process crashed. Excluded that fetcher, retained only decoders/mappers
  plus mock URL/protected-image and byte-array fetchers. Did not add network
  permission. Subsequent Sandbox process remained alive and rendered real tabs.
- First new contract suite: five of seven cases failed because test public
  addresses lacked required revision/isAlias fields. Fixed fixtures; full suite
  and both app unit variants then passed. No assertion weakened or failed run hidden.
- Broad regression passes after those repairs: 448 cases per app variant, both
  Sandbox manifests/APKs/DEX, both ordinary debug APK/DEX, and app iOS Simulator
  Arm64 compilation. Native is compatibility only, not iOS UI acceptance.
  Logs outside Git: coffeepeek-sandbox-application-final-2026-10-18.log and
  coffeepeek-sandbox-application-verified-2026-10-18.log. Latest filter case and
  final bootstrap checks are recorded after the last regression below.
- Studio inspection of the client found unnecessary retained factory properties;
  converted them to constructor-only parameters. Android assembly-file inspection
  timed out: no complete clean IDE/preview claim. Existing unrelated AGP/KMP,
  serialization and expect/actual warnings remain out of scope.
- Pixel 7 was connected/unlocked for deployment, then auto-locked during builds.
  Only `com.coffeepeek.sandbox` was updated (1.0.656-sandbox and the image-loader
  repair build); the ordinary app was not installed, cleared or mutated. Final
  device verification is pending unlock; do not count the earlier failed launch
  or calendar-only APK acceptance as complete application-wide QA.

Final local regression: **449/449 per Direct/Play** (898 unit results, zero
failures/errors/skips), including 17 Sandbox transport cases per variant. The
eight new application cases use the actual CoffeePeekClient/legacy services,
Coffee repository and migrated Shop writer; all three client engines are MockEngine.
They cover address decoding, shared favorites, profile edits/reset, coffee reads,
nearby search/filter flags, Shop upload/history and private/pending feed exclusion,
write errors and rejected unsupported mutations. Ordinary debug APK/DEX pass;
both Sandbox manifests/APK/DEX pass. Final log:
coffeepeek-sandbox-application-safe-final-2026-10-18.log (11s). Earlier final
filter/bootstrap regression: coffeepeek-sandbox-application-complete-2026-10-18.log
(12s). No concurrent Gradle builds, all limited to six workers.

Final Direct Sandbox is **1.0.656-sandbox**, SHA-256
`588302f9c12e9c8397ceb6d81e579fc95ea9d51b23e1314313a950cebee9644f`.
It includes a movable debug handle and a no-op/error-only update installer; no
Play Store/Package Installer action can be invoked by that port. Source identity:
1873b05 plus this application-wide checkpoint diff. The phone's final full UI
walkthrough remains a gate, not inferred from the unit/assembly passes above.
Final APK update succeeded over our own Sandbox without uninstall/data clear;
the updated process stayed alive while Pixel 7 remained locked. Ordinary
CoffeePeek stayed at 1.0.648. This is not an unlocked UI walkthrough.

## Unlocked application walkthrough — 2026-10-18

The pending unlock gate above is now superseded by this scoped manual run.
Initial walkthrough used 994cb846 runtime / 1.0.656-sandbox. The redesigned
profile failed twice, including its retry: its public-profile DTO requires
additional statistics and isFollowing that the older /me DTO defaults had hidden.
Added those fixture fields and corrected the unwrapped followers/following page
contract. A new test uses the actual PublicProfileRepositoryImpl for metadata,
activity, favorites and both connection kinds, rather than only the older user API.
The profile successfully rendered after the repair; no production UI was changed.

Repaired source: 5a27cb22 plus SandboxApplicationBackend/profile regression diff.
Direct Sandbox **1.0.658-sandbox**, SHA-256
`8e1506602cb5fea5715db8107e03834ef3a97be80be47f98c99f241abb5505ac`.
Updated in place on physical Pixel 7 / Android API 37, normal window/font settings.
No uninstall, data clear, security or device-setting changes. Ordinary CoffeePeek
remained **1.0.648** and was not installed or mutated.

Manual fake-transport checks (not an automated UI suite):

- Discovery list, Shop detail, Coffee list/detail, Community feed and Settings
  loaded on 1.0.656; repaired profile loaded on 1.0.658. These use the actual
  root navigation, not the old independent calendar host.
- On 1.0.658, profile -> All Check-ins opened full combined history: four seeded
  visits, including three Home preparations. Home detail -> edit changed rating
  4 to 5 and displayed the saved value. Confirmed deletion removed that synthetic
  row. A new Home preparation with synthetic original/cutout saved and appeared
  in history; photo viewer opened and Back returned correctly.
- Shop creation with synthetic photo succeeded through the actual form. Its
  My Check-ins action opened shared history with **5 visits / 3 Home**, and four
  visits on today's calendar cell. The private save stayed private. Public-feed
  exclusion of private/pending writes is asserted separately by unit tests.
- Shop favorite remove/add round trip on 1.0.656; full three-kind favorites
  destination and roaster detail opened on 1.0.658. Actual Coffee and feed tabs
  were browsed; this does not certify every filter, pagination or interaction.
- Offline and HTTP 500 modes displayed a recoverable Discovery error. Empty
  reset showed no shops; populated reset restored both local shops. SaveError
  kept the Home editor open with a service-unavailable message. UploadError with
  synthetic photo kept the editor open with the photo-preparation field error,
  not a success screen. Closed failed forms before resetting controls.
- Light/dark visual checks included Shop, settings/theme, repaired profile and
  combined calendar. The debug handle dragged away from controls. Guest mode
  removed Profile from the real tab graph; local login restored it (both initial
  and repaired APK walkthroughs). Restored populated/normal/local-account state.
- Current Sandbox process crash-buffer check contained no crash entry. Local
  screenshots/UI dumps remain outside Git. No real photos, backend writes,
  OAuth, analytics acceptance or production-account session changes were tested.

Regression after the fixture repair: **450/450 per Direct/Play** (900 unit
results, zero failures/errors/skips), including **18 Sandbox cases per variant**.
Both Sandbox APK/manifest/DEX guards pass. Both ordinary debug APK/DEX guards
also pass, including forbidden Sandbox/MockEngine definitions. Logs:
coffeepeek-sandbox-profile-contract-2026-10-18.log (55s) and
coffeepeek-sandbox-profile-production-isolation-2026-10-18.log (6s).
Studio could not inspect the Sandbox source because the active IDE model excludes
that source root; no clean IDE inspection is claimed. Import/sync, expanded
font/window/accessibility coverage, unsupported remote lifecycles and live
server/media/release acceptance remain gates, not inferred from this walkthrough.

## Return to feature refactoring

After this host is verified, first rerun the remaining Home/history acceptance
scenarios safely in Sandbox. Keep real API/media/account/release gates separate.
Then audit fresh main and choose the next cohesive owner; Coffee catalogue
(list/detail/filtering + the existing Home bean picker contract) is a candidate,
not authorization to recreate removed screens or start empty modules. Extract
api/domain/data/impl in a new child checkpoint, preserve existing visuals and
root navigation, and use this same Sandbox rather than another demo screen.
