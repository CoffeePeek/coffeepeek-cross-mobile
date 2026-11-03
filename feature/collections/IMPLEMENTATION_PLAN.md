# Share favorite collections — 2026-11-02

## Denser exported collection layout — 2026-11-03

Use the user's new reference for picture exports. Center each card's information
block vertically, put the kind chip directly under its title, and align check-in
counts to the rating's right edge. Keep shop photos, current statistics and full
available location fields. Roasters wrap into a compact grid with widths measured
from their names. Add repeating COFFEEPEEK strips at both picture edges, retaining
the website and page numbering, with no QR artwork.

Collections impl owns this presentation-only change. A pure row-packing layout
replaces fixed four/six-place pages. Preview navigation, photo preparation,
accessible page descriptions and PNG export consume the same measured plan.
Check stable shops-first ordering, pagination without loss/duplicates and 2D
bounds/non-overlap across both formats, including 205 places and long headings.
Compile/assemble Android with packaged runtime checks, compile shared iOS, and
review the explicitly selected directSandbox manually if Pixel 9 is available.
No automated UI/instrumentation/screenshot tests or production writes.

During manual review the user reported that Story cropped its header/footer and
could not open fully. The thumbnail now bounds both axes before applying aspect
ratio. Tapping opens the existing design-system photo viewer with pinch/double-tap
zoom, close/Back and page swiping. Its generic slot entry is public by necessity;
the feature supplies the bounded poster content rather than copying zoom logic.
New frame/thumbnail/viewer content has colocated light/dark previews.

Verification on 2026-11-03, working patch on develop `e9976ba3`:

- 19 collections impl cases and 57 design-system cases passed (76 total), with
  no failures/errors/skips. Layout cases cover both formats, 205 members,
  variable grid widths, no overlap/loss/duplicates, eight mixed Story cards,
  eight roasters in Chat and seven readable shop rows in Story. RU/BE keys align.
- Direct Sandbox assembly, DEX runtime-class and isolation checks passed. The
  first manual Chat preview displayed the new centered information/branding on
  Pixel 8 AVD, emulator-5580, API 37 x86_64; the original Story crop report above
  remains a failed check before the viewport fix.
- The user explicitly switched to ordinary directDebug. Its assembly and DEX
  runtime-class check passed. The first launch failed before application startup
  because ignored local.properties lacked API_BASE_URL; the production endpoint
  was configured locally and the APK rebuilt, preserving the application guard.
- Debug `1.1.5.2677`, package com.coffeepeek, was installed in place over version
  1092 with matching certificate SHA-256 48ad0071309c72a29cab9ab9747519c06754837d7a3066de2a7b5a8f72a08abe.
  First-install time was retained; no uninstall, downgrade or data clearing.
  Cold startup passed (1156 ms), and MainActivity was confirmed foreground.
  Installed configured Debug APK SHA-256:
  `3735E2CB6255CEDD13E006C331591AD26B36DCF92D35AB9D124588AB81294951`.
- Post-fix fullscreen/zoom and real-collection content review remains a manual
  gate in the chosen Debug app; no real-account collection/favorite writes were
  performed. Native iOS compilation was SKIPPED on Windows, not executed; macOS
  CI is still required. IDE previews were not rendered. Existing deprecations
  were retained. Sanitized diagnostics and emulator captures stay in ignored build.

## CI and review correction — 2026-11-03

Android CI #530 failed at `mergeExtDexPlayDebug` with D8
`OutOfMemoryError: Java heap space` under the repository's 3 GiB Gradle heap.
The CI workflow now gives its three Android phases separate fresh Gradle
processes with a CI-only 6 GiB heap: Direct build, Play build, then unit tests/
lint. The worker ceiling remains six; developer `gradle.properties` is unchanged.
Local Direct and Play builds passed under that command layout. The original
failure is retained; full local and remote validation results follow separately.

Review thread `PRRT_kwDON4AUts6zByTC` identified an owned-link retry returning
to Favorites selection. The ViewModel now retains the requested collection ID,
clears it on account/selection changes, and retries that read on Preview. A
regression checks recovery on the same link without reloading private favorites.

## Composer polish and entry relocation — 2026-11-03

Latest user feedback supersedes the previous entry decision: share belongs in
Favorites, not the profile menu. Remove QR artwork from PNGs, retain the site
branding/page count, and keep the native message's collection URL. Collections
impl owns the semantic-color grouped selector, format preview and pinned primary
action. Favorites exposes a navigation event; root composition supplies the
collection route. No favorite writes are needed. Latest feedback removes the
place-count ceiling in mobile, Sandbox and the server request/domain validation;
the title rule and owner-link quota are separate and unchanged.

