# Profile display controls — 2026-11-04

## Scope and verification plan

Complete the profile integration supporting saved collections in the current
`codex/migration-saved-collections` checkpoint (PR #302, parent `79cc0e9e`).
The actual own/public profile uses migrated common UI on Android and iOS; global
Settings alone is insufficient. Reuse the existing Edit Profile destination,
feature slots and design-system grouped rows/switches. No new Gradle modules.

- Edit Profile exposes server-backed collection and shelf visibility controls.
  Their immediate-save behavior is labelled independently of the identity form.
- Selected public collection title is the profile heading. With no selection the
  owner's existing favorites preview remains «Избранное»; another visitor never
  receives the owner's global favorites. Existing nullable featured ID is reused.
- Shelf is private by default. Its owner can explicitly expose only active bags
  to profile visitors, then hide them again. Public projection excludes archive,
  notes, purchase data, owner IDs and storage keys. Public photos recheck current
  visibility, author access, bag status and the currently attached photo.
- Submitted cafe/roaster menus move to the owner's corresponding real counters;
  another user's counters do not navigate into owner-only submission lists.

Collections owns its selection/presentation; shelf owns visibility/public models,
HTTP, UI and persistence contract. Profile owns layout/stat actions. Application
composition supplies slots, routing, account-pinned HTTP and DI. The shared
session-pinning adapter replaces duplicated network-boundary logic in composition,
preserving existing collection request behavior. Features do not import another
feature's domain/data/impl.

Verification: sequential Gradle with max-workers=6, affected shelf data/impl,
collections impl, profile impl and non-UI DI/session/fake HTTP regressions; Debug
assembly including packaged DEX checks. Backend authorization/projection/media
and default-off migration tests; web transport/public profile validation. Preserve
existing favorites analytics and contributions routing; no new telemetry service.
No automated UI/instrumentation/screenshot tests. Colocated light/dark previews
use fake state. Manual launch remains ordinary Direct Debug on the agreed
Pixel_8 emulator; real-account visibility/favorite/collection writes are not part
of agent review. Native compilation, server deployment and live opt-in acceptance
remain separate gates and must not be inferred from fake/unit results.

## Evidence

Initial shelf-data suite passed (55s) with the new visibility/public transport
regressions and existing owner shelf contracts. Existing unnecessary-non-null
warnings in ShelfRepositoryContractTest lines79–80 were retained. The subsequent
photo-path Unicode validation adjustment requires the final aggregate rerun.
The final aggregate suite passed 299 cases with zero failures/errors/skips:
shelf data46, shelf impl78, collections impl52, profile impl62, selected app
contracts61. It includes actual repository-over-fake loading of all seven bags,
including a photo-only bag, and the existing collection before-engine session
guards after extraction to the shared application adapter. Original failed
attempts remain in ignored local logs: profile-display-first-compile-failure.log
(test-only event-list type inference) and profile-display-second-compile-failure.log
(a fixture imported serializer plugins not directly exposed to the app test
module). The fixture now uses clones of the existing configured gateway client;
no extra production dependency was added. Subsequent suite passed in37s.

The last UI-only adjustment reused CoffeePeekSettingsRow for the shelf toggle,
with a single accessible switch role and a whole-row target. Its Direct Debug
assembly and packaged DEX checks passed in1m14s, log
`build/profile-display-final-ui-build.log`. Static HIG helper checks confirm the
48x48 minimum target and existing semantic secondary-text contrast (4.8 light,
5.69 dark). These numeric checks do not certify complete accessibility.
RU/BE keys align: collections87/87, profile122/122, shelf212/212.

Final APK: package com.coffeepeek, launcher com.coffeepeek.admin.MainActivity,
Direct Debug1.1.5.2682/code2682, SHA-256
`1D380018140EC019EC0AAD689C6D17378577DF31A7B4C50B2D38EB76E6AECAF3`.
Source is parent79cc0e9e plus this profile-display patch. On2026-11-04 the user
explicitly chose the currently running Pixel_10_Pro_Fold AVD, emulator-5554,
API37/x86_64. Installed1.1.2.2538 and built APK had matching signing certificates;
the in-place update succeeded and cold MainActivity/foreground were confirmed.
First-install time2026-11-04 09:03:23 was preserved. No data clearing, downgrade,
uninstall, account replacement, favorite or collection/visibility writes occurred.

Read-only manual review opened Edit Profile and confirmed both visibility rows,
the grouped heading and immediate-save footer on the wide Fold/light layout.
Shelf preference load failed while the new backend route is not deployed. A
collection Save error was also visible; its trigger/cause was not established,
and it is not recorded as successful persistence. Later HTTP-status-only
diagnostics were cancelled when that emulator disconnected; the subsequently
visible Pixel_8 was not substituted for the user's selected Fold. Screenshots
and UI dumps remain ignored/local and are not included in commits. No switch,
form Save or other real-account write was exercised. Dark theme, narrow/folded
windows, larger fonts, IME and full native/device acceptance remain unverified.

The main Profile tab, no-slug own fallback, and own public-profile destination
use the selected-title preview/private favorites fallback consistently. Both
own routes open submitted shops/roasters from their counters; non-owner counters
remain read-only. Public shelf taps open only public coffee catalogue pages,
never private owner bag destinations.

Backend local verification:133 cases passed; one new live PostgreSQL migration
case compiled but was skipped locally and is enforced in CI. Web463 Jest plus65
SSR cases, both TypeScript projects, production build and design checks passed.
New shelf backend/web changes are not deployed by these checks. Production
opt-in/opt-out, media denial after hiding, account-scoped persistence, native
compilation and the mandatory PostgreSQL migration CI gate remain acceptance
requirements; unit/fake success does not replace them.

## Initial profile shimmer — 2026-11-04

The migrated own-profile fallback previously used a centered section loader,
while the shared public-profile route showed a route-supplied avatar/name plus a
section loader before the profile response arrived. Both actual initial-loading
branches now render `ProfileLoadingSkeleton` using the existing shared
`CoffeePeekShimmer` primitive. The main own-profile tab delegates to that shared
public route once its public address is known; the no-address fallback also uses
the new skeleton while no profile object is available.

The skeleton contains only neutral avatar, nickname, counter, biography, activity
and main-section shapes. It has no identity inputs, image loading, DI, network
calls or interactive targets. One existing localized `profile_loading` status
owns the accessibility announcement; decorative shapes are hidden. The RU/BE
resource key is reused without adding hard-coded UI copy. The initial skeleton
cannot be scrolled and owns no real-content scroll state. Cached profile refresh
continues rendering its existing content; no resume/top-scroll effect or ViewModel,
API, domain, persistence or account behavior was changed.

Paired common light/dark previews live beside the new component. The integrating
root must record the final Debug assembly/runtime-class checks, exact source/APK
identity and agreed Pixel device launch. Full manual initial-loading appearance,
Back/scroll preservation, themes/font/window sizes and reduced-motion acceptance
remain device gates. No automated UI/instrumentation/screenshot tests or tests
that merely duplicate the layout were added or run for this UI-only change.
