# Favorites: server-backed domain and data

Reviewed against `develop` on 2026-11-27. Completed Android/iOS screen migration
plans have been removed; their dated verification remains available in Git.

The favorites **screen** was removed (2026-11-06): the route `Screen.Favorites` had no
callers, the Android destination opened the collections library, and the old list UI
(`feature/favorites/impl`, `FavoritesEntry`, the drinks list) was unreachable.

What remains and why:

- `domain` + `data` — the server favorites store that the app still reads and writes:
  shop favorites (`FavoritesRepository`/`ServerFavoriteShopsRepository`), roaster
  favorites (`FavoriteRoastersRepository`/`ServerRoasterFavorites`, hearts on roaster
  cards), favorite id observation for collection pickers, and the membership cache.
- `FavoriteKind.Drink` and drink rows stay in the model: the server favorites store
  contains drink rows and the shared parser must keep reading them. There is no UI
  listing favorite drinks on any platform.
- "Favorites" for users live in the collections library (profile → collections, private
  "Избранное" collection); the Android heart flow opens the collection picker.

Do not reintroduce a favorites screen or a local favorites writer: the server is
authoritative, logout never deletes server favorites (see AGENTS.md).

The populated Gradle boundaries are [domain](domain/src/commonMain) and
[data](data/src/commonMain); no Favorites API/impl screen modules exist.
Composition supplies the configured authenticated client and session identity.
Data owns DTOs, membership caching, account-bound loading and server writes;
callers integrate through their own public contracts/adapters. Old responses
must not overwrite a replacement account's membership. Logout/cache clearing
must not upload old local rows or delete server favorites.

Existing domain and data unit/contract suites cover ID observation, cache
behavior, account changes and backend parsing. See
[Collections](../collections/README.md) for the active library and membership
picker, including nested create/cancel behavior. Behavioral changes require
affected caller tests and Android/Native build gates; manual guest/sign-in,
account replacement and favorite synchronization remain separate acceptance.
This documentation review performed no build, device action or favorite write.
