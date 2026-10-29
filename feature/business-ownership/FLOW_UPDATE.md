# Business flow correction — 2026-10-29

Source: codex/migration-business-flow, base develop a46715f7, validated feature tree.
Backend: codex/business-evidence, base be74a79b.

## Behavior and boundaries

Ownership owns access reads, mode switching, claim sheet, business home and
direct editor. Shop/Roaster consume only its api through the app-provided entry.
No new modules or feature-to-feature domain/data dependencies were introduced.

A non-owner coffee-shop header keeps favorite/share and adds a trailing menu
with Edit and I represent this coffee shop. Edit retains moderated shop changes.
The claim belongs to the loaded public slug, without manual URL entry.

Per the user's clarification, non-owners do not edit roasters; they get the
representative action. Confirmed owners see direct Edit, favorite and share for
both kinds. Owner editor writes existing UUID endpoints with If-Match and
publishes immediately. Owner list publicSlug maps profile slugs to owner UUIDs.
Leaving a saved editor invalidates the public details.

Claim: feature-owned bottom sheet, contact, explanation, up to three private
photos of at most 5 MiB. Includes discard/busy guards, guest sign-in, identity
change disposal and in-memory photo state. Paired fake-state light/dark previews
are colocated. The shared design-system menu is Android-only; shared dropdown
uses Material, while sheet/photo controls/buttons/dialogs use design-system.

My Business is removed from personal-profile menus and its obsolete callback.
Settings offers Switch to business account only for current owned businesses.
Business mode has exactly Business and Settings. Business shows owned shops/
roasters and the existing personal check-in activity/history link. Each business
can open its public profile or direct editor. Settings switches back.
Mode is account-scoped; token refresh preserves it. Logout/account replacement,
failed access revalidation or revocation clear permissions/mode. An old Approved
claim does not grant current access.

## Contract and release order

Deploy backend and migrations before client:
Shops 20261029083139_AddOwnershipEvidencePhotos;
Media 20261029083142_AddOwnershipEvidencePhotos.

Private upload: POST /api/Photos/ownership-evidence, multipart file.
Claim POST adds evidencePhotoIds. Read:
GET /api/owner/ownership-claims/{id}/photos/{photoId};
admin uses the corresponding /api/admin/ownership-claims path.
Public gallery uploads are not evidence. Existing text-only clients remain
compatible. Owner-list publicSlug is additive.

Existing Shop edit/favorite/share intent and exposure ownership is preserved.
Ownership adds no analytics service and emits no contacts, proof or photo bytes
as analytics properties. Roaster share uses the app share service. Sandbox uses
its bundled fake photo and in-memory HTTP stores.

## Verification

Plan: preserve non-owner shop moderation, server favorites/revisions; test
current bindings vs claims, account change/revocation, slug/UUID mapping,
private upload/claim contracts and two-tab routing. Build Android Sandbox/Debug.

Final results:
- Ownership unit/contracts: domain 5, data 11, presentation 16; 32 passed.
- Application Direct Debug unit tests: 836 passed.
- Direct Sandbox/Direct Debug APKs and packaged DEX class guards passed.
- Shared ownership and roaster Kotlin metadata compilation passed.
- Backend: 116 unit/contracts passed, 13 live-DB cases skipped.
- ADB inventory: no connected devices. No install, launch, real-account write,
  camera, consent change or production event occurred.
- Studio MCP was unavailable; IDE diagnostics/modal previews were not rendered.
  Existing Compose/AGP/KLIB warnings remain.

Earlier failures retained: first Android build exhausted disk space. A contract
assertion needed additive evidencePhotoIds. A metadata task was requested for
Android-only Shop impl and replaced by its actual Android compilation. Removing
the retired profile callback left one override parameter, corrected before rerun.

Open gates: manual Sandbox light/dark, font/window/accessibility, menu/sheet/
keyboard/back/discard/sign-in/switching review; native iOS app build/runtime on
macOS; prepared live-backend upload/read/approval/direct-save acceptance.
Common metadata does not certify UIKit compilation.
