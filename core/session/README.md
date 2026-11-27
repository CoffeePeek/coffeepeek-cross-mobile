# Session infrastructure

Reviewed against current source on 2026-11-27. `core/session` owns session models,
read/store contracts, credential persistence and platform secure storage. It does
not own login UI, HTTP token exchange, navigation or a second analytics service.
Composition supplies existing Room legacy-row access through a narrow port.

## Persistence and account lifetime

`DefaultSessionStore` serializes writes and publishes an in-memory snapshot with
revision/account generation. Responses from a replaced account must not update
its successor. `replaceSession` is conditional on the expected session; refresh
must not replay old work as another user.

One-time import reads secure storage first, then the old SESSION/API_KEYS rows.
It writes credentials successfully to secure storage **before** deleting both
legacy rows. During logout the order is intentionally reversed: remove both old
rows **before** writing an empty secure store, so the next startup cannot import
and resurrect a signed-out session. Persistence commits use NonCancellable where
needed without making arbitrary business requests uncancellable. Do not replace
this with data clearing or remove the legacy-row adapter only because of its name.

Android secure storage and iOS Keychain are platform implementations. Existing
storage identifiers and upgrade behavior are compatibility contracts. Logout
clears local session/device memory; it never deletes server favorites or uploads
old local favorite rows.

## Token exchange boundary

[Auth](../../feature/auth/README.md) owns native token DTO/exchange.
[Network](../network/README.md) coordinates credential-bound refresh/retries.
Native refresh uses JSON PUT `/api/tokens/native`, requires rotated tokens and
both expiries, and only HTTP401 definitively invalidates the refresh session.
Transient errors retain session state. Request cancellation stays explicit.

## Verification

Existing `src/commonTest` covers import ordering, persistence races, account
identity and logout behavior; network/auth suites cover refresh and stale
requests. Platform secure-store access, upgrade of an old installation,
background/resume and account switching remain separate device gates. This
Markdown refresh ran no app, login, logout, storage cleanup or live request.
