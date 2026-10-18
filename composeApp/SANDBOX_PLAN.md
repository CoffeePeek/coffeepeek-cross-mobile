# Application-wide offline Sandbox checkpoint

Date: 2026-10-18. Branch `feature/sandbox-application`, child of Home checkpoint
1873b05 / PR #99. The Home PR remains focused; the application-wide host has its
own review boundary. No main/develop merge or new Gradle business module.

## Plan and ownership

1. [x] Reuse the actual root navigation/tab graph and Android feature bindings.
   Remove the old independent Home/calendar Sandbox navigator, not product UI.
2. [x] Add optional client factories to legacy HTTP composition; production
   defaults and request/auth semantics remain unchanged. Replace all API/plain/
   upload engines, session/auth, image fetching and extraction in Sandbox only.
3. [x] Seed discovery, coffee, Shop, roaster, profile, feed and favorite fixtures;
   share mutations with history. Preserve private/pending feed exclusion.
4. [x] Inject synthetic photo selection throughout the app and an offline map
   style. Block external activities instead of escaping into live services.
5. [x] Preserve no-INTERNET manifest and normal-APK forbidden-class guards.
   Verify both delivery flavors and Native compilation compatibility.
6. [ ] Finish physical Pixel 7 navigation/CRUD/error/theme/guest verification on
   the final APK; compilation is not proof that every destination works.
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

## Return to feature refactoring

After this host is verified, first rerun the remaining Home/history acceptance
scenarios safely in Sandbox. Keep real API/media/account/release gates separate.
Then audit fresh main and choose the next cohesive owner; Coffee catalogue
(list/detail/filtering + the existing Home bean picker contract) is a candidate,
not authorization to recreate removed screens or start empty modules. Extract
api/domain/data/impl in a new child checkpoint, preserve existing visuals and
root navigation, and use this same Sandbox rather than another demo screen.
