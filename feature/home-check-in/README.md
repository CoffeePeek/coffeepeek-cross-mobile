# Home preparations — incremental migration

Latest checkpoint, 2026-10-17: public Android editor entry/factory and app-owned
catalogue/media/date/picker/extraction/analytics adapters are prepared. The entry
owns a form store, one-shot picker deliveries and account-scoped outcomes.
One existing consent-aware tracker is reused; no raw private content enters its
typed metrics. App api/impl dependencies and DEX guards now cover these classes,
but **no active route, Koin or writer binding has switched**. Next is prepared
data/DI activation and replacement of the history compatibility bridge. The
historical sections below describe earlier slices; use MIGRATION_PLAN.md for
latest evidence and remaining physical-device/live acceptance gates.
This slice passes **36/36 isolated Home UI cases** (7 new public-entry scenarios)
and **1017 selected unit executions**, both app APK/expanded DEX checks, API/impl
lint and shared Native compilation. Studio still needs Gradle Sync to import the
new modules/dependencies; these results are compiler/lint/device-fixture checks,
not a clean IDE inspection or real-account/OS-picker acceptance.

2026-10-16: the pure read domain is integrated into combined Android history in
PR #95. All three Android history routes use the migrated combined entry; an
account-owned application bridge reuses the existing Home editor/detail/writer.
This does not migrate or replace that editor/writer. Shared/iOS remains legacy.

2026-10-17: domain now includes write inputs, typed failures and validation; a
populated data module implements read/detail/create/update/delete and signed
photo uploads. It is **not wired into the application**. The old Home writer,
shared/iOS UI and profile Activity bindings remain active and unchanged.
Foundation verification passes 37 Home unit cases, both app APK/DEX checks,
domain/data lint and shared Native compilation; live/visual acceptance is pending.
See [MIGRATION_PLAN.md](MIGRATION_PLAN.md) for source audit, module gates and the
remaining api/impl, MVI, parity and integration work. No placeholder module exists.

The detail-MVI slice on 2026-10-17 populated impl with guarded
detail loading, explicit deletion confirmation, typed failures, cancellation,
account revocation and scoped one-off events. It depends on own domain and core
presentation, never data or the app. At that checkpoint it had no renderer;
the subsequent Android renderers are described below. Public editor composition
and physical-device visual parity remain pending, including Pixel 7 checks.
All 24 detail-MVI cases pass, with 944 selected unit executions overall, both
APK/DEX checks, impl lint and shared Native compilation. No renderer or binding
changed in that MVI slice; those checks are not live-write or physical-device acceptance.

## Module gate and migration boundary

The editor-MVI slice prepared shared logic and internal catalogue/photo ports:
guarded edit loading, optional form fields, incremental error correction,
debounced bean search, first-photo selection/skip/retry and explicit create/update.
Actual account replacement erases private work; a token error for the same owner
keeps the draft. Saved outcomes carry only ID/notice metadata, not private photos
or notes. No Android editor renderer, analytics adapter or active binding is
introduced in that logic-only slice; current app/iOS flows remain unchanged.
Verification and remaining factory/device gates are in the latest migration-plan section.
All 43 editor cases and the expanded 987-unit selection pass, with both app APK/
DEX checks, impl lint, instrumentation APK assembly and Native compatibility
compilation. No renderer changed; UI tests were compiled but not executed anew,
and physical-device/editor visual acceptance remains open.

The Android detail slice now adds a populated API and HomeCheckInApiImpl with a
lifecycle adapter, stateless body, own components/resources and same-file paired
previews. It uses caller-supplied image/date/method ports and preserves the current
detail geometry. Shared MVI retains Native compilation; Android UI/design-system
dependencies remain in androidMain. Editor and application activation are still
pending at that detail checkpoint. The API contains only the intentionally public detail entry, not
domain re-exports, ViewModels, transport or DI; its module prevents those internals
from becoming a caller dependency. See the newest plan section for verification.
That detail checkpoint passed 944 selected unit executions, 12 self-targeted UI tests
on a read-only API 37 emulator, both app APK/DEX checks, API/impl lint and shared
Native compilation. Two initial dialog-layout synchronization failures were
fixed without weakening assertions; the full UI suite was rerun. Fixture light/
dark screenshots were inspected locally, not certified against a physical phone.

