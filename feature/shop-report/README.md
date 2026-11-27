# Reports about coffee shop information

Reviewed against `develop` on 2026-11-27. This feature reports inaccurate place
information; [Content Report](../content-report/README.md) handles publications.
Completed Android activation/native-form correction ledgers remain in Git.

## Ownership and entries

`api/domain/data/impl` own `ShopReportEntry`, issue categories and validation,
the existing POST contract, and MVI form. Composition supplies the configured
authenticated client, account reader, child store and navigation. Shop detail
handles guest access before emitting its `ReportShop` effect; the application
retains `ReportShop(shopId, shopTitle)` as the destination.

Android uses the shared Compose form. iOS `NativeShopReport` presents the same
Kotlin ViewModel in `ShopReportScreen.swift`; UIKit owns Back/interactive pop.
No nested runtime navigation stack, new repository or Swift-side mutation policy
is needed. Native strings live in the `ShopReport` RU/BE tables; shared resources
remain feature-owned. DTOs/backend stay inside data; domain is pure Kotlin.

## Form, session and delivery contracts

Six categories share one selectable group; no reason is selected initially.
Other requires a description with the existing Unicode-safe 500-character limit.
Selection/editing is disabled while sending. Android semantics retain a
single-choice group even though the visual selection is a check mark.

Data posts to `/api/ShopIssueReports`. One form allows one pending request.
Known failure retains correction/retry with the same draft; uncertain delivery
blocks blind repeated submissions. This is not a promise of server idempotency
across a new destination/process. Preserve cancellation explicitly.

An account-generation change rejects queued and late actions/requests, clears
the draft and cancels work. Same-account token renewal preserves it. Leaving a
native screen suspends its subscription; removing the controller closes the
owner. Root navigation owns screen analytics; the originating shop report action
keeps its existing shop event, with consent/session guards.

## Source and verification

Start with [API](api/src/commonMain), [domain](domain/src/commonMain),
[transport](data/src/commonMain) and [MVI/native session](impl/src/commonMain).
Native application/session adapters are under `composeApp/.../di/shopreport`;
SwiftUI lives in `iosXtool/CoffeePeek/Sources/CoffeePeek/ShopReportScreen.swift`.
Existing feature and application contracts cover category/Unicode validation,
retry and uncertain delivery, subscription resumption, cancellation, queued
actions across account replacement and token refresh. Check the current Native
CI selectors when adding tests; metadata compilation alone executes none.

Code changes require relevant non-UI tests, Android assembly/DEX checks and
Native CI. Manual acceptance needs both themes/languages, large text, VoiceOver/
TalkBack, keyboard/scrolling, Back and completed/cancelled edge swipe, and fake
success/error outcomes. No real report, device check or app build was performed
for this documentation update. Remaining repository migration gates belong in
the [legacy exit plan](../LEGACY_EXIT_PLAN.md).
