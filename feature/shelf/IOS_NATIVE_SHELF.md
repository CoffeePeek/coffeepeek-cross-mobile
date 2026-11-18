# Native iOS shelf checkpoint

Source base: `codex/ios-native`, 2026-11-18. Scope: native UI with shared Kotlin ViewModels, repository, scanner, and write contracts.

## Routes and ownership

| Active entry | Native UI | Shared owner |
| --- | --- | --- |
| Profile > Shelf | SwiftUI shelf grid, summary, visibility, reorder, finish, undo | ShelfViewModel / ShelfProfileVisibilityViewModel |
| Public profile > Shelf | SwiftUI read-only bags and catalogue links | PublicShelfProfileViewModel |
| ShelfAddBag (global + / profile) | SwiftUI camera, scanner preparation/progress/retake, catalogue form, duplicate confirmation | ShelfAddViewModel |
| ShelfBag (profile / archive) | SwiftUI details, brews, finish, delete and restore | ShelfBagViewModel |
| ShelfArchive | Existing SwiftUI archive | ShelfArchiveViewModel |

Feature-owned iOS sessions adapt the existing ViewModels and own their ViewModelStore. Application composition maps native snapshots, wires navigation, authorization and the existing native Home check-in form. Swift owns rendering and observation subscriptions. The iOS Kotlin presentation session owns the transient finish selection (five stars by default, nullable buy-again); SwiftUI renders it and submits intents. Native root routes and profile embedded slots both use these entries. The iOS ShelfScreenRenderer delegates nested navigation to the same native controllers; it no longer selects the Compose shelf screens. Android keeps its existing feature entries.

The native camera reuses IosShelfCameraSession (AVFoundation focus, metering, torch, portrait capture and normalization), and gallery reuses the existing PHPicker delegate. Scanner/model readiness, preparation, original-photo processing, moderation and processed-card uploads remain in Kotlin. Existing package runtime choices are preserved. Photo selection/cancellation does not dismiss the shelf destination. Photos selected after a revoked owner session are ignored. Public cards use the anonymous bounded photo repository; private cards use the existing authorized media loader. Closing clears the ViewModelStore, camera and pending work.

The existing form edits optional bag metadata before creation; there is no existing edit-an-already-saved-bag UI to retire. No additional update writer or business state was introduced.

## Verification

- Plan: verify native handoff/finish/undo, scanner download/photo continuity, duplicate writes while saving, public read-only/hidden projection, cancellation and existing ViewModel contracts.
- 2026-11-18, Windows host: `python -m unittest discover -s tools/ios -p 'test_*.py'` — 30 passed, including the cross-language printf resource contract. `git diff --check` — passed. Isolated `:feature:shelf:impl:compileIosMainKotlinMetadata` — passed (common + iOS feature bridge metadata).
- Full application metadata on Windows stopped at the pre-existing generated Room expect/actual collision in core/database, before application compile. Body-limit validation reports only the two existing ownership/public-profile violations; this checkpoint adds none and does not change the baseline.
- macOS run `44264106089` at `812ab4c4a`: all 94 shelf unit/contract tests passed (including four new native-session regressions), and the complete Kotlin/Native test job passed. Kotlin/Native framework embedding passed; Xcode stopped at Swift optional inference in the weight choices. Retained the hosting model for the UIKit controller lifetime. Run `44270561997` at `378ff70ae` again passed the entire Kotlin/Native test job; its Xcode diagnostic identified the exact remaining type mismatch: NSNumber.intValue imports as Swift Int and must be compared with Int, rather than Int32. The corrective batch removes that mismatch.
- Fixed the Kotlin-to-NSString localization boundary: positional `%n$s` placeholders become `%n$@`, keeping argument indices for dates and month/year strings. A resource contract test verifies this without rendering UI.
- macOS `ios-verify` builds both Kotlin/Native and the Xcode app and executes the entire shelf unit/contract suite (including new native-session regressions). Record the final run/source after completion.
- No automated UI, screenshot or instrumentation tests were added or run.
- Pending physical-device/manual gates: camera permissions and focus/torch, real scanner model download, retake and gallery cancellation, persisted create/finish/restore/delete, light/dark and large fonts, VoiceOver reordering, public visibility and account switch, nested Home check-in camera, image authorization and back/cancel. Windows verification does not certify these gates.
- Paired light/dark SwiftUI previews beside shelf grid, bag details, form, finish sheet and camera permission content use fake snapshots and no DI/network. Hardware camera and scan animation rendering remain manual gates.
- Native shelf reuses the source-owned Russian strings; shared shelf has no Belarusian translation, so native retains the Russian fallback.

## Presentation parity and active-renderer audit

- Profile settings retain the original gear entry and a separate native visibility sheet. Load and save failures preserve their distinct Kotlin error identities. The catalogue selection badge, archived remaining-weight visibility, and finish-sheet roaster/age metadata retain their original conditions.
- The finish sheet begins at five stars in Kotlin presentation state; cancel/reopen resets it, and Skip still sends no rating/verdict. Shared ViewModels and repository writers remain unchanged. Native session regressions cover grid and detail defaults/reset, actual selected values and Skip.
- Shelf feedback uses the existing issue-specific text and five-second Undo window (extended for VoiceOver). An opaque Kotlin message identity prevents a stale native timeout from dismissing a newer undo, including a later finish of the same bag. Drops validate their actual item-provider payload before changing order or starting finish.
- All active Shelf loaders, errors, camera permissions, preparation/download/scan states, retake, duplicate confirmation, form/date picker, visibility, finish, delete, reorder and feedback render with SwiftUI/UIKit. Native archive and all shelf/coffee artwork reuse CoffeePeekCardImage(symbol: nil) and its shared CFPK light/dark placeholder; rating uses the shared brand accent.
- Remaining Compose controller boundaries are lifecycle/composition hosts: UIKitViewController hosts the native shelf root; makeBrew calls the shared Home check-in entry, which chooses the existing SwiftUI form; original PHPicker and AVFoundation controllers remain native platform media. These hosts do not render a second Shelf screen, sheet, error or loader. Shared Compose Shelf content remains compiled for Android.
- Windows parity batch: isolated feature common/iOS metadata passed; 30 tooling/resource contracts passed. Four additional native session regressions are compiled/executed by the next macOS run, not by Windows metadata. Body gate on the original base still reports only the old ownership/public-profile factory violations; no baseline extension.

- After merging integration `0fb8b2d1d`, Xcode OpenStep parsing, object uniqueness and references passed (328 objects; only the same inherited Firebase reference). Resolved native profile constructors preserve both Shelf and Collections callbacks. The body gate reports only the existing ownership factory on this base; no new/grown Shelf violation. The companion Business checkpoint removes that final factory false positive.
