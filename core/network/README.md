# Network infrastructure

Owns shared API JSON configuration, base URL setup and upload timeouts.
Also provides an engine-independent client factory and `requestResult`, which
returns Kotlin `Result` and rethrows coroutine cancellation. The caller owns
client/engine lifecycle. These additions are prepared for later integration.
New factory clients validate HTTP status (`expectSuccess = true`), so 4xx/5xx
responses become failures when wrapped in `requestResult`. Callers may override
validation explicitly. Legacy client response handling is not changed.
Future consumers: feature data modules during migration and application composition.
The legacy network module remains independent until explicit integration.
Dependencies: Ktor, Kotlin serialization and coroutines; no feature or legacy modules.
Public surface: `HttpClientFactory`, `requestResult`, `configureApiTransport`,
`configureUploadTransport`, and `networkJson` for transport contract serialization.
Ktor core is exported because the public extensions expose `HttpClientConfig`.
Serialization options are defined once in `networkJson`.

The Gradle boundary prevents infrastructure from depending on business DTOs or
services. Engines, caching and auth integration remain in the legacy client in
this first slice. Existing iOS target variants are declared for consumer
compatibility; this module contains no iOS-specific implementation.

## Independent bearer authentication

`HttpClientFactory.authenticatedApi` installs transport authentication using
`BearerSession` and `SessionTokens`, with no dependency on `AuthResp`,
`AuthService`, storage or feature endpoints. `SessionTokens.toString` redacts
credentials. HTTPS is required; automatic token attachment and 401 refresh are
restricted to the API origin (scheme, host and port), not arbitrary external URLs.
Upload/plain factory clients stay unauthenticated.

Ktor coordinates concurrent refresh attempts per client and limits auth retries.
Refresh callbacks return Kotlin `Result`; errors propagate to the original
request through HTTP failure handling. Ordinary refresh failure or a missing
refresh token clears the stored session, matching the legacy policy. Cancellation
propagates without clearing credentials. No bodies, cookies or tokens are logged.

Callbacks must use a separate plain client and must not call the authenticated
client from `load` or `refresh`. Token persistence and API response/cookie mapping
belong to future feature data/composition adapters. Call `clearBearerTokenCache`
after external login/logout changes; session owners must coordinate such changes
with in-flight refresh (for example cancel session work before logout).

Migration: adapt legacy token storage and refresh service to `BearerSession` only
at the explicit integration stage, wire the engine and client lifecycle in app
composition, then switch consumers and remove the old client when unused.
Current legacy configuration, DI and consumers are not modified. Tests use MockEngine and cover
refresh retry bounds, parallel 401 responses, failures, cancellation, token cache
invalidation, absent credentials and foreign-origin redirects.

## Opt-in cache and safe diagnostics

`configurePublicHttpCache(storage)` prepares Ktor HTTP caching for anonymous
clients. Storage is explicit, not a hidden global/unbounded cache. Common code
does not choose a filesystem path or Android context. The caller owns capacity,
cleanup and lifecycle; create dedicated storage rather than reusing legacy caches.
Shared-client mode and disabled private storage prevent private response caching.
Requests with Authorization/Cookie are rejected, including cache hits. Responses
setting cookies or varying on session headers are not stored/reused. HTTP
Cache-Control, expiry and ETag revalidation remain Ktor responsibilities.
Use only for public endpoints: the server must still label sensitive responses
correctly. Authenticated/upload clients do not automatically install this cache.

`configureNetworkDiagnostics(observer)` emits only a fixed method enum, outcome
and numeric HTTP status when available. There is intentionally no URL, path,
query, header, body, timestamp identifier or exception text. This is not a cURL
logger. Composition chooses a debug/platform sink; no observer is installed by
default. Ordinary observer failures do not fail requests; transport exceptions
and cancellation remain unchanged. Events represent send-hook activity, not an
exact engine-call count: retries, redirects and cache hits depend on Ktor hooks.
Authenticated clients accept an optional diagnostics observer but no cache option.

Existing FileStorage and cURL logging remain untouched in legacy modules until
integration. Contract tests cover cache hits, private/no-store responses, cookie
exclusion, credential rejection, conditional revalidation and diagnostic privacy.
