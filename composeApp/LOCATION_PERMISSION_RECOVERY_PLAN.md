# Location permission recovery — issue #351

Source: `codex/migration-location-permission-351`, based on develop
`8168bfed4388b8051fc0c87ca6411198e3e69ca2`, 2026-11-27.

## Scope and cause

The Android platform adapter persisted an `asked` flag and classified every
subsequent Discovery request as permanent denial, even when Android could still
present a permission prompt. Its result callback did not distinguish a soft
refusal from a denial that requires Settings. Native iOS delivered only the first
permission result and dropped the receiver, so a grant in Settings did not reach
the live request. Both shop-detail renderers read the permitted position once and
could keep distance unavailable after authorization changed.

Keep the existing platform location service, feature state machines, settings
notice and analytics callbacks. The platform adapter uses Android's rationale
signal alongside its previous-request flag. Only an explicit request key can
present a dialog; lifecycle resume only checks current authorization. Coarse
Android permission is sufficient. iOS observes authorization and foreground
changes, delivers a changed result to the current live request once, and removes
its observer on close. An unrequested/disposed source stays inert. The existing
`requestOnce` parameter is retained for caller compatibility; repeated automatic
requests remain bounded by the feature-owned request keys, while an explicit new
key can recover after a soft denial.

The Android/Compose shop distance refreshes on resume. The native shop reader
observes authorization while its screen is visible and refreshes on foreground.
Both use the same permitted read: check actual access before and after the
suspending location lookup, preserve cancellation, and clear unavailable or
revoked coordinates. No permission request, coordinate fabrication, catalog or
city fallback is introduced by these readers. Existing distance formatting,
feature APIs, DI/dependencies, routes and production analytics remain unchanged.
Discovery rechecks its existing location port after denial when a cached point
exists. Search and Map consume the resulting null position instead of preserving
an old distance origin; Nearby falls back to the existing selected-city results
and the next explicit tap requests location again. This uses the current feature
port and guards, with no new platform dependency in feature UI/domain.
The optional permanent-denial callback defaults to the existing denial callback,
so older platform consumers cannot silently swallow a terminal result.

## Verification plan and gates

Run the existing app JVM location/Discovery contracts plus the new fake platform
policy and permitted-read regressions. Cover first denial, a requestable second
tap, permanent denial/Settings, grant while inactive followed by foreground,
duplicate/stale request keys, disposal, revoke during read and cancellation.
Compile the app through `directSandbox`, require packaged runtime-class and
Sandbox isolation checks, and verify function body limits without extending the
baseline. Compile native Kotlin/iOS in CI. The coordinator granted one host Gradle slot;
no concurrent Gradle build was used.

## Local verification — 2026-11-27

On Windows/JDK 17, source base `8168bfed` plus this working patch, executed app
Direct Debug JVM contracts: 764 cases / 171 suites; Discovery impl: 211 cases /
25 suites. Total: 975 cases / 196 suites, zero failures/errors/skips. This includes
13 new fake regressions (seven permission lifecycle/policy, four permitted reads,
and two Discovery stale-origin cases). Both selected test tasks executed.

The first combined invocation stopped on one new assertion: the test expected
one reader refresh, but enabling Nearby with an existing position already issues
one refresh before denial performs its necessary recheck. The expectation was
corrected to two; production code was not changed for that failure. The original
failed run/log is retained. The final full invocation passed in 1m 37s.

Function-body limits and Direct Debug lint passed. Lint has zero errors and
153 warnings across the app/dependencies; the only report in a changed file is
the pre-existing `UseKtx` suggestion for the retained SharedPreferences edit API.
Existing compiler Preview, expect/actual and Google sign-in deprecations remain.
There is no function-body baseline, dependency or resource expansion.

Direct Sandbox assembly, packaged DEX runtime-class and Sandbox isolation checks
passed. APK: `com.coffeepeek.sandbox`, `1.2.2.3437-sandbox` / 3437, SHA-256
`878487cecaa03f409820aaa1135e997fd453ccf50f48141dac40a2b8f8174e7e`.

After the successful Android run, static native review qualified the UIKit enum
as `UIApplicationState.UIApplicationStateActive` and added the standard foreign
API opt-in. Those iOS-only source edits do not change tested Android/common code.
At published runtime head `6b89eec5985af7ae50cb2a206f729092c06421d6`, iOS CI
`45971098951` passed actual Kotlin/Swift Simulator compilation and binary
compatibility. The original core contract step also passed its selected tests.

A subsequent selector audit found that the core command did not select the new
`LocationPermissionRequestTest` and `PermittedUserLocationTest` classes, and the
Discovery command selected `SearchViewModelTest` but not `MapViewModelTest`.
Those earlier green stages prove compilation and their existing selection;
they do not prove execution of the omitted new Native regressions. The 975 JVM
cases above were executed and are unaffected by this gap.

The same existing Native commands now explicitly select both new location
classes and `MapViewModelTest`. All earlier filters are retained, with no new
workflow or runtime-code change. Native execution of the added selectors remains
a required CI gate on the follow-up head. No local Gradle rerun was used for this
YAML/documentation-only correction. Manual device behavior remains unverified.

Bounded static review covered OS permission → location reader → distance origin,
callback lifetime, disposal and cancellation (ASVS V4/V8/V11). It found no new
confirmed authorization bypass or data-exposure path in the final patch. This is
a narrow self-review, not a full independent security audit or device evidence.

No user-facing strings or visual components change, so existing RU/BE settings
resources and feature previews remain applicable. Studio tools are unavailable.
No device, real permission dialog/Settings mutation, live account, network write,
analytics event, UI/instrumented/screenshot test or preview rendering is used.
Manual Android/iOS denial → explicit retry → Settings → foreground behavior,
coarse/precise access, denied location services and real distance remain device
acceptance gates. Fake tests and compilation do not certify these device gates.

Platform references: [Android runtime permissions](https://developer.android.com/training/permissions/requesting)
and [Core Location authorization changes](https://developer.apple.com/documentation/corelocation/cllocationmanagerdelegate/locationmanagerdidchangeauthorization(_:)).
