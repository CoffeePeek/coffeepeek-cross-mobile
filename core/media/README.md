# Shared image infrastructure

Reviewed against the current source on 2026-11-27. This module owns URL/byte
loading, bounded public/protected caches, access revocation, platform decoding,
progressive image resources and bounded speculative work. Design-system owns
Compose visuals and viewer gestures; native SwiftUI presenters use the same KMP
image/access contracts through application adapters.

Dependencies include core/session, Ktor, coroutines, Compose/Kamel resource APIs,
Android bitmap/EXIF and iOS ImageIO. No feature or application DI dependency is
allowed. Composition supplies clients, `SessionReader`, API URL and author
visibility invalidation; media does not create a second authentication service.

## Photo representations and ownership

Cards can use feed previews, calendars thumbnails, and detail viewers the supplied
detail URL. Progressive upgrades keep the ready preview while a better image
loads or fails. The current photo may fall back to its original when no detail is
ready; speculative work must not request a protected original. Keep source URL,
recipe/purpose/format, gallery order and access scope intact.

Protected image fetching uses `ApiClients.imageClient`, without Ktor `HttpCache`
and with platform URL-cache bypass. Manual ETag/304 handling belongs to this
module. API and image clients share credential-bound refresh coordination;
account replacement cannot retry an old request under the new account.

## Protected permission and cache contract

Encoded immutable variants use a 48-entry / 12 MiB LRU. Their keys include the
complete source identity and an opaque rotating session scope. Storage requires
an explicit response grant; `no-store` evicts bytes/ETag. Zero freshness can allow
online revalidation, never an offline stale display. A 304 must authorize the
exact retained bytes, ETag and current policy; missing/invalid metadata or a
failed/denied request cannot expose stale bytes.

Reusable freshness is capped by server max-age, 20 seconds and authenticated JWT
expiry. Date/Age, transfer/decode time and wall/monotonic deadlines participate.
Missing Date grants no positive reuse. Unknown/expired authenticated tokens fail
closed. An online-only grant authorizes that presentation, not a later cache hit.

Decoded protected painters have a separate 48-entry / 24 MiB LRU and require a
reusable display grant. Retained bytes are not permission to render. Session/token
changes, logout, author visibility, photo-affecting mutations and backgrounding
revoke grants. Remove a painter at its actual deadline before reauthorization;
resume rechecks clocks. Original/private/home/moderation responses retain their
no-store behavior. These policies must survive replacement of any UI renderer.

## Bounded work and iOS decoding

Transfers are limited to four, duplicates share in-flight work within one access
scope, and leaving the final subscriber cancels queued/running work. Late results
from cancelled or revoked generations cannot publish or replace current results.
List lookahead is bounded (three items, six distinct URLs, two speculative loads).
A full-screen viewer pauses covered-screen speculative work; prefetch does not
renew unseen grants. Protocol variant bodies have an 8 MiB limit.

`IosImageBitmapDecoder` uses ImageIO thumbnails with a default maximum edge of
2048 pixels, preserves orientation/alpha, checks cancellation and serializes native
decode. `ImageBitmapMemoryCache` defaults to a 48 MiB pixel budget and 100 entries.
Those bounds do not cap active painters, decoder temporaries or GPU memory.
The native `IosPhotoPresentation` observes access expiry/session changes and emits
cleared frames before reauthorization. Keep raw HTTP channel consumption inside
the owning response scope; buffering across that boundary changes its lifetime.

## Verification and remaining gates

Non-UI regressions live in `src/commonTest` and platform test sources: policy
clocks, ETag/304, cache bounds, cancellation, access scope, decoding and refresh.
No UI/instrumented Compose/screenshot suite is required or permitted by the current
repository rules. Existing tests must be selected for the affected platform;
compiling them is distinct from executing them.

Manual signed-device gates remain: long-scroll/viewer memory profiling,
background/resume and rapid account replacement, both directions of author
blocking, protected/private/home access against the real gateway, corrupt/EXIF/
alpha images, and measured transfer/decode/render behavior under constrained
network. Historical stutter/termination reports do not prove a leak or jetsam
without fresh diagnostics. No new device, backend or profiling acceptance is
claimed by this documentation update.
