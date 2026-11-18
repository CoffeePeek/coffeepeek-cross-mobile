# Native iOS business ownership checkpoint

2026-11-18, branch `codex/ios-native-business-ui`, PR base `codex/ios-native`.

## Active entries

| Entry | Native content | Shared owner |
| --- | --- | --- |
| Business tab | SwiftUI shop/roaster cards, pagination and check-in QR | OwnershipViewModel(businessTab=true) |
| MyBusiness / editor route | SwiftUI claims, details, business drafts, photo controls, publish/discard/reload confirmations | Existing OwnershipViewModel and parent event collector |
| BusinessOwnershipEntry.ProfileActions claim | Native UIKit page sheet with SwiftUI form/details | Existing isolated OwnershipClaimSheet store and generation dismissal |
| Shop/Roaster owner toolbar | Shared app-owned `iosOwnershipActionsController` creates SwiftUI edit/representative menu | Same OwnershipEntry/OwnershipAccess; no second ViewModel |

BusinessOwnershipEntry API is preserved. AccountSwitch and its existing native Settings integration remain intact. Native entry selection happens in feature platform hooks. Android retains the existing feature screens. Replaced iOS screens have no selectable Compose UI fallback; composition still owns lifecycle, native controller hosting and the existing platform photo picker.

`iosOwnershipActionsController(ownership, language, kind, id, slug, allowed, onSuggest, onOwnerEdit)` lives in application composition. Shop and Roaster provide current identity and callbacks; favorite/share remain in their native SwiftUI header. The factory reads the entry's existing access and editor ID, calls the same edit/login navigation, and opens the original isolated claim lifecycle. Native toolbar claim presentation closes on account generation replacement. The parent integration updates the two active Shop/Roaster consumers to this factory after this checkpoint lands.

Validation, ownership rules, writes, conflict/partial-save recovery, generation guards, submission bounds, draft dirtiness and event delivery stay in Kotlin. Swift renders immutable snapshots and sends typed user intents. Native photo selection reuses OwnershipPhotoPicker and its current iOS normalization; stale results keep the original generation guard. Private photos stay behind the account-aware Kotlin media loader and image keys include confirmed profile revisions. Claim dates use the existing formatter. Native QR rendering encodes the injected existing check-in link and exports a 1024px PNG through the existing sharing port.

UIKit controllers retain the Swift observable model; SwiftUI graph rebuilds do not revoke the shared session. Composition owns screen disposal and clears claim ViewModelStore as before. UIKit swipe-dismiss attempts enter the same Back/discard/busy logic. Native publish alerts leave confirmation ownership with the shared ViewModel.

## Verification

- Plan: preserve claim modal vs full destination behavior, native access identity mapping, private state/generation guards, publish confirmation and ProfileSaved event ownership, pagination, picker return, conflict recovery, QR link and share contract.
- Windows 2026-11-18: isolated `:feature:business-ownership:impl:compileIosMainKotlinMetadata` passed (common and iOS feature bridges). `python -m unittest discover -s tools/ios -p 'test_*.py'`: 29 passed. `git diff --check`: passed.
- After integration of global overlays (#403): `verifyFunctionBodyLimits` passed, 132 existing records and zero new violations. Fresh MainViewController factory blocks and Xcode registrations were reviewed after conflict resolution; ActionButton fontSize default 17 is retained.
- macOS run `44268689443` at `8276afba8` stopped in application Kotlin compilation (build and test jobs) because the merge resolution omitted a factory closing brace. Rebuilt Main from the verified integration source and added the complete ownership forwarder; no native ownership tests executed on that failed revision.
- Root body gate: old ownership factory false-positive removed with an explicit factory body/cohesive native access extraction. No baseline extension. The old public-profile factory false-positive on the original branch base is separately fixed by the parent integration.
- Added native unit/contract regressions for immediate generation privacy, shared publish/event semantics, and native toolbar access/editor-ID reuse. Existing macOS ios-verify runs all ownership domain/data/impl tests, including these new iOS tests; compiled metadata is not executed-test evidence.
- Paired light/dark SwiftUI previews live beside tab, overview, claim form/details, editor, owner actions and QR. They use fake snapshots, no DI/network.
- Pending physical device/manual gates: evidence/gallery return and cancellation, busy and dirty sheet swipe/back behavior, native ownership route transitions, QR scanning/PNG sharing, private photo authorization/account replacement, live claim/save/conflict acceptance, light/dark, large fonts and VoiceOver. No live account writes or automated UI/screenshot/instrumented tests were run.
