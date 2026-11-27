# Personal check-in history

Reviewed against `develop` on 2026-11-27. This guide replaces completed Android-first
preparation and combined-history checkpoint ledgers; Git retains their evidence.

## Ownership and surfaces

The populated `api/domain/data/impl` modules own combined Shop/Home read models,
calendar/filter/page state and the profile history entry. They do not own either
feature's writer or the application's root navigation.

- `ProfileHistoryEntry` retains the signed-in calendar/filter/paged timeline across
  profile tab changes. Selecting a day advances its selection revision so the
  caller can reveal the Check-ins tab. The caller owns its header and navigation.
- Its current app adapter is `FeatureProfileActivityRenderer`, which constructs
  `createProfileHistoryEntry` and exposes one calendar/list scope. The old
  `ProfileActivityEntry` and standalone `VisitedPlaces`/`HomeCheckIns` destinations
  are absent from the current public API/root route model. Do not copy their
  activation instructions from retired plans. Public profile calendars must not
  receive private readers.
- Home detail/editor overlays delegate to [Home check-ins](../home-check-in/README.md).
  iOS route adapters supply native detail/edit presentation over those existing
  shared contracts; they do not create a second Home store or writer.

## Date, paging and privacy contracts

Shop history reads `/api/v1/check-ins/mine` using instant bounds with an exclusive
upper bound. Home reads cross an application adapter to Home's inclusive civil
date range. A captured time-zone snapshot converts each boundary independently,
including DST; a calendar day is not assumed to be 24 elapsed hours.

`CombinedHistoryViewModel` owns All/Shop/Home filtering, source-specific paging,
independent retries and retained-content refresh errors. Selected-day Home reads
exhaust their pages and deduplicate records. Calendar counts/photos follow the
existing source contracts; timeline order and photo selection have dedicated
presentation helpers and tests. Preview, detail and original photo order stays
aligned through the caller-owned viewer.

Composition supplies revocable account-bound reader adapters, clock/date labels,
images and child stores. Account replacement clears private state and rejects late
reads/events. Known-user token refresh preserves ownership. Root destinations
own individual Home detail/edit IDs; stale navigation cannot replay into another
account. Successful writes invalidate history; failed or cancelled forms retain
their existing behavior. Creation analytics has one session-aware owner,
including delegated Home flows.

## Source and verification

Start with [public entries](api/src/commonMain), [history models](domain/src/commonMain),
[Shop transport](data/src/commonMain) and [presentation](impl/src/commonMain).
The actual Home/date/session assembly is under
`composeApp/src/commonMain/kotlin/com/coffeepeek/admin/di/checkinhistory`; native
Home route integration is in the matching `iosMain` directory.

Existing non-UI suites cover date arithmetic, inclusive/exclusive ranges,
calendar photos, Home page exhaustion, timeline ordering, filter/request races,
Profile Activity and session replacement. Execute affected tests plus app bridge
contracts and platform builds for behavioral changes; Native test selection must
include any new common/ios suite explicitly where CI filters tests.

Manual gates remain: month/day/today/filter navigation, combined paging and
per-source retry, profile tab/scroll continuity, both themes, large text and
accessibility, photos, detail/edit/back/cancel, pending save and account changes.
Use [Sandbox](../../composeApp/SANDBOX.md) for synthetic records; live backend/date
parity is separate acceptance. This documentation review performed no build,
device test, live record creation or telemetry emission.
