# Bottom navigation long-press recovery

## Scope and verification plan — 2026-11-27

The owner requested fixes and ordered merges of PRs 415, 416, 417, 430 and 431.
PR 431 was previously merged into the subsequently closed/deleted 430 branch,
so that merge did not deliver its navigation behavior to develop. Recovery keeps
the original history, reverts the navigation delta in 430, then reapplies only
that delta in a separate final PR after 430. No forced history rewrite is used.

Remove the obsolete large-content viewer around Android floating navigation
items and its last design-system implementation. A held tab retains its existing
press highlight without showing a magnifier. Existing click/navigation callbacks,
analytics, selected state and native iOS UITabBarController remain the owners of
their existing behavior. This removes root UI; it introduces no new business UI,
resource, API, dependency, account policy or platform fallback.

Verification plan: compile the existing colocated fake light/dark bottom-bar
previews; execute application Direct Debug and design-system unit contracts;
require function-body limits, Direct Sandbox assembly, packaged runtime classes
and Sandbox isolation. Check that no Kotlin consumer of the deleted viewer
remains. Fresh Android/iOS CI validates the final immutable source head before
its ordered merge. No UI/instrumented/screenshot tests are added or run.

No Android device was selected and no app is installed/launched. Manual normal
and large-font light/dark review, long press/release, tab navigation, accessibility
and glass appearance remain unverified. Fake unit contracts or APK assembly do
not certify these visual/device behaviors or live analytics.

## Local result — 2026-11-27

On source 4966333fe plus this plan (Windows/JDK 17), both selected JVM tasks
executed: application 753 cases/169 suites and design system 82 cases/22 suites,
835 cases total, zero failures/errors/skips. Function limits passed (125 recorded,
zero new), with no baseline change. Direct Sandbox assembly, packaged DEX runtime
classes and isolation passed. The artifact is com.coffeepeek.sandbox,
1.2.2.3431-sandbox / 3431. Kotlin search found no remaining viewer consumer.

Colocated fake previews compiled but were not rendered. Existing compiler
deprecation warnings remain. No device, live account or UI/instrumented test ran;
the manual gates above remain open. Fresh Android/iOS CI on the published head
is separate from these successful local checks.
