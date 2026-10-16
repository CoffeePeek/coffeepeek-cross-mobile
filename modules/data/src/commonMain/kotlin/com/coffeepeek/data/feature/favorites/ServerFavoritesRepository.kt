package com.coffeepeek.data.feature.favorites

import com.coffeepeek.api.feature.favorites.FavoritesApiService
import com.coffeepeek.api.model.response.shop.variantOr
import com.coffeepeek.data.mapper.toDomain
import com.coffeepeek.domain.feature.favorites.api.FavoriteEntry
import com.coffeepeek.domain.feature.favorites.api.FavoriteKind
import com.coffeepeek.domain.feature.favorites.api.FavoritesRepository
import com.coffeepeek.domain.model.PublicAddress
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ServerFavoritesRepository(
    private val api: FavoritesApiService,
    private val sessions: SessionRepository,
) : FavoritesRepository {
    private data class Cache(val account: String?, val result: Result<List<FavoriteEntry>>? = null)
    private val cache = MutableStateFlow(Cache(null))
    private val mutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeFavorites(): Flow<Result<List<FavoriteEntry>>> = sessions.observeSession()
        .map { account() }
        .distinctUntilChanged()
        .flatMapLatest { account ->
            if (account == null) flowOf(Result.success(emptyList())) else flow {
                // Clear the previous account's presentation before awaiting this account's request.
                emit(Result.success(emptyList()))
                getFavorites()
                emitAll(cache.filter { it.account == account }.map { it.result ?: Result.success(emptyList()) })
            }
        }.distinctUntilChanged()

    override suspend fun getFavorites(refresh: Boolean): Result<List<FavoriteEntry>> = result {
        val account = activeAccount() ?: return@result emptyList()
        mutex.withLock { load(account, refresh) }
    }

    override suspend fun setFavorite(kind: FavoriteKind, address: PublicAddress, favorite: Boolean): Result<Unit> =
        setFavorite(FavoriteEntry(kind, address), favorite)

    override suspend fun setFavorite(entry: FavoriteEntry, favorite: Boolean): Result<Unit> = result {
        val account = checkNotNull(activeAccount()) { "Войдите в аккаунт, чтобы сохранять избранное" }
        val slug = requireNotNull(entry.slug?.takeIf(String::isNotBlank)) { "У объекта нет идентификатора избранного" }
        mutex.withLock {
            val items = load(account, refresh = false)
            requireAccount(account)
            api.setFavorite(entry.kind.apiValue, slug, favorite).getOrThrow()
            currentCoroutineContext().ensureActive()
            requireAccount(account)
            val existing = items.firstOrNull { it.kind == entry.kind && it.slug == slug }
            val remaining = items.filterNot { it.kind == entry.kind && it.slug == slug }
            cache.value = Cache(account, Result.success(if (favorite) listOf(existing ?: entry) + remaining else remaining))
        }
    }

    override fun clearCache() { cache.value = Cache(null) }

    private suspend fun load(account: String, refresh: Boolean): List<FavoriteEntry> {
        requireAccount(account)
        val saved = cache.value.takeIf { it.account == account }?.result?.getOrNull()
        if (!refresh && saved != null) return saved
        try {
            val items = api.getFavorites().getOrThrow().mapIndexedNotNull { index, dto ->
                val kind = when (dto.kind) {
                    "coffee_shop" -> FavoriteKind.CoffeeShop
                    "roaster" -> FavoriteKind.Roaster
                    "drink" -> FavoriteKind.Drink
                    else -> return@mapIndexedNotNull null
                }
                FavoriteEntry(
                    kind = kind, address = dto.address?.toDomain(), drinkSlug = dto.drink?.slug,
                    name = dto.name, createdAtUtc = dto.createdAtUtc,
                    coverPhotoUrl = dto.coverPhoto?.let { it.urls.variantOr(it.fullUrl) { urls -> urls.card } },
                    occurrence = index,
                )
            }.distinctBy { it.id }
            currentCoroutineContext().ensureActive()
            requireAccount(account)
            cache.value = Cache(account, Result.success(items))
            return items
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            requireAccount(account)
            // A failed refresh doesn't replace already confirmed favorites with an empty list.
            if (saved == null) cache.value = Cache(account, Result.failure(error))
            throw error
        }
    }

    private fun account(): String? = sessions.peekSession()?.takeIf(sessions::isActiveSession)
        ?.let { it.userId ?: it.accessToken }

    private suspend fun activeAccount(): String? {
        if (sessions.peekSession() == null) sessions.getSession()
        return account()
    }

    private fun requireAccount(expected: String) {
        if (account() != expected) throw CancellationException("Favorites account changed")
    }
}

private val FavoriteKind.apiValue: String get() = when (this) {
    FavoriteKind.CoffeeShop -> "coffee_shop"
    FavoriteKind.Roaster -> "roaster"
    FavoriteKind.Drink -> "drink"
}

private suspend inline fun <T> result(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Result.failure(error)
}
