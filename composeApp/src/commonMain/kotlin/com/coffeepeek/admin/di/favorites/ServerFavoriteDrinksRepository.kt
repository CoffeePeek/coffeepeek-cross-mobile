package com.coffeepeek.admin.di.favorites

import com.coffeepeek.domain.feature.favorites.api.FavoriteKind
import com.coffeepeek.domain.feature.favorites.api.FavoritesRepository
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.favorites.domain.model.FavoriteDrink
import com.coffeepeek.feature.favorites.domain.repository.FavoriteDrinksRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.map

internal class ServerFavoriteDrinksRepository(
    private val server: FavoritesRepository,
    private val sessions: SessionReader,
) : FavoriteDrinksRepository {
    override fun observe() = server.observeFavorites().map { result ->
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        result.map { entries ->
        entries.filter { it.kind == FavoriteKind.Drink }.map {
            FavoriteDrink(it.id, it.name, it.coverPhotoUrl, !it.drinkSlug.isNullOrBlank())
        }
        }
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
