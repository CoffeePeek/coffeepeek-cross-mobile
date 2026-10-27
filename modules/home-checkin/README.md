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

## User-controlled visibility

Home check-ins can be private or public by the author's explicit choice. The form
starts private and reuses the shop check-in's publication switch. The profile
keeps the combined history and its type filters; no extra feed filters are needed.

The client and backend share this contract:

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

Backend migrations `PublicHomeCheckIns` (Shops) and `HomeCheckInModerationSnapshots`
(Moderation) extend the earlier private-only implementation. Existing home entries
remain private. Deploy the updated Moderation consumer/schema before the Shops
publisher. Local contract tests do not establish that deployment has completed.

The existing feed transport adds a read-only recipe summary under `home`, with
`kind: "Home"`, `shop: null` and `rating: null`. Its legacy feed model remains the
owner of that projection; creation/editing and validation stay in this feature.
The shared timeline renders a home card and routes editing to the profile's home
editor. No new module dependency or feed filter is introduced.

If an older server omits or disagrees with a requested visibility, the client retains
the saved record ID and displays an explicit notice. It never reports publication
or automatically repeats creation. When editing an entry whose visibility was not
returned, the field stays omitted unless the user chooses a value.

## Form errors

Validated against CoffeePeek-NET `origin/main` at `3216c360` (2026-10-14):
`HomeCheckInInput`, `CheckIn.Home`, `DrinkSelection`, `ErrorResponse`, and the
global exception handler. Method, bean, dose, duration, rating, note, drink and
photos are optional. Supplied values still follow the server's bounds; timestamps
need an explicit offset and at most seven fractional digits. New drafts use
millisecond precision on both platforms.

Domain validation returns all invalid fields. Data translates both `ErrorResponse`
(`errorCode`, `message`, `errors`) and ASP.NET validation problem details. Older
home endpoints still return message-only errors: their exact messages are mapped
in data until the server supplies field keys. Raw exceptions, JSON conversion
details and stack traces never reach the form. Unrecognized failures receive a
safe general message; authentication and service failures are not field errors.

The editor shows a summary above the scrollable fields and inline explanations.
An edit revalidates only previously invalid fields that changed, preserving other
server errors and the entered draft. Tests cover current backend messages, model
binding paths, infrastructure failures, cancellation and correction of one field
without hiding the remaining errors.
