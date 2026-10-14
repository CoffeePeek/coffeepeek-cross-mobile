# Home check-in form and shared brew catalog

The home form uses `feature/catalog/api/BrewMethodCatalog`, backed by the existing
shop catalog cache, and the same `brewMethodIcon` as shop details. It sends the
catalog slug as `brewMethodCode`. Historical home codes remain editable and map to
the current catalog names in the selector.

New entries start with taste score 5; editing preserves the recorded score.
Bean selection loads the first catalog page on opening and searches after 300 ms.
Cancelled responses cannot overwrite newer results. Empty results show the search
mascot centered in the available results area. Duration is entered as minutes and
seconds and sent as total seconds. Custom drinks send `drinkSlug = "other"` together
with `customDrinkName`, matching `DrinkSelection.ResolveAsync` on the server.

## Server rollout dependency

Deploy the backend change in branch `codex/home-brew-catalog` before releasing the
mobile catalog selector. Its working copy is
`C:/Users/User/RiderProjects/CoffeePeek-NET-home-brew-catalog`.

Apply both migrations through the normal backend deployment process:

- Shops: `20261014073826_HomeBrewMethodCatalog`
- Moderation: `20261014073836_HomeBrewMethodCatalog`

The server validates new method slugs against the public shop catalog, accepts old
mobile codes, and preserves an unchanged historical method on edit. The migrations
widen the stored code to the catalog's 100-character limit and replace the fixed
home-method database whitelist. Existing rows are not rewritten. Rollback refuses
to discard incompatible new values. Neither deployment nor database updates were
performed as part of this change.

## Verification

- Android and iOS Kotlin compilation.
- App unit tests, including initial rating, catalog loading/debounce, cancelled
  requests, edit preservation, and minute/second conversion.
- Home feature HTTP contract tests for create/edit, catalog slugs, custom drinks,
  and duration serialization.
- Two Android instrumentation scenarios on Pixel_8: creation/form controls and
  existing home-feed actions.
- Backend home-check-in application, domain and query tests; migration SQL generated
  and inspected. Live database migration and iOS runtime UI were not exercised.
