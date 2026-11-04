# Favorites and collections flow — 2026-11-04

This correction follows the user's current instruction to work directly on
`develop`. The default collection starts private, but publishing it changes the
same collection; the older requirement to recreate an always-private copy is
superseded.

| Action | Result |
| --- | --- |
| Open collections for the first time | Ensure one server-marked default collection; use the title-based compatibility path only when the new endpoint returns 404. |
| Publish or rename the default | Keep its ID, items and default marker. Do not create another private row. |
| Create a collection | Save cover, name, description and visibility with no places. Cover upload failure does not create an empty orphan. |
| Tap a shop/roaster heart | Open the collections picker with current memberships, including when the heart is already selected. Do not mutate favorites before confirmation. |
| Add a place | Select its collections in the picker. Membership does not require a prior legacy favorite. |
| Confirm the picker | Apply only the user's added/removed marks against current server memberships. Keep unrelated changes from another device. |
| Edit collection metadata | Preserve memberships unless the user explicitly reordered/removed places in this editor. |
| Change memberships, delete a collection or edit its items | Invalidate owner collection screens and refresh the server favorite projection used by hearts. |
| Remove a heart | Choose which collection memberships to remove in the picker; a remaining membership keeps the heart selected. |
| Cancel a picker | Do not write memberships or change the heart. |

The backend `IsFavorite` and owner favorites reads include membership in any
owned active collection, regardless of visibility. Shared/public profile APIs
must not expose private collection membership. Legacy drinks remain outside the
shop/roaster collection contract.

The legacy server favorite-delete endpoint still removes all memberships for
older clients; current Android shop/roaster taps use the picker instead.

The default endpoint adopts one existing exact-title `Избранное` when possible;
its stable marker survives later title/visibility edits. Existing duplicate rows
are preserved. Automatically merging private places into a public duplicate
would publish data without an explicit choice, so this patch does not do that.
Legacy private favorites are seeded into a newly adopted private default only
once; a public default is never populated implicitly with private legacy data.

The Android legacy Favorites destination now opens the collection library, so
it no longer exposes the old snapshot composer as a second creation flow.
Individual collection sharing remains available. The native iOS legacy writer
and its snapshot entry are unchanged under the repository's Android-first rule.

Current Android heart prompts cover both selected and unselected hearts in
migrated shop/roaster surfaces and the legacy feed/search/favorites/detail paths.
Confirmation analytics stay in application composition, use the existing consent
and account operation scope, and compare the server union before/after the write.
Picker cancel and list metadata edits do not report a favorite change.

Server changes are prepared/published on backend `develop`, not deployed to
production by this task. Until deployed, the mobile compatibility path still
works, but the old server can only consume the rebased full membership set;
atomic delta application and server default uniqueness require the new server.
Backend `develop` checkpoint `14d0fe7b` passed [CI run 41824818571](https://github.com/CoffeePeek/coffeepeek-backend/actions/runs/41824818571),
including mandatory PostgreSQL 17 migration/discovery/profile concurrency checks
and secret scanning. Image publishing and deployment jobs were skipped.
The initial CI attempts failed first on the direct-store transaction contract
(`41820674689`) and then on a historical-schema fixture calling the current
writer (`41822170161`). Follow-up commits preserve ambient transactions and
seed that older schema directly; the green run above includes both corrections.

UI uses the shared shimmer for section/photo loading, a neutral image fallback,
and no visible bag labels. Collection captions sit on the photo above a fading
blur/dark gradient. New collection editing contains cover with an edit control,
name, description and visibility; existing collections retain item management.
