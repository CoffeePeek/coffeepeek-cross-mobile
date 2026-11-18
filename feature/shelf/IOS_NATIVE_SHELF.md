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

Feature-owned iOS sessions adapt the existing ViewModels and own their ViewModelStore. Application composition maps native snapshots, wires navigation, authorization and the existing native Home check-in form. Swift owns rendering and observation subscriptions. Native root routes and profile embedded slots both use these entries. The iOS ShelfScreenRenderer delegates nested navigation to the same native controllers; it no longer selects the Compose shelf screens. Android keeps its existing feature entries.

The native camera reuses IosShelfCameraSession (AVFoundation focus, metering, torch, portrait capture and normalization), and gallery reuses the existing PHPicker delegate. Scanner/model readiness, preparation, original-photo processing, moderation and processed-card uploads remain in Kotlin. Existing package runtime choices are preserved. Photo selection/cancellation does not dismiss the shelf destination. Photos selected after a revoked owner session are ignored. Public cards use the anonymous bounded photo repository; private cards use the existing authorized media loader. Closing clears the ViewModelStore, camera and pending work.

The existing form edits optional bag metadata before creation; there is no existing edit-an-already-saved-bag UI to retire. No additional update writer or business state was introduced.

## Verification

- Plan: verify native handoff/finish/undo, scanner download/photo continuity, duplicate writes while saving, public read-only/hidden projection, cancellation and existing ViewModel contracts.
- 2026-11-18, Windows host: `python -m unittest discover -s tools/ios -p 'test_*.py'` — 29 passed. `git diff --check` — passed. Isolated `:feature:shelf:impl:compileIosMainKotlinMetadata` — passed (common + iOS feature bridge metadata).
- Full application metadata on Windows stopped at the pre-existing generated Room expect/actual collision in core/database, before application compile. Body-limit validation reports only the two existing ownership/public-profile violations; this checkpoint adds none and does not change the baseline.
- macOS run `44264106089` at `812ab4c4a`: Kotlin/Native framework embedding passed; Xcode stopped at Swift optional inference in the weight choices. Constrained the choices to `[Int]` and retained the hosting model for the UIKit controller lifetime; the next run verifies this correction.
- macOS `ios-verify` builds both Kotlin/Native and the Xcode app and executes the entire shelf unit/contract suite (including new native-session regressions). Record the final run/source after completion.
- No automated UI, screenshot or instrumentation tests were added or run.
- Pending physical-device/manual gates: camera permissions and focus/torch, real scanner model download, retake and gallery cancellation, persisted create/finish/restore/delete, light/dark and large fonts, VoiceOver reordering, public visibility and account switch, nested Home check-in camera, image authorization and back/cancel. Windows verification does not certify these gates.
- Paired light/dark SwiftUI previews beside shelf grid, bag details, form, finish sheet and camera permission content use fake snapshots and no DI/network. Hardware camera and scan animation rendering remain manual gates.
- Native shelf reuses the source-owned Russian strings; shared shelf has no Belarusian translation, so native retains the Russian fallback.
