# Share favorite collections — 2026-11-02

## Scope and ownership

A user chooses saved shops/roasters, names a collection, previews it and shares its
public link or branded PNG cards. Membership is frozen when created. Recipients
can read without signing in; only the creator can revoke a link. Other favorites
are never copied. Revocation cannot retract downloaded images.

The new `feature/collections` owns collection creation, reading, selection and
export presentation. Its api exposes only screen entry points and share output;
domain owns pure models, rules and repository contracts; data owns Ktor DTOs and
transport; impl owns typed MVI and shared Compose content. Separate Gradle
boundaries prevent UI/data and cross-feature dependencies. Packages alone would
not enforce that boundary. Domain depends on coroutines only; data consumes
domain/Ktor; impl consumes domain/api/design-system/presentation. Application
composition supplies session identity, authenticated client, navigation and
native sharing. Existing favorites writers and the iOS favorites screen stay in
place. No second favorites store or generic sharing manager is introduced.

Backend: Shops Discovery owns selected membership and public card reads. Store
stable entity identities, resolve current published cards on read, hide removed
or unavailable entities, and keep revoked collections unavailable. Gateway owns
public GET routing and authenticated writes. Website owns anonymous collection
pages and browser sharing. Backend and website changes are prepared in isolated
checkouts; production deployment is a separate gate.

## Verification before changes

- Contract/unit tests: selection across both kinds, stable order, empty/oversized
  validation, server failures, cancellation, account replacement, revocation and
  anonymous read; unselected favorites never appear.
- Backend tests: real EF mapping/migration, owner-only revoke, snapshot survives
  unfavorite, unavailable businesses omitted, route authorization. Existing
  favorites projection/contract regressions must remain green.
- Mobile: compile affected modules, unit tests, Direct Sandbox APK + packaged
  runtime-class and isolation checks. Compile iOS shared code. No automated UI,
  instrumentation or screenshot tests are added/run.
- Web: affected unit tests, TypeScript and production build. No live account
  mutations or production analytics are used in tests.
- Manual gates: chosen Sandbox device, both themes, RU/BE, large fonts/window
  sizes, preview/QR readability, native share/cancel and revoked/unavailable link.
  Pixel 9 is the selected Android device; previews/builds do not certify these gates.

## Evidence

Implemented from develop `a016fc78` on `codex/migration-favorites-sharing`.
The only composer entry is the profile menu. Per the user's follow-up, no share
button is added to the Favorites screen or its profile preview.

Executed on 2026-11-02:

- 35 relevant mobile unit/contract cases: domain 3, transport 6, ViewModel 11,
  pre-send account guard 3, actual Sandbox DI 2, Sandbox backend contract 1,
  AppLinks 9; no failures. Existing Favorites tests passed separately (15).
- Direct Debug APK assembly and packaged runtime-class checks passed. Direct
  Sandbox assembly, packaged runtime-class checks and network-isolation checks
  passed. Full shared application `compileKotlinIosArm64` passed; this compiles
  Kotlin native adapters, not the Xcode application or iOS device flow.
- The reported false-empty Sandbox screen was caused by applying a production
  bearer-token guard to its intentionally credential-free fake HTTP engine.
  Sandbox now has an explicit local transport binding that retains fake-owner
  checks. Production still rejects missing/mismatched bearer credentials.
  The composition regression tests use those actual bindings. A canceled load
  for the unchanged account displays a retryable error, never the empty hint.
- Export generation/account checks reject old-account completion and Back cancels
  preparation. Public reads never load private account data. Optional photos
  have bounded download, decode, concurrency and time budgets; the readable
  names/QR fallback remains available when images fail.
- Updated Pixel 9 (`48111FDAQ001K5`) in place with Direct Sandbox
  `1.1.5.2665-sandbox`, package `com.coffeepeek.sandbox`. Signature matched the
  installed Sandbox; first-install time remained unchanged. Startup succeeded.
  Installed fix APK SHA-256:
  `D1FB47CF865FF0058213E04640580A56614A4220452B8D6064CB6D83A206EE60`.
  This identifies the tested working patch on `a016fc78`; version alone does not
  identify a source commit. Later accessibility/verification edits are separate
  from that installed artifact. No live app, real favorites or phone settings
  were changed; no data was cleared.
- Backend: 19 persistence/favorites cases, 92 host/architecture/static-handler
  and Discovery contracts, 34 Gateway route cases passed. Shops/Gateway Release
  builds and EF pending-model check passed. Generated migration only adds the
  two Discovery-owned collection tables. Secret scan found no leaks.
- Web: 30 API/analytics cases and 53 server-rendered-route/link cases passed;
  TypeScript and production build passed. Unlisted collection IDs are removed
  from analytics URLs. No backend or website production deployment was made.

Remaining gates: manually inspect both themes, RU/BE, accessibility, large fonts
and narrow windows, poster pagination/cropping/QR, loading/errors, revoke/cancel
and anonymous links. The phone was locked/in another app when a further manual
review was attempted, so no unrelated screen was captured or operated. Colocated
light/dark previews exist but were not rendered in an IDE. Existing Compose
preview/BackHandler deprecations remain. Sandbox deliberately blocks outbound
native intents; do not disable that guard to certify share sheets. Real Android
and iOS share/cancel, Xcode build, PostgreSQL migration/transactions, gateway
deployment and website/app associated-domain behavior require separate acceptance.
No automated UI, Compose instrumentation or screenshot tests were created/run.
