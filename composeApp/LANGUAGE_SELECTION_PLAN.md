# Russian and Belarusian application language

Date: 2026-11-01. Child branch: `codex/migration-language-selection-20261101`.
Parent UI checkpoint: `ef6263b024ee5b93bc531efffe51793e31f1ce1d` / build 2641.
This feature is not in that already-built parent artifact.

## Behavior and ownership

- Exactly two in-app choices, with autonyms `Русский` and `Беларуская`.
  Missing/unsupported stored preference uses Russian. Save before publishing the
  selected language; retain the old selection on failure and support retry.
- `feature/settings` owns the language enum, narrow port, typed MVI and one shared
  language screen. The rest of its migrated settings UI remains Android-only.
  Existing API/impl modules gain the common Compose support needed by that leaf;
  there are no new Gradle modules or a second Settings implementation.
- Application composition owns Room preference adaptation, app lifetime, platform
  locale application, DI and routes. Feature domain/API do not depend on Room,
  platform locale APIs or navigation. Preference writes serialize; cancellation
  remains cancellation.
- Resources stay with their existing owners. Russian remains the base fallback;
  Belarusian files cover the same keys, array lengths, attributes and formatting.
  Active hardcoded UI copy moves into paired resources. User/server content,
  brands and machine/cancellation protocol tokens retain their meaning.
- The design-system resource getters observe an app-provided language tag and
  refresh only resource reads. The navigation/controller/composable trees are
  not keyed by language. Android preserves the Activity context chain and other
  configuration fields; UIKit tab titles update in place. Display dates follow
  Russian/Belarusian month forms; ISO/POSIX parsing and UTC day rules stay intact.
- Existing native purpose strings need app-root `InfoPlist.strings` in both
  Xcode and xtool packaging. Project resource links use Swift/XcodeProj tooling,
  not manual PBX text changes. Auth diagnostic/cancellation codes are preserved.
- Notification preferences move from both inbox sheets to an inline Settings row
  directly below Language. It retains the existing master toggle, account, loading, retry
  and device-permission states. Opening an inbox from its bell and a successful
  check-in call the same guarded permission offer; mounting or refreshing a list
  does not request permission. Native prior-denial/asked guards remain in force.
  An explicit first open waits for settings to load, once for that account
  generation. Closing/disposal or an account change cancels the opportunity.
  Settings composes the application-provided notification slot through its public
  API; it does not import notification domain/data/implementation. Inbox items are
  flat rows with shared separators, retaining unread and action semantics.
- Android Play bundles retain both language resource sets offline. Activity-based
  integrations use the existing activity-compose owner lookup through the locale
  context wrapper; no OAuth or real permission choice is exercised by tests.

This checkpoint exceeds the usual 100–150-file review guide because resource
coverage and resource-getter import updates span the existing app and feature
owners. Most import changes are mechanical. Splitting by technical layer would
expose a selector with incomplete resource behavior. No unrelated feature or
module migration is included.

## Verification plan and evidence

Preserve navigation, drafts, account/session guards, notification refresh queues,
update retry behavior, analytics consent/exposure ownership, theme/city settings,
safe areas and accessibility. Do not perform real-account writes or OAuth flows.

1. Parse both locale trees; compare key/type/attribute/placeholder parity, arrays,
   escaping and duplicate keys. Audit active hardcodes separately from fixtures,
   technical protocol tokens and backend/user text.
2. Run Settings MVI and app preference persistence/ordering/failure/cancellation
   contracts, existing affected feature suites and display-date contracts.
3. Compile the shared language leaf for iOS, assemble Android with packaged DEX
   guards, then run full macOS app compilation/native contracts. A Linux-skipped
   app target does not establish UIKit compilation.
4. Manual Sandbox review: both languages, selection/restart, back/navigation
   retention, themes, long/large-font text and modal placement. No automated UI,
   instrumented-Compose or screenshot tests/infrastructure are added or run.
5. Deliver a source-identified signed Debug build. The user explicitly deferred
   Android installation; do not install or ask again until they resume it.

Initial feature run: `language-feature-verification.log` executed notifications
22 tests, with three failures after newly introduced suspend resource reads
delayed error publication and queued refresh. The concurrent app-update test task
also stopped making progress during resource work. The exact Gradle client was
terminated (exit 143); this run is not a pass. Original log/XML are retained
locally outside tracked sources. The fix uses semantic feedback in presentation
state and localizes in rendering, preserving failure and retry coordination.
The retry succeeded: Settings 38, Profile 60, Notifications 22, App Update 19,
Author Blocking 4 and design-system 57 tests (200 across 34 suites), zero failures,
errors or skips. Some unaffected suites reused results from the initial run.

