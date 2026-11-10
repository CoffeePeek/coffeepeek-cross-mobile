# Favorites: server-backed domain and data

The favorites **screen** was removed (2026-11-06): the route `Screen.Favorites` had no
callers, the Android destination opened the collections library, and the old list UI
(`feature/favorites/impl`, `FavoritesEntry`, the drinks list) was unreachable. Historical
plans below (IOS_MIGRATION_PLAN, ANDROID_COMPLETION_PLAN) describe the removed screen.

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
