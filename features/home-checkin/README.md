# Home check-ins

Owns the home preparation domain contract, validation, HTTP implementation,
and photo upload flow. Only application composition may construct the repository;
UI consumes its domain interface. It may depend on shared network infrastructure
and Ktor, but has no dependency on shop check-ins or the legacy domain/data/network
modules. DTOs and the Ktor implementation remain in `data` and are not exposed.

A separate module is needed because the application composition module must not
own feature business rules or HTTP representations, and the legacy layer modules
are not the destination for new features. The existing image picker and navigation
remain in the application during this incremental migration.

## User-controlled visibility: client integration and backend gap

Home check-ins can be private or public by the author's explicit choice. The form
starts private and reuses the shop check-in's publication switch. The profile
keeps the combined history and its type filters; no extra feed filters are needed.

The client now targets this extension to the existing home endpoints:

- `POST /api/v1/check-ins/home`: `visibility: "Private" | "Public"`, default Private.
- `PUT /api/v1/check-ins/home/{id}`: the same field; omission must preserve visibility.
  Apply content and visibility together so publishing does not need a second request.
- Create, update, history and detail return `visibility` and `moderationState` using
  the same values as shop check-ins. Public does not mean approved: show Pending
  separately, and show "Виден всем" only for an approved public entry.
- The server must enforce owner-only mutation, allow reading approved public home
  entries/photos, and remove access promptly when hidden/deleted or under re-review.
- Public feed items must identify `kind: "Home"`, support a missing shop and carry
  preparation fields. Home ratings must not contribute to coffee-shop ratings.

**This extension is not implemented in the inspected backend branch.** In
`C:/Users/goloo/VSCodeProjects/CoffeePeek-NET`, local `main` has no home endpoints;
`codex/home-checkins` at `3af49e02` contains the private-only implementation:

- `CheckIn.Home.cs` always creates Private entries; domain visibility changes reject Home.
- `CK_CanonicalCheckIns_KindShape` requires Home visibility/moderation to stay private.
  Change it through a new migration, preserving existing entries as Private.
- `CheckInEligibility`, feed projections, moderation snapshots and photo authorization
  exclude home publication or assume a shop. Extend them together, with tests for
  publish → moderate → read → hide, stale moderation decisions and photo access.
- Home request/response DTOs have no visibility fields.

These mobile contract tests describe the intended extension, not a verified deployed
API. If a server omits or disagrees with a requested visibility, the client retains
the saved record ID and displays an explicit notice. It never reports publication
or automatically repeats creation. When editing an entry whose visibility was not
returned, the field stays omitted unless the user chooses a value.
