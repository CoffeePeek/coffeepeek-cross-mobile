# Reports about published content

Reviewed against `develop` on 2026-11-27. This feature reports publications;
[Shop Report](../shop-report/README.md) handles inaccurate place information.

## Ownership and runtime

The populated `api/domain/data/impl` modules own the report target/reason, request
contract, MVI form, resources and entry. App composition supplies the configured
authenticated client, an account-bound child lifetime and navigation callbacks.
Android renders the feature Compose form; iOS uses `NativeContentReport` and the
native SwiftUI form over the same report ViewModel. Public APIs do not export
DTOs, repositories or ViewModels. Domain has no transport/platform dependency.

`ReportCheckIn(id, isPreview)` is used from shop detail/check-in lists and public
profile cards. The `ReportReview(id, isPreview)` compatibility destination and
review-specific backend contract remain; retaining that route does not imply an
active review editor. Community's inline report presentation remains owned by
Community and must not silently change into this route's form.

## Contracts to preserve

- A reason is required and trimmed length must be 1–2000 characters. Preserve
  Unicode pairs while truncating input. Submit is explicit; success/preview
  confirmation, Back/Done and guest sign-in retain their existing semantics.
- Data posts to `/api/v1/check-ins/{id}/reports` or
  `/api/CoffeeShopReviews/{id}/reports`, using the existing configured client.
  Request/response validation and error categories remain inside data.
- There is one pending submission. A known rejection permits correction/retry.
  Lost/invalid acknowledgements and conservative unconfirmed outcomes block a
  second send in that form instance. No automatic delivery retry or cross-route
  idempotency guarantee is introduced; auth refresh remains client-owned.
- Account replacement revokes requests/actions and clears private draft data.
  Same-account token refresh preserves the draft. Missing stable account IDs
  fail closed on identity changes. Preview bypasses session/network ownership.
  Cancellation is propagated, not rendered as another recoverable rejection.
- Root navigation/callers remain analytics owners. Do not create a new report
  tracker or production telemetry from tests.

## Source and verification

Inspect [API](api/src/commonMain), [validation](domain/src/commonMain),
[transport](data/src/commonMain), [MVI/Compose](impl/src/commonMain) and
[native adapter](impl/src/iosMain). Existing domain/data/ViewModel and app session
contracts cover reason limits, endpoint selection, known/uncertain failures,
guest/preview and stale-account behavior. Sandbox uses validated in-memory
check-in reporting; it does not manufacture a review-report dataset.

For behavioral changes run affected non-UI suites, Android assembly/DEX and
Native gates. Manual acceptance separately covers keyboard, light/dark and large
text, accessibility, success/uncertain states, repeated taps, Back and completed/
cancelled native pop. No real report, app run, build or device check occurred in
this documentation-only review. Legacy exit acceptance remains tracked by the
[feature exit plan](../LEGACY_EXIT_PLAN.md).
