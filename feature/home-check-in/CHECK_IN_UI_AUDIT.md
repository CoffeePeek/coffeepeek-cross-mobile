# Check-in presentation audit — 2026-11-15

Source: `codex/migration-ios-checkin-layering`, based on `1affc4512` plus the
changes in this checkpoint. Its application sources match fresh `origin/main`
`bbc91a035` and `origin/develop` `2bae86f80`; the differences between those refs
are release automation. The reporter identified the installed iPhone app as
the latest version, without an exact build number. The screenshots establish
the defects, but do not establish the binary's source revision.

Verification plan: trace both editors and their actual entry points, fix shared
UIKit rendering/insets and the Home date-picker call, preserve form state and
submission contracts, run the affected unit suites and Android assembly guards,
and compile the affected iOS libraries. Native appearance, scrolling and keyboard
transitions require subsequent iPhone acceptance; compilation cannot verify them.

## Findings and implementation

| Reported behavior | Source finding | Change |
| --- | --- | --- |
| Discovery's shop heading/cards show through the note | The UIKit text view has a 4% alpha fill in the Compose interop cutout | Composite that fill over the opaque theme background and wrap the rounded input in an opaque rectangular native host |
| Enabled switch is brown | `UISwitch.onTintColor` uses a dark thumb/track contrast token | Use the existing yellow `CoffeePeekColors.Primary` in both themes |
| Material calendar appears on iPhone | Home calls Material `DatePickerDialog` directly | Use `CoffeePeekDatePickerDialog`, which delegates to the installed native `UIDatePicker` presenter on iOS; Shop already uses this contract |
| Publication controls float above the keyboard | The iOS dialog consumes/resizes for IME before the Home editor observes it; Shop always renders its save bar | Disable the dialog's software-keyboard inset, reserve only system bars/cutouts in the Home host, and let forms own IME padding; hide publication controls while typing in both editors |
| Blue Done accessory exposes the screen underneath | The native keyboard toolbar uses default translucent appearance/tint | Give standard, compact and scroll-edge toolbar appearances an opaque theme background and readable foreground; keep numeric accessories attached to their own Compose controller |
| Native picker can open while a UIKit field still edits | Compose focus dismissal does not itself guarantee that a UIKit responder resigns | End native editing before presenting the next native controller |

The keyboard Done action remains available to dismiss a multiline/numeric
keyboard. It is not a publication action. Its native glyph/shape can differ
between iOS versions; the theme and opaque backing are explicit.

Dates continue to represent civil days at UTC midnight. The maximum is the end
of today; selecting the existing date leaves its stored instant unchanged.
The shared Material fallback no longer rejects epoch/pre-epoch dates that the
Home picker previously accepted. No feature-specific minimum date was added.

## Source audit coverage

This table records code inspection, not successful device scenarios.

| Flow or area | Reviewed behavior |
| --- | --- |
| Home creation from the global check-in flow | Uses `HomeCheckInFullScreenDialog` and the migrated editor; opaque surface and safe-edge ownership are shared |
| Home creation from a shelf bag | Reuses the same host/editor, including the selected bag and draft actions |
| Home edit from history | Reuses the same host/editor; existing entry identity, original instant and dismissal guard remain intact |
| Cafe check-in | Migrated Shop form uses the same native note/switch primitives; save bar now follows IME visibility |
| Date selection | Home and Shop delegate to the shared platform presenter; cancel/confirm actions and future-date bounds are preserved |
| Notes and recipe fields | Native text view retains selection, marked-text handling, UTF-16 limits and dynamic fonts; normal/numeric inputs retain existing keyboard actions |
| Photos, bean selection, rating and errors | Existing feature components, scroll containers and actions are retained; no photo/network writes were performed |
| Visibility, save, retry and close | ViewModels, repositories, session guards and persistence contracts are unchanged |
| Analytics | Home open/field/bean/photo/save/cancel metrics, Shop field changes and application `AnalyticsOverlay`/consent adapters retain their existing owners; no test events were sent |

No modules, dependencies, source-set boundaries, DTOs or domain contracts were
introduced. The shared native toolbar appearance helper lives in design-system;
the application remains the adapter for numeric keyboard presentation.
Existing paired light/dark previews for notes, switches, date pickers and editor
contents remain colocated. They do not render native UIKit on this Windows host.

## Verification evidence

Windows verification uses sequential Gradle runs with `--max-workers=6`.

| Executed unit suite | Suites | Tests | Failures / errors / skips |
| --- | ---: | ---: | ---: |
| Design-system | 21 | 81 | 0 / 0 / 0 |
| Home check-in impl | 4 | 81 | 0 / 0 / 0 |
| Shop impl | 10 | 108 | 0 / 0 / 0 |
| Application Direct Debug | 151 | 673 | 0 / 0 / 0 |
| Total | 186 | 943 | 0 / 0 / 0 |

The first combined run executed the three library suites but failed the iOS
design-system compilation: the new `UIView.endEditing` call lacked its
Kotlin/Native extension import. After adding that import, the second run passed
in 1m11s; it reused the three successful suites and executed the application
suite. There were no test failures or test retries.

Direct Sandbox and Direct Debug APK assembly, both packaged DEX runtime-class
checks, Sandbox isolation and function-body limits passed (136 recorded,
zero new violations). Built artifacts are `com.coffeepeek.sandbox`
`1.2.0.3004-sandbox` and `com.coffeepeek.debug` `1.2.0.3004-debug`, code 3004.
Neither APK was installed or launched. The final host-inset adjustment is
verified by another successful application build/test run (1m6s), including
all 673 application tests, both APK/DEX guards and Sandbox isolation.
`git diff --check` also passes.

Design-system, Home impl and Shop impl compiled for iOS Arm64. The application
iOS task is **skipped** on Windows: its transitive SwiftPM cinterop cannot be
processed on `mingw_x64`. Thus the application-side native accessory edit still
requires macOS compilation. No Xcode link, simulator run or device run is claimed.

The first failure, retry and final verification logs are kept locally outside
tracked sources as `ios-checkin-ui-verification*.log`. Existing preview
deprecation and unrelated compiler opt-in warnings remain. No automated UI,
instrumented Compose or screenshot tests were added or run. Studio diagnostics
and preview rendering were unavailable. No device was selected.

## Remaining acceptance gates

On an iPhone build containing this checkpoint, manually verify Home creation
from Discovery and shelf, history editing and cafe creation:

1. Light/dark themes, normal/large text and narrow/landscape layouts. Scroll the
   note through clipping boundaries: no shop heading/cards should appear inside
   the field, its corners or keyboard accessory.
2. Open/close keyboards from drink, note and numeric recipe fields; scroll to
   the focused input. Publication controls must disappear while typing and
   return once, with draft and visibility preserved, after Done/dismissal.
3. Open the date picker from an active input: system iOS picker, keyboard
   dismissed, cancel retains the date, confirm updates it, future days blocked.
4. Photo-source menus and bean picker, cancelled back/close, validation and
   retry layouts; VoiceOver focus/labels and minimum touch targets. Use safe
   synthetic data, not real-account writes.
5. Check the other shared-dialog callers (photo viewer/onboarding) and numeric
   accessories for unintended inset or placement changes.

Android Direct Sandbox has the equivalent keyboard/footer/date regression gate.
Physical-device visuals, full iOS linking and live-backend/release acceptance
remain unverified at this checkpoint.
