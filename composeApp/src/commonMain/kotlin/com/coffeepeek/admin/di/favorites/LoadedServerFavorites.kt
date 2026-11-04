package com.coffeepeek.admin.di.favorites

import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.domain.feature.favorites.api.FavoriteEntry
import com.coffeepeek.domain.feature.favorites.api.FavoritesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Raw observation also emits account-clearing signals; only a completed read is a settled list. */
internal fun observeLoadedServerFavorites(server: FavoritesRepository, sessions: SessionReader? = null,
    account: String? = sessions?.favoritesAccountId()): Flow<Result<List<FavoriteEntry>>> =
    server.observeFavorites().map { signal -> loadedServerFavorites(server, signal, sessions, account) }.distinctUntilChanged()

private suspend fun loadedServerFavorites(server: FavoritesRepository, signal: Result<List<FavoriteEntry>>,
    sessions: SessionReader?, account: String?): Result<List<FavoriteEntry>> {
    requireFavoritesAccount(sessions, account)
    signal.exceptionOrNull()?.let { if (it is CancellationException) throw it }
    if (signal.isFailure) return signal
    if (sessions != null && account == null) return Result.success(emptyList())
    val loaded = server.getFavorites()
    loaded.exceptionOrNull()?.let { if (it is CancellationException) throw it }
    currentCoroutineContext().ensureActive()
    requireFavoritesAccount(sessions, account)
    return loaded
}

internal fun SessionReader.favoritesAccountId(): String? =
    peekSession()?.takeIf(this::isActiveSession)?.userId?.takeIf(String::isNotBlank)

internal fun requireFavoritesAccount(sessions: SessionReader?, account: String?) {
    if (sessions != null && sessions.favoritesAccountId() != account) throw CancellationException("Favorites account changed")
}