Selection now uses continuous grouped rows, inset separators and a trailing
checkmark only when selected. Remove both explanatory paragraphs. Keep the
select-all/clear action floating above the list at bottom trailing, independently
of scroll; the bottom count contains only the selected quantity. Preserve all
members when selecting/creating collections larger than the former 100-place cap.

Use the current CoffeePeek scaffold, text field, segments, grouped surfaces,
buttons and theme. Apple HIG Color/Buttons/Layout were checked on 2026-11-03:
one prominent action, semantic light/dark colors, scalable text and accessible
targets. Verify favorite navigation with unit tests, preserve account/cancel
contracts, check both poster formats after QR removal, assemble Android with
runtime-class/isolation checks and compile shared iOS code. Manual Sandbox review
must cover light/dark, font scaling, keyboard/insets, Back and empty/error states.
No automated UI, instrumentation or screenshot tests will be introduced.

Latest polish verification, 2026-11-03:

- 86 affected mobile unit/contract cases passed. A 205-place selection is sent
  intact; grouping/pagination tests now cover counts beyond the old ceiling.
  Server HTTP annotations/domain/store accept 125 saved places, with all 25
  affected persistence/rating and 92 host/architecture contracts passing.
- Android Direct Debug/Sandbox assembly and packaged runtime-class checks,
  Sandbox isolation and full shared iOS compilation passed.
- RU/BE resource keys align. The counter has one quantity placeholder; the two
  removed explanations and old limit messages are absent. Static HIG design-token
  checks passed; this is not a certification of the complete interface.
- Pixel 9 received Direct Sandbox `1.1.5.2672-sandbox` in place and started
  successfully. Latest grouped-list APK SHA-256:
  `DFCA7CEFF39415E4FE467D3662F750118F9541EED7D4069D856B2E78B154C4DD`.
  Source is the working patch on `d25fea4f`, saved by this change. Observed the new
  Favorites share entry and created-collection preview; the user reviewed and
  accepted the updated interface, then requested merging to develop. Screenshots
  and UI dumps remain local in ignored build output.
- The first manual Story preview extended below the visible area; it was then
  constrained to the viewport without changing export dimensions. No QR drawing
  remains. Text/URL sharing is preserved. Favorites and collection mutations in
  this review used only the local Sandbox backend; no production writes occurred.

Large-font/narrow-window coverage and native iOS share/device review remain
release gates. The iOS legacy Favorites screen only receives a native navigation
action with a Compose fallback; its binding, writer and list are not migrated.

## Reference design update — 2026-11-03

Use the supplied reference for exported pictures: warm dark panels, large shop
photos, readable names, kind chips, street addresses and actual public shop
rating/check-in totals. Roasters use a separate compact logo-first card with
uncropped artwork. Group all shops before all roasters, stably within each kind,
before pagination. Preserve title, count, QR/link, both formats and all members.
Verify pure ordering/pagination contracts, optional statistics and photo bounds;
compile Android/iOS and assemble the explicitly selected Sandbox. Manual visual
and native share checks remain separate from unit tests. No automated UI tests.

Reference update evidence, 2026-11-03, based on `4c585957` plus this feature patch:
40 mobile unit/contract cases passed (including stable global ordering and full/
partial page bounds in both formats). Direct Sandbox assembly, runtime-class and
isolation checks and full shared iOS compilation passed. Backend: 25 affected
persistence/rating cases passed; private/pending visits do not enter the public
statistics and a later visibility change is reflected on the next read. Website:
5 collection API/order cases, 54 server-rendered/link cases, type checks and build
passed. Paired Chat/Story light/dark previews use synthetic data beside the
poster; they were not rendered in the IDE. Pixel 9 was not connected today, so
the new reference design has not been installed or visually checked on a phone.
The earlier installed APK below predates this design update.

Final integration with current develop, `c274549a`, passed Direct Sandbox
assembly/runtime-class/isolation checks and full shared application iOS
compilation. The built Sandbox is `1.1.5.2669-sandbox`; SHA-256:
`3C83B42BFB295956BED999D3C866CE18C6ECA9BAB1B448C2BFE39B83ACCC5350`.
Direct Debug assembly/runtime-class checks passed on feature revision `c4abf2f8`.
Review PRs: mobile #299, backend #376, website #35. Website CI passed; backend
and mobile CI were still running when this evidence was recorded.

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
- Updated Pixel 9 in place with Direct Sandbox
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
