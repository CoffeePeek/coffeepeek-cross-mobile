# SwiftUI check-in creation

The center + button keeps the existing Kotlin auth gate. In an authenticated
session its entry sheet now renders the coffee shop, home and shelf bag choices
in SwiftUI. The existing GlobalCheckInFlow owns the active session, analytics,
cancellation and one-time outcome delivery. UIKit presents the sheet and
reports visibility and interactive dismissal to that flow.

The Home editor now has a native presentation entry behind the same
`HomeCheckInEditorEntry`. Android keeps its existing Compose screen. On iOS the
application composition creates `NativeHomeCheckInSession`, whose existing
`HomeCheckInEditorViewModel` remains the sole owner of load, validation, search,
editing, save, analytics and confirmation. The Swift surface receives copied
presentation values through `IosHomeCheckInEditor`; its subscription does not
own or destroy the form when the camera covers it. Composition disposes the
session only when the entry leaves composition.

Entry coverage is the global create flow (Home or shelf bag), the shelf's brew
action, and existing Home edit consumers. `preparationId == null` is create;
any non-null ID is edit. Shop creation remains its own session and feature; a
Shop editor route does not imply an API for editing an existing Shop record.
The shop search with MapLibre and full history are separate migration slices.
No new writer, network client, auth flow or image pipeline is introduced.

The native Home API preserves the owner lease, server timestamps when selecting
the same civil date, nullable publication state and explicit saved confirmation.
Photo delivery is request-scoped, bounded, copied, and discarded after closing
or replacing the owner. The existing platform picker handles camera/gallery and
compression; edit still forbids changing server photos. Private shelf images
use the existing authorized fetcher under a cancellable account lease. The
completed Home editor refreshes the existing feed through its existing callback.

The iOS Kotlin tests cover all three options, single delivery, stale sheet
dismissal after selecting home, and session replacement. The local iOS tooling
and signing tests, launcher labels, Xcode targets and RU/BE keys were checked.
On an iPhone, verify the guest auth gate, each choice, VoiceOver, extra-large
text and swipe dismissal in both themes with test data.

## Native editor verification (2026-11-17, working tree after 11535611b)

Added non-UI Home session contracts for local-day projection and field rules,
unchanged edit timestamp and unknown visibility, owned/bounded/single-use photo
delivery, picker cancellation and stale callbacks, submit blocking while photos
are pending, owner revocation, one saved outcome after confirmation, and detail
request cancellation on owner disposal. The coordinating run executed Home88,
Shop112 and composition681 JVM unit/contract tests with zero failures/errors/skips;
Profile72 was UP-TO-DATE. Android Debug/packaged DEX checks and Windows iOS
Debug1.2.1(3087) assembly passed at source `c651f529d`. iOS-only tests remain
authored but unexecuted on Windows. See `docs/IOS_SWIFTUI_EXECUTION_PLAN.md` for
the retained failed attempts, inherited body-limit violations and device gates.

Manual gates remain: create/edit and shelf entry placement in both themes,
native date and bean selection, keyboard/large text/VoiceOver, cancelled photo
picker and camera round trip, back/cancel and account replacement. Opening a
real-account form is read-only acceptance only; persistence and analytics writes
require an explicitly authorized safe account and are not certified by previews.

## Photo source anchor and recipe disclosure — 2026-11-25

Source: `dce8989e5` plus `codex/migration-ios-shop-filter-parity` working changes.
The audited Home editor attached its photo confirmation dialog to the whole photo
field, so the system could anchor the menu away from the camera button. The shared
attachment grid now attaches the system dialog directly to that button. Shop's
editor uses the same correction. The grid owns only transient dialog state; owners
still supply localized choices and receive the camera/gallery selection. Menu state
is cleared when photo identities, editability or add availability change, and source
selection rechecks availability. Dialog tint uses the adaptive foreground to avoid
yellow labels on a pale system background. System size-class adaptation is retained;
no coordinates, delayed task or custom popover presentation was added.

