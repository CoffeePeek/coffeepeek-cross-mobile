package com.coffeepeek.admin.di.favorites

import com.coffeepeek.feature.favorites.domain.model.FavoriteKind
import com.coffeepeek.feature.favorites.domain.repository.AccountFavorites
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.favorites.domain.model.FavoriteDrink
import com.coffeepeek.feature.favorites.domain.repository.FavoriteDrinksRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal class ServerFavoriteDrinksRepository(
    private val server: AccountFavorites,
    private val sessions: SessionReader,
) : FavoriteDrinksRepository {
    override fun observe(): Flow<Result<List<FavoriteDrink>>> = channelFlow {
        var observation: Job? = null
        var firstOwner = true
        try {
            sessions.observeSession().map { sessions.favoritesAccountId() }.distinctUntilChanged().collect { account ->
                observation?.cancel()
                if (!firstOwner || account == null) send(Result.success(emptyList()))
                firstOwner = false
                if (account != null) observation = launch {
                    drinks(account).collect { result ->
                        currentCoroutineContext().ensureActive()
                        requireFavoritesAccount(sessions, account)
                        send(result)
                    }
                }
            }
        } finally { observation?.cancel() }
    }.buffer(Channel.RENDEZVOUS)

    private fun drinks(account: String): Flow<Result<List<FavoriteDrink>>> =
        observeLoadedServerFavorites(server, sessions, account).map { result ->
            requireFavoritesAccount(sessions, account)
            result.map { entries -> entries.filter { it.kind == FavoriteKind.Drink }.map {
                FavoriteDrink(it.id, it.name, it.coverPhotoUrl, !it.drinkSlug.isNullOrBlank())
            } }.also { requireFavoritesAccount(sessions, account) }
        }

    override suspend fun remove(id: String): Result<Unit> = try {
        val account = sessions.peekSession()?.takeIf(sessions::isActiveSession)?.userId
            ?: error("Sign in to change favorites")
        val entry = server.getFavorites().getOrThrow().firstOrNull { it.kind == FavoriteKind.Drink && it.id == id }
        currentCoroutineContext().ensureActive()
        if (sessions.peekSession()?.userId != account) throw CancellationException("Favorites account changed")
        if (entry != null) server.setFavorite(entry, false).getOrThrow()
        currentCoroutineContext().ensureActive()
        if (sessions.peekSession()?.userId != account) throw CancellationException("Favorites account changed")
        Result.success(Unit)
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (error: Exception) { Result.failure(error) }
}
