# Favorites

The current account's favorites are stored on the server using the existing web
contract: `GET /api/v1/favorites`, `PUT` / `DELETE`
`/api/v1/favorites/{coffee_shop|roaster|drink}/{slug}`. Mutations have no request body
and return 204. The updated discovery contract supplies kind, name, nullable
cover photo, nullable public address, nullable drink reference, and creation time.
Drink slugs retain underscores and do not imply a public address or detail route.
Entries without a public address remain visible using the server's name/photo.

`modules/domain/.../feature/favorites/api` publishes only the account favorites
contract. `modules/network/.../feature/favorites` owns its transport, and
`modules/data/.../feature/favorites` owns the server state and account isolation.
The existing shop repository is a compatibility adapter; `ServerRoasterFavorites`
adapts the same shared state for roaster and favorites presentation, hydrating
public metadata through the existing roaster repository. No new Gradle module
is needed for these package boundaries.

Only confirmed server mutations update the in-memory list. Signing out clears
device memory without deleting server favorites. Local-only snapshots from older
versions are not uploaded automatically, because they may belong to a different
account. Public profile favorites use their own public user endpoint: this endpoint
always belongs to the signed-in account. Identity includes kind so a shop,
roaster and drink sharing a slug cannot overwrite one another.