The subsequent Android editor slice adds internal lifecycle/runtime and stateless
form content, own component files/resources and colocated light/dark previews.
It retains the current field order, timeline, rating, outlines, errors and compact
spacing. Date/method formatting, real artwork/decoding and picker launch remain
explicit composition ports; fixtures use icons/boxes rather than production
images. No public editor factory, live writer, analytics flow or Android/iOS
application binding switches in this slice. The next step is real entry/adapter
composition, then activation of existing Android entrances. See the newest plan
section for executed verification and outstanding keyboard/modal/device gates.
Latest checks pass **29/29 self-targeted UI cases** (17 editor + 12 detail) on a
read-only API 37 emulator, **987 selected unit executions**, both app APK/DEX
guards, impl lint and Native compatibility compilation. Initial fixture/selector/
synchronization failures, corrected strict reruns and final APK identity are
recorded in MIGRATION_PLAN.md. Light/dark brand-font layout captures were checked
with fake media; they are not real-phone, protected-photo or IME acceptance.

`domain` owns preparation models, write inputs, field validation/typed failures,
civil-date bounds, page metadata and read/write contracts. Dependencies: Kotlin only. Allowed consumers:
Home data/presentation and application composition. Other features must not
import this domain: history owns its own read port/models, adapted by the app.
HTTP, DTOs, signed-upload representation, repository implementations and DI stay
outside domain; its upload input is just caller-supplied original/cutout bytes.

A package inside `features/home-checkin` would also expose that artifact's Ktor
dependencies and mixed read/write implementation. A separate populated module
enforces the independent boundary. No empty api/data/impl modules are created.
The existing legacy artifact and all iOS/editor/writer consumers remain intact.
Prepared data depends on this domain, core/network and Ktor/serialization; only
application composition may call its repository factory. Implementation and DTOs
are internal, and the factory requires distinct gateway/upload clients. The caller
must supply a credential-free upload client and an account-bound access lease.
No new Room cache, Koin module or client lifecycle is created.

Current path: legacy HomeCheckInRepository → Android app compatibility reader →
new Home domain → app history adapter → history-owned port. The Android history
account lease must wrap the final port before presentation consumes it. Resolving
DI does not open a lease or make a request. There is no new client or endpoint.

`preparedOn` stays a civil date distinct from `preparedAtUtc`/`createdAtUtc`.
The compatibility reader forwards the existing `from`/`to` date strings unchanged,
including equal day bounds and first/last calendar days; it does not substitute
Shop UTC-exclusive bounds. This preserves the audited client's convention. The
backend source audit pinned in check-in-history/COMBINED_HISTORY_PLAN.md confirms
inclusive civil bounds; the deployed server still needs safe date-edge QA.
Unknown visibility stays null and unknown
moderation stays Unknown. Recipe fields and issued photo URLs are retained;
no URL is synthesized from an ID/storage key.

Combined history MVI/UI, delegated Home actions and Android route activation
are now implemented. Home editor/entry migration continues in the Home child;
prepared data/detail MVI are not active bindings yet. Physical-
device/live-read release checks remain open; see the combined-history plan.

Verification: three pure Home model tests, two new history model tests and 14
app read/adapter/session/transport scenarios per Android unit variant pass.
Both app APKs and expanded packaged DEX checks pass, as do app/Home/history lint
and shared domain compilation for iOS Simulator Arm64. Unknown states, recipe/
photo values, same-day bounds, cancellation and account replacement use fake
repositories/HTTP only. No real account, writer, camera or iOS screen was touched.
