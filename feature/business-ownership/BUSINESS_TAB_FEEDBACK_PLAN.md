# Business tab feedback

Base: develop `c41003e6` (discovery PR #213). Discovery overlay PR #214 is
independent. This slice changes the shared Android/iOS business tab and the
ownership overview only.

## Behaviour and boundaries

- Preserve owner session isolation, business/profile navigation, claims and
  editor save/discard rules. Remove the Business tab's explicit Edit, Refresh,
  My check-ins and activity preview controls. Keep edit actions on business
  profiles.
- Use the design-system pull gesture for refresh, including its accessibility
  action. The ownership overview also drops its manual Refresh button.
- Show owned shops and roasters as a vertical list using the existing catalog
  card designs. Hide the roaster section if there are no roasters. Owner list
  endpoints only provide ID, name and slug, so owned profile reads supply
  available cover, address and description. Missing rating, open state and
  catalog counts are omitted rather than guessed. Limit profile read
  concurrency and reject replies after an account change.

## Verification

Run ownership presentation unit tests and affected app tests. Compile the
ownership/design-system modules, Android Direct Debug and Direct Sandbox APKs
with packaged runtime and Sandbox isolation checks, and Kotlin iOS device and
simulator sources. PR CI executes native Swift/Xcode checks. No automated UI,
instrumentation or screenshot tests. Manual Sandbox/iOS light/dark, font size,
accessibility, pull, card tap, empty-roaster and navigation checks remain a
separate device gate. No install, real-account write or backend mutation.

## Results — 2026-10-29

Ownership presentation: 18 unit tests passed, including card details and
account-switch rejection. Application: 856 unit/contract tests passed. No
failures, errors or skips. Android Direct Debug and Direct Sandbox APKs passed
packaged runtime checks, and Sandbox isolation passed. Kotlin iOS Arm64 and
simulator production plus simulator test sources compiled. The final source
change preserving cached card images on refresh was followed by a successful
ownership test rerun, both Android APK assemblies and both iOS production
compilations. The first test attempt stopped before compilation because this
new worktree had no local Android SDK path; adding only its ignored `sdk.dir`
setting resolved that environment issue. No application was installed or run.
Native Swift/Xcode and device visual acceptance remain separate PR/device
gates.

## Mode switch integration — PR #417, 2026-11-27

This checkpoint preserves the current ownership boundary while integrating the
older mode-switch fix with develop 36fd1f38a. Shared Kotlin owns membership reads
and the mode flag; Android composition owns its existing NavHost. The native
application starts IosNativeRoot directly, so the retired Compose native-root
implementation is not restored.

Verification plan before the follow-up edits: keep the Android graph start
stable across mode recomposition; serialize refresh and switch operations; retain
cached grants when returning to personal mode, guest denial, account-change
rejection and coroutine cancellation. Run Ownership impl unit tests and the
application NativeRootNavigationModel contracts, function-body verification and
Direct Sandbox assembly with packaged runtime/isolation checks. Root coordinates
all Gradle work. No automated UI/instrumented tests, device launch or account
writes are part of this checkpoint; manual mode-switch/back/app-link review and
macOS native compilation remain separate gates.

Entry trace: BusinessOwnershipEntryImpl.Session/refreshSession and
switchAccountMode delegate to one OwnershipAccess. Android's
BusinessModeNavigationEffect navigates the existing graph; its initial start is
remembered by a small cohesive helper without growing the frozen MainScreen
body. On native iOS, a mode change replaces the IosNativeRoot owner/model and
revokes pending navigation acknowledgements. NativeRootController's create tab
is rejected by shouldSelect; selected callbacks additionally require the current
generation, readiness, no pending command and membership in visibleTabs. Thus
the original create-slot-as-navigation crash path is already retired. The native
bootstrap guard and develop's function baseline are preserved verbatim.

Review scope is Kotlin Compose/KMP and the existing Swift/UIKit routing boundary,
with account/ownership inputs mapped to ASVS V3/V4/V11. No authentication, server
permission, TLS, persistence, analytics or public entry contract is changed.
Read-only review found no new security finding in this slice. Four new non-UI
contracts cover queued/duplicate/cancelled mode switches and visible native-root
policy across mode/guest changes. They are authored here and await root-run
verification; source inspection is not execution evidence.
