package com.coffeepeek.feature.favorites.data.repository

import com.coffeepeek.core.session.SessionStore
import com.coffeepeek.feature.favorites.data.backend.FavoriteEntryDto
import com.coffeepeek.feature.favorites.data.backend.FavoritesBackend
import com.coffeepeek.feature.favorites.domain.model.FavoriteAddress
import com.coffeepeek.feature.favorites.domain.model.FavoriteEntry
import com.coffeepeek.feature.favorites.domain.model.FavoriteKind
import com.coffeepeek.feature.favorites.domain.model.FavoriteTarget
import com.coffeepeek.feature.favorites.domain.repository.AccountFavorites
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** [client] is the authenticated API client; the account is the active session's user id, else its token. */
fun createAccountFavorites(client: HttpClient, sessions: SessionStore): AccountFavorites =
    RemoteAccountFavorites(FavoritesBackend(client), sessions)

internal class RemoteAccountFavorites(
    private val api: FavoritesBackend,
    private val sessions: SessionStore,
) : AccountFavorites {
    private val cache = ServerFavoritesCache(::currentOwner)
    private val mutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeFavorites(): Flow<Result<List<FavoriteEntry>>> = sessions.observeSession()
        .map { currentOwner() }
        .distinctUntilChanged()
        .flatMapLatest { account ->
            if (account == null) flowOf(Result.success(emptyList())) else flow {
                // Clear the previous account's presentation before awaiting this account's request.
                emit(Result.success(emptyList()))
                getFavorites()
                emitAll(cache.rows(account))
            }
        }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMemberships(): Flow<Result<Set<FavoriteTarget>>> = sessions.observeSession()
        .map { currentOwner() }.distinctUntilChanged().flatMapLatest { owner ->
            if (owner == null) flowOf(Result.success(emptySet())) else channelFlow {
                send(Result.success(emptySet()))
                launch { getFavorites() }
                cache.memberships(owner).collect { send(it) }
            }
        }.distinctUntilChanged()

    override fun cachedMembership(target: FavoriteTarget) = cache.cachedMembership(target)

    override fun applyConfirmedMembership(expectedAccount: String, expectedGeneration: Long, target: FavoriteTarget, favorite: Boolean) =
        cache.confirm(FavoriteCacheOwner(expectedAccount, expectedGeneration), target, favorite)

    override suspend fun getFavorites(refresh: Boolean): Result<List<FavoriteEntry>> = result {
        val account = activeOwner() ?: return@result emptyList()
        if (!refresh) cache.peek(account)?.let { return@result it }
        mutex.withLock { load(account, refresh) }
    }

    override suspend fun setFavorite(entry: FavoriteEntry, favorite: Boolean): Result<Unit> = result {
        val account = checkNotNull(activeOwner()) { "Войдите в аккаунт, чтобы сохранять избранное" }
        val slug = requireNotNull(entry.slug?.takeIf(String::isNotBlank)) { "У объекта нет идентификатора избранного" }
        mutex.withLock {
            load(account, refresh = false)
            requireAccount(account)
            api.setFavorite(entry.kind.apiValue, slug, favorite).getOrThrow()
            currentCoroutineContext().ensureActive()
            requireAccount(account)
            cache.recordWrite(account, entry, favorite)
        }
    }

    override fun clearCache() = cache.clear()

    private suspend fun load(account: FavoriteCacheOwner, refresh: Boolean): List<FavoriteEntry> {
        requireAccount(account)
        val saved = cache.peek(account)
        if (!refresh && saved != null) return saved
        repeat(2) { readRows(account)?.let { return it } }
        val error = IllegalStateException("Favorites changed during metadata loading")
        cache.fail(account, error)
        throw error
    }

    private suspend fun readRows(account: FavoriteCacheOwner): List<FavoriteEntry>? {
        val revision = cache.revision()
        try {
            val items = api.getFavorites().getOrThrow().mapIndexedNotNull { index, dto -> dto.toEntry(index) }.distinctBy { it.id }
            currentCoroutineContext().ensureActive()
            requireAccount(account)
            return cache.recordRows(account, items, revision)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            requireAccount(account)
            // A failed refresh doesn't replace already confirmed favorites with an empty list.
            cache.fail(account, error)
            throw error
        }
    }

    private fun currentOwner(): FavoriteCacheOwner? = sessions.peekSession()?.takeIf(sessions::isActiveSession)
        ?.let { FavoriteCacheOwner(it.userId ?: it.accessToken, sessions.accountGeneration()) }

    private suspend fun activeOwner(): FavoriteCacheOwner? {
        if (sessions.peekSession() == null) sessions.getSession()
        return currentOwner()
    }

    private fun requireAccount(expected: FavoriteCacheOwner) {
        if (currentOwner() != expected) throw CancellationException("Favorites account changed")
    }
}

private fun FavoriteEntryDto.toEntry(index: Int): FavoriteEntry? {
    val kind = when (kind) {
        "coffee_shop" -> FavoriteKind.CoffeeShop
        "roaster" -> FavoriteKind.Roaster
        "drink" -> FavoriteKind.Drink
        else -> return null
    }
    return FavoriteEntry(
        kind = kind,
        address = address?.let { FavoriteAddress(it.slug, it.canonicalPath, it.revision, it.isAlias) },
        drinkSlug = drink?.slug, name = name, createdAtUtc = createdAtUtc,
        coverPhotoUrl = coverPhoto?.let { photo -> photo.urls?.card?.takeIf(String::isNotBlank) ?: photo.fullUrl?.takeIf(String::isNotBlank) },
        occurrence = index,
    )
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
