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
| Add a place | Use the shop/roaster action and collections picker. Membership does not require a prior legacy favorite. |
| Confirm the picker | Apply only the user's added/removed marks against current server memberships. Keep unrelated changes from another device. |
| Edit collection metadata | Preserve memberships unless the user explicitly reordered/removed places in this editor. |
| Change memberships, delete a collection or edit its items | Invalidate owner collection screens and refresh the server favorite projection used by hearts. |
| Remove a heart | Server removes the place from all owned active lists and the legacy favorite projection. |
| Cancel a picker | Do not write memberships; retain the already completed legacy heart action. |

The backend `IsFavorite` and owner favorites reads include membership in any
owned active collection, regardless of visibility. Shared/public profile APIs
must not expose private collection membership. Legacy drinks remain outside the
shop/roaster collection contract.

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

Server changes are prepared/published on backend `develop`, not deployed to
production by this task. Until deployed, the mobile compatibility path still
works, but the old server can only consume the rebased full membership set;
atomic delta application and server default uniqueness require the new server.

UI uses the shared shimmer for section/photo loading, a neutral image fallback,
and no visible bag labels. Collection captions sit on the photo above a fading
blur/dark gradient. New collection editing contains cover with an edit control,
name, description and visibility; existing collections retain item management.
