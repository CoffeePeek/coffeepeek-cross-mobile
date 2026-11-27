# Coffee shelf

Reviewed against `develop` on 2026-11-27. This guide replaces completed timber,
rail, scanner-label and native activation plans; prior verification is in Git.

## Ownership and entries

Shelf has populated `api/domain/data/impl` boundaries. `ShelfEntry` covers the
owner shelf, add, bag and archive routes; `ShelfProfileEntry` covers public
profile projection. ViewModels own shelf loading/reorder/finish/undo, visibility,
archive paging, scanner/add and bag actions. Composition supplies the account,
media, time-zone, camera, coffee catalogue and brew-history ports.

Android uses feature Compose. Native iOS profile slots and root destinations
use SwiftUI through feature-owned sessions over the same Kotlin ViewModels.
Sessions own their child stores and close subscriptions, camera and pending
work on disposal. Swift renders state; Kotlin retains mutation/scanner policy.

## Current profile presentation

- The owner's Shelf tab displays bags without a repeated shelf heading,
  bag/roaster/weight totals, bag captions or scan CTA. Accessible names and
  open/reorder/finish actions remain. Bags occupy rows of three on glass rails,
  including every returned item rather than only the first row. The old oak
  texture and timber material are retired.
- One glass settings control floats above the root navigation, matching the
  Collections control's placement. It opens visibility settings and the archive
  in one sheet: search, month groups and paging share one scrolling content area.
  Android bounds that sheet to 80% of its modal viewport and skips partial
  expansion. The archive child store exists for the opening and closes with it.
- Selecting an archived bag seeds its handoff before dismissing/navigating; an
  opening consumes only its first navigation event. Restore/delete refresh page
  one for the current query so shifted items remain reachable. Other full-screen
  archive consumers retain their public entry contract.
- Public shelf projection is read-only and respects visibility. Its bounded
  anonymous photo access is separate from private authorized bag images.

## Data, photos and scanner

Data owns `/api/v1/shelf` requests, DTOs, uploads and mapping. Finish, restore,
delete and reorder retain existing explicit writes, duplicate-work protection,
session guards and undo behavior. The add form edits optional metadata before
creation; a separate saved-bag editing UI must not be inferred from a data update
method. Closing settings does not create or modify bags.

Native camera uses the existing AVFoundation session; gallery uses PHPicker.
Retake/cancel preserves destination ownership. Revoked account callbacks cannot
apply selected photos. Scanner preparation, model readiness/download, original
processing and processed-card upload stay in Kotlin. Download progress renders
one percent sign. Scanner cutouts are a shelf capability; do not restore retired
drink-photo extraction in Home/Shop check-ins.

Private/archive covers observe the authorized `IosCatalogueMedia` image stream
and its current scope/photo identity. Cancellation and expiry clear frames;
protected URLs are not handed to a raw anonymous one-shot decoder. Public cards
use the public photo contract. See [media](../../core/media/README.md).

The iOS runtime remains checksum-pinned ONNX Runtime 1.24.2 with local bindings.
Keep the [package guide](data/ShelfOnnxRuntime/README.md) and
[third-party provenance](data/ShelfOnnxRuntime/THIRD_PARTY.md); do not disable TLS
or checksum verification to resolve a download failure.

## Source and verification

Start with [API](api/src/commonMain), [backend/scanner](data/src),
[shared UI and ViewModels](impl/src/commonMain) and [iOS sessions](impl/src/iosMain).
Existing unit/contract suites cover archive paging, handoffs, public projection,
scanner/add, mutation guards and native session disposal. For code changes run
the affected tests, platform compilation and Android DEX/Sandbox isolation checks.

Manual acceptance remains distinct: owner/public/hidden/guest states, empty and
multirow shelves, light/dark and reduced transparency, large text/accessibility,
sheet initial size, search/paging, back/cancel, drag/finish/undo, photos after
expiry/account replacement and native scanner continuity. Use an explicitly
selected Sandbox or authorized test account; this docs review performed no app
run, photo selection, model download, live write or device check.
