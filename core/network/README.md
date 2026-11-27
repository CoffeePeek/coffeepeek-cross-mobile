# Network infrastructure

Reviewed against the current source on 2026-11-27. This is the active shared
transport module, not a future replacement for a separate legacy network module.
Feature DTOs and endpoint APIs stay in feature data; composition supplies settings,
session storage, refresh exchange and the optional request observer.

## Application clients

`ApiClients` owns four transports:

| Client | Purpose |
| --- | --- |
| `plainClient` | Sign-in, refresh and anonymous calls without bearer credentials |
| `client` | API calls with the current session and HTTP cache |
| `imageClient` | Authenticated photo delivery without Ktor `HttpCache`, sharing refresh coordination with the API |
| `uploadClient` | Presigned uploads without the API base URL or session credentials |

Application clients preserve their established status contract: 4xx responses
remain available for feature handling; 5xx responses fail with
`ServiceUnavailableException`. `requestResult` returns Kotlin `Result` and
rethrows coroutine cancellation. `networkJson` centralizes serialization.
Platform engines are OkHttp on Android and Darwin on iOS; tests/Sandbox can inject
engine factories. Client owners must manage their lifetime.

The separate `HttpClientFactory` supports an injected engine and transport-level
contracts, with `expectSuccess = true` by default. Do not assume its status policy
is the same as `ApiClients` or silently change feature error handling.

## Credentials and refresh

`BearerSession`, `SessionCredentials` and `SessionTokens` keep transport independent
of feature auth models and storage. Token string representations redact secrets.
Bearer attachment and refresh are restricted to the configured API origin;
production transport requires HTTPS. Uploads do not receive credentials.

`SessionRefreshCoordinator` binds a retry to the actual rejected credentials,
coordinates concurrent API/image refreshes and uses atomic replacement. An account
switch must never replay an old operation as a different user. Refresh callbacks
use the plain client. Explicit login/logout owners invalidate cached credentials
and coordinate in-flight work. Cancellation propagates without clearing a valid
session. The Auth exchange uses JSON `PUT /api/tokens/native` on the plain
client and requires a rotated refresh token plus both server expiry timestamps.
Only refresh HTTP 401 definitively rejects and invalidates the session; timeout,
network errors, 429 and 5xx retain it. Other unusable responses are failures, not
authorization to erase credentials. See the implementation/tests for exact
rejection versus transient-failure handling.

## Caches and diagnostics

`configurePublicHttpCache` is an opt-in anonymous-client policy with caller-owned
storage. It rejects credential-bearing requests and excludes private/cookie
responses. HTTP freshness and ETag handling remain transport responsibilities.
Protected check-in photos use the separate application permission/cache protocol
in [core/media](../media/README.md); never put their manual 304 responses through
Ktor `HttpCache`.

`configureNetworkDiagnostics` emits fixed method/outcome/status values without
URLs, headers or bodies. `ApiClients.requestObserver` is the composition adapter
for request observations. Debug cURL output uses the sanitizer in
`CurlInterceptor`; it is a separate diagnostic facility, not an analytics SDK.
Review local diagnostic output before sharing it.

Contract tests in `src/commonTest` cover refresh races/rejection/cancellation,
origin boundaries, transport errors, cache exclusions and diagnostic privacy.
Run the relevant non-UI unit tasks with `--max-workers=6`; changes affecting
Native code also need executed Native tests and the full iOS build. Source review
or a host-skipped task is not runtime verification.