After inbox permission coverage was added, the focused feature run succeeded in
6 seconds: Settings 38, Profile 60, Notifications 29, App Update 19 and Author
Blocking 4 tests (150 across 18 suites), zero failures, errors or skips. Existing
unchanged suites were up to date. Fake repositories/platform ports are used;
these results do not certify native permission dialogs or live server writes.

Initial application integration failed on unescaped Kotlin format placeholders;
the retry compiled production Android sources but failed compiling six stale
unit-test references. Both failures are retained in local logs. A corrected full
application run remains required. The Linux app iOS target is skipped by Kotlin's
cinterop cross-compilation guard because shelf.data imports ONNX via SwiftPM;
it is not an app iOS compilation pass. Full app compilation requires macOS.

Parent visual checkpoint 2641 passed a full unsigned physical-iPhone Debug build
on macOS and Android DirectDebug assembly with packaged DEX guards. The archive
source revision and SHA-256 were checked locally before separate Debug identity
preparation. Native navigation/configuration contracts were not repeated for this
small visual checkpoint; the last full execution remains build 2640 (16 tests in
4 suites, zero failures/errors/skips).
The user connected/unlocked the iPhone; an in-place update was installed and
re-read as CoffeePeek Debug 2641 on iPhone 15 / iOS 27.0.1. The separate TestFlight
2633 remains installed. The user confirmed that the thumb, rail and fasteners
look correct in build 2641.

Final static resource audit matches 2,486 production string pairs and 21
arrays (141 items), including placeholders and attributes. Internal Sandbox
scenario/debug-panel resources are outside this production translation scope.
The 44 confirmed reachable legacy error/validation sites also use semantic state
or UI-prepared copy. Technical compatibility messages in legacy data preconditions
and raw backend/user content are preserved; presentation uses the typed helpers.

Final application verification first assembled DirectDebug and passed its DEX
runtime-class check, then failed unit compilation on two misplaced test-fixture
declarations. Only those fixtures changed for the retry. The retry passed in
11 seconds: app 219, Settings 38, Notifications 29, Shelf 58, Home Check-in 81 and
Community 40 tests (465 in 54 suites), zero failures/errors/skips. App tests
executed in the retry; feature tests reused successful executions from earlier
runs. Design-system and Settings iOS KLIB tasks were up to date from prior passes.
Full app macOS compilation, language artifact packaging and language device
acceptance are the remaining integration/delivery gates at this source checkpoint.

After that checkpoint, the user requested an inline notification toggle instead
of a leaf and flat notification rows separated by lines. The not-yet-delivered
2642 iOS/Android workflows were cancelled to build the revised source together.
The 465-test result and Play-bundle evidence belong to `4474d9a9`.

Revised inline/list integration passed in 45 seconds: Android DirectDebug assembly
and DEX runtime-class checks, plus app 217, Settings 38, Notifications 32,
design-system 57, Shelf 58, Home Check-in 81 and Community 40 unit tests (523 in
70 suites), zero failures/errors/skips. All seven test tasks executed in this run.
Design-system and Settings iOS KLIB compilation executed and passed. Separate
notification-page routes/files and their new, unshipped navigation tests were
removed. Full application iOS compilation and physical-device review remain open.
Existing preview-annotation/expect-actual and unrelated test style warnings were
retained without expanding the scope.

Static resource/previews and newly written contracts are not executed-test or
device evidence. Final counts, source/build identity, device outcomes and open
gates must be recorded after integration checks. Existing SDK/preview warnings
are not a reason for unrelated cleanup.

Open manual gates include changing both languages and restarting, preserving an
open route/draft while switching, long text/font scale in both themes, and opening
the notification list before its first settings response, closing it while that
response is pending, and changing accounts. No IDE previews, system permission
dialogs, OAuth flows or real notification writes have been exercised for this
language checkpoint.

## Primary API references

- [JetBrains resource environment guidance](https://github.com/JetBrains/kotlin-multiplatform-dev-docs/blob/master/topics/compose/compose-resource-environment.md)
  and cached Compose 1.12.1 sources informed the locale/resource refresh boundary.
- [Android application language documentation](https://developer.android.com/guide/topics/resources/app-languages)
  distinguishes platform APIs from ComponentActivity-compatible configuration.
- [Apple localized Info.plist guidance](https://developer.apple.com/library/archive/documentation/General/Reference/InfoPlistKeyReference/Articles/AboutInformationPropertyListFiles.html)
  defines localization of the native permission descriptions.
