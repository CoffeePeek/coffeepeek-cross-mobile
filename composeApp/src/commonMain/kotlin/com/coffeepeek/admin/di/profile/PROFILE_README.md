# Public profiles

Profile models, the `PublicProfileData` contract and the HTTP data layer now live in
`feature/profile/{domain,data}` (migration step 2, see `feature/profile/LEGACY_INVENTORY.md`). This package keeps the UI, the
check-in list (it still uses the legacy timeline `CheckIn`) and the adapters that answer the profile's ports from legacy
repositories. Root composition owns its routes and DI. HTTP DTOs stay in the data module.

`GET /api/Users/{slug}` supplies the nickname, biography, avatar, all four
statistics and the viewer's following state. The server's public address gives
the canonical author slug and share path. The authenticated gateway client
supports both anonymous public reads and personalized following state.

Following uses the JWT-protected PUT/DELETE `/api/Users/{slug}/follow` contract.
Its direct response is authoritative for the flag and follower count. Guests
are directed to login. Account changes cancel outstanding requests and reload
viewer-specific state; a late response cannot overwrite a new viewer's state.

Public favorites are read from `/api/v1/profiles/{slug}/favorites`. All server
entries, including drinks, retain their supplied names, photo variants and
public entity references. Optional real shop/roaster details enrich catalog
cards with bounded concurrency. Failure to enrich an item preserves the public
entry. Unknown opening status stays unknown. Own-account favorites are never
used as fallback data.

Activity uses `/api/v1/profiles/{slug}/activity` with the selected year, month
and `Europe/Minsk` time zone. All day counts come from the server. Switching
months cancels the old request and clears the old grid. Followers and following
use the public paginated `/api/Users/{slug}/followers` and `/following` routes.
Favorites, activity and connection lists have independent error/retry states.

Check-ins reuse the existing public feed repository with the canonical author
filter; only matching public, approved entries are displayed. No private diary
or current-account profile data is used. The legacy feed abstraction remains
a temporary integration boundary until that feature publishes its own API.