Home's Additional section retains the system automatic DisclosureGroup, its native
indicator/accessibility and existing initial/error expansion rules. A 250 ms layout
animation with content fade follows effective expansion, including validation-driven
opening, and is disabled for Reduce Motion. The label fills its available width.
No draft, validation, permission, upload, analytics or photo request/session ownership
changed. Screen/exposure and creation-step analytics remain in the existing owners;
no new analytics events, Swift tasks or isolation escapes were introduced.

Design references: [Apple HIG popovers](https://developer.apple.com/design/human-interface-guidelines/popovers)
requires a relationship to the invoking control; [HIG disclosure controls](https://developer.apple.com/design/human-interface-guidelines/disclosure-controls)
identifies SwiftUI DisclosureGroup as the platform control. The existing system
[confirmation dialog](https://developer.apple.com/documentation/swiftui/view/confirmationDialog(_:isPresented:titleVisibility:actions:))
keeps automatic dismissal and platform presentation behavior.

Verification plan: preserve five-photo limits, create/edit restrictions, picker
cancellation, stale delivery/account fencing and error-driven recipe opening. All
93 Home unit tests in 6 suites and 112 Shop tests in 11 suites executed successfully,
with zero failures, errors, skips or retries. Android `directDebug`
(`com.coffeepeek.debug`, `1.2.0.3221-debug`, code 3221) assembly and DEX runtime-class
checks passed with `--max-workers=6`. Function limits reported 130 recorded and zero
new violations. Swift 6.4.2 syntax parsing with strict-concurrency flags and
`git diff --check` passed; parsing does not prove SwiftUI type checking or concurrency
diagnostics. No automated UI/instrumentation tests were added or run.

Colocated previews cover scrolled/wrapped photo tiles in both themes and large text,
plus interactive collapsed/error recipe disclosure in both themes and large text.
Reduce Motion requires device review; its EnvironmentValues entry is read-only.
They were not rendered on Windows. No device was selected or app
installed, and no live account, gallery, camera or backend writes occurred. Full
native compilation remains open after the previously recorded application Room
metadata failure. Manual iPhone/iPad gates include menu anchoring before/after scroll,
keyboard and rotation/window changes, photo limit/edit/preparing/revoked states,
cancel/camera/gallery round trips, VoiceOver and disclosure motion/error reveal.

## Startup onboarding actions — 2026-11-25

The user reported that all startup onboarding actions were unresponsive in Debug
3221. The prior device check established process startup only; it did not cover
these actions. The native controller used `.fullScreen`, which removes its
presenting view after presentation. Dismissal and navigation are driven by the
presenting Compose composition, so removing that host can prevent the state change
from dismissing the native controller. Use `.overFullScreen`, as the existing root
update/global-dialog controllers do, to keep the composition attached. UIKit
documents this difference in its
[full-screen presentation guide](https://developer.apple.com/library/archive/featuredarticles/ViewControllerPGforiPhoneOS/PresentingaViewController.html).
This is a source-grounded diagnosis pending reproduction on the updated device.

The shared native action-button label now defines a content shape over its full
layout, including the padded width. Native onboarding page dots are buttons with
44-point targets, retaining the single adjustable accessibility element and Reduce
Motion policy. GestureState resets press/hold on cancellation; rotation remains
in the existing lifecycle-owned `.task(id:)`. No unstructured tasks, isolation
escapes, navigation ownership or session-rule changes were introduced. The iOS
presentation uses the existing AnalyticsOverlay guard so that the retained,
covered catalogue cannot accrue background feature exposures. No new analytics
events or service were introduced; consent/session checks remain in their owners.

Verification plan: build/sign scanner-disabled Debug 3222, retain the previous IPA,
run the existing non-UI iOS tooling contracts and Android assembly/DEX/body checks.
After installation on the agreed USB iPhone, manually check Register and Sign In
open the expected forms, Back returns normally, and guest close reveals Discovery;
relaunch between independent startup choices without clearing app data. Also check
dot navigation, swipe/hold cancellation, light/dark, large text and VoiceOver.
Do not submit forms or modify a real account. Tests and device acceptance below
must distinguish compilation/startup from these interaction checks.

Executed preflight: all 32 existing iOS tooling tests passed, including both native
factory-registration checks. Android `:composeApp:assembleDirectDebug` and packaged
DEX verification passed; function limits report 130 recorded and zero new violations.
Gradle used `--max-workers=6`, with no installation or Android run. The first Android
invocation selected an unavailable SDK path; the retry used the already configured
SDK and passed. Both logs are retained locally. `git diff --check` passed.

After adding the existing overlay guard, all 58 analytics unit tests in 12 suites
executed successfully, with zero failures, errors or skips. They use fake/in-memory
services. Android assembly/DEX and the 130-recorded/zero-new function check passed
again in that invocation. Pre-existing Discovery adapter test opt-in warnings remain.
The first full native build before this guard passed (packages 1m34s, Kotlin 19.5s,
framework link 2.5s, Swift runner 2m59s, assembly 1.3s); final compilation/signing and
physical interaction acceptance must cover the guard as well.

The final native pass includes the iOS overlay guard: Swift packages completed in
28.4s, Kotlin compilation in 1m04s and the changed framework linked in 13m34s.
Swift runner compilation passed in 3m16s and assembly in 1.9s. Cached development
signing and IPA integrity/hash checks passed for Debug `1.2.2 (3222)`,
`com.coffeepeek.xcross`. The signed source manifest covers 28 working-tree source
files on top of `dce8989e58b109f0c6e9bfbf31b6713430eba8a7`. IPA SHA-256:
`e2a44cc512290a702871baf8be83cefb255a6ea2f86a6cd7e1b6d7431b7f5deb`.
Build 3221's signed artifact is retained separately.

Final tooling retry: all 32 tests passed after restoring the temporary package
manifest. The first final invocation had three Git Bash resource-script failures
because the Windows sandbox denied MSYS named-object creation (`0xC0000022`);
the reviewed retry with the required host access passed. This initial failure is
retained alongside the successful result rather than reported as a clean first run.
The manifest is restored and `git diff --check` passes.

The selected USB iPhone was unavailable after signing. Installation, Register,
Sign In, guest close and dot/gesture device checks are pending reconnection. Full
compilation/signing does not establish that the user-reported interaction failure
is resolved on the device. Native language-mode and remaining manual gates are
the same as the Debug 3221 record in the shared execution plan.

After the user reconnected/unlocked the phone, Windows PnP detected it while
usbmux initially returned an empty list. Opening the already installed Apple
Devices application restored USB access. InstallationProxy then confirmed an
in-place update from `1.2.2 (3221)` to `1.2.2 (3222)` on `iPhone15,4`, iOS `27.0.1`,
using the same signing identity. The separate main application version remained
unchanged. The USB endpoint disappeared again before CoreDevice launch; three
subsequent samples still returned no matching device despite healthy PnP entries.
Thus 3222 is installed, but startup and onboarding interaction acceptance remain
open. No phone security/settings or account data were changed.

## Onboarding visual follow-up — 2026-11-25, Debug 3223

User requirements: Liquid Glass close control above the artwork, compact adjacent
carousel dots, and automatic advance every three seconds with press/hold pause.
The close control reuses the existing native glass-circle modifier, with system
material on older iOS. It overlays the carousel inside the illustration margins.
Illustrations now retain their full aspect ratio at the available width and scroll
vertically with their captions when needed; the previous 72%-height box no longer
leaves the close control outside the image. The carousel is centered up to 480 pt.

The page indicator uses a native UIPageControl with intrinsic compact width and a
44-point-high interaction area. Its main-actor coordinator only updates the existing
page binding. It retains native adjustable accessibility and the existing localized
step label. Three-second rotation stays in the lifecycle-owned `.task(id:)`; holding
the artwork cancels the delay, release starts a new three-second delay, cancellation
resets GestureState, and background/VoiceOver pause automatic rotation. Reduce Motion
disables the page transition animation. No Timer, detached/unstructured Swift task,
unchecked Sendable or nonisolated escape was introduced.

Verification plan: full native Debug 3223 build/sign, preserve the 3222 artifact,
then verify the glass/image stacking, compact indicator, three-second cadence,
hold/release, swipe/dot selection and all three exit actions on the agreed iPhone.
Colocated light/dark, compact-window and large-text fake previews are present;
Windows cannot render them. No automated UI or screenshot tests are introduced.
Android sources are unchanged from the recorded passing assembly/DEX checks.

The intermediate 3223 compiled successfully. Its onboarding changes are included
in the final combined Debug 3224, which compiled, linked, signed and installed in
place on the agreed iPhone15,4 / iOS 27.0.1 on 2026-11-25. The same app process
survived the 12-second startup observation. A manual tap on onboarding Sign In
opened the native login form. The startup capture shows the glass close control
above the illustration and compact adjacent dots. These observations do not certify
Register, guest close, exact three-second cadence, hold cancellation, Reduce Motion,
VoiceOver, dark mode or large text. Further phone control stopped when the user
started entering credentials; no agent-submitted login or account write occurred.

## Caption and editor follow-up — 2026-11-25, Debug 3225

Verification scope: preserve onboarding hold/rotation and exit actions, auth form
bindings, editor drafts/submission, photo authorization/expiry and rating semantics.
Check affected JVM/non-UI contracts, Android assembly/DEX/function limits, both
native factory registrations and the complete scanner-disabled native build. Sign
and install in place on the agreed iPhone when ready; manual acceptance covers full
captions, auth transitions, remote photos, stars and editor safe areas/keyboard.
No real form submission or automated UI/screenshot tests are authorized by this plan.

The slide measures its complete title/description before fitting the illustration
into the remaining viewport. Large captions retain natural height and can scroll;
the close overlay, page-control placement and lifecycle-owned rotation remain.
Both check-in editors reuse the existing glass-circle close modifier and place
their footer in a bottom safe-area inset. The home header uses symmetric eight-point
vertical padding. The shop form omits its empty warning stack so it cannot create
an otherwise unexplained 24-point gap above the first field. Existing paired fake
previews cover both forms; caption, full/fractional/rounded rating previews are
colocated with their existing Xcode-registered owners. No project/dependency or
new Swift Task/actor-isolation escape was needed.

Preflight 3225: 687 application JVM tests in 153 suites executed successfully,
including both new auth-route replacement tests. Media 71 tests / 10 suites were
restored FROM-CACHE; Shop 112 / 11, Home 93 / 6 and Auth 39 / 4 were UP-TO-DATE.
All reported zero failures, errors or skips. Android directDebug assembled and
the packaged DEX runtime-class check passed. All 39 existing iOS Python tooling
tests passed on the first invocation (1.409s). Final changed Swift syntax passed
Swift 6 / complete strict-concurrency parsing; full native compilation is a
separate gate. No previews or automated UI tests were executed on Windows.

Final combined native Debug 3225 compiled, linked and signed successfully; the
52-file source manifest matches the working tree. It also includes the MainActor
UIKit dismissal adapter: onboarding completes its modal animation before calling
the existing Kotlin finish action. This preserves the model/analytics-overlay
lifetime and guards repeated taps without unstructured Swift concurrency. See the
shared execution plan for stage timings and signed artifact hashes. The USB phone
was unavailable at installation; captions, transitions, editor spacing/glass and
keyboard/accessibility remain physical-device gates.

## Native presentation lifetime — 2026-11-25

Home create/history edit now bypass the Android full-screen Dialog. Their existing
native editor controller is presented full-screen with native safe areas and the
same shared session/events/dismissAllowed policy. Shelf brew uses IosNativeHomeFlow
directly, retaining GlobalCheckInFlow's lease, analytics and accepted-save handoff.
Closing cancels observations, Photos/Camera preparation and the feature store and
releases the native controller reference; temporary modal coverage keeps the draft.

The plain native photo launcher shares the existing JPEG pipeline. Gallery
provider callbacks suspend in a cancellable coroutine; decoding runs off the main
dispatcher, delivery returns to the owner and late results cannot be retargeted.
No GCD handoff or unstructured Swift task was added. Seven native Home session
contracts and 31 application modal/global-flow contracts executed on the JVM with
no failures. Native device safe-area/keyboard/Photos acceptance remains pending.
