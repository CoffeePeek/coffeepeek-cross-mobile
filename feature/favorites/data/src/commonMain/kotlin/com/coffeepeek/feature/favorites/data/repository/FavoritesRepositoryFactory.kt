package com.coffeepeek.feature.favorites.data.repository

import com.coffeepeek.feature.favorites.data.local.FavoritesStorage
import com.coffeepeek.feature.favorites.data.local.model.StoredFavorite
import com.coffeepeek.feature.favorites.data.mapper.toDomain
import com.coffeepeek.feature.favorites.data.mapper.toStored
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Share one instance per storage/session: its mutex coordinates read-modify-write. */
fun createFavoritesRepository(storage: FavoritesStorage, dispatcher: CoroutineDispatcher): FavoritesRepository =
    StoredFavoritesRepository(storage, dispatcher)

private class StoredFavoritesRepository(
    private val storage: FavoritesStorage,
    private val dispatcher: CoroutineDispatcher,
) : FavoritesRepository {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val key = "local_favorite_shops"

    override fun observe(): Flow<Result<List<FavoriteShop>>> = flow {
        emitAll(storage.observe(key).map { raw -> result { decode(raw).map(StoredFavorite::toDomain) } })
    }
        .distinctUntilChanged()
        .flowOn(dispatcher)
        .catch { error ->
            if (error is CancellationException) throw error
            emit(Result.failure(error))
        }

    override suspend fun read(): Result<List<FavoriteShop>> = result {
        withContext(dispatcher) { mutex.withLock { decode(storage.read(key)).map(StoredFavorite::toDomain) } }
    }

    override suspend fun save(shop: FavoriteShop): Result<Unit> = result {
        require(shop.id.isNotBlank()) { "Favorite ID must not be blank" }
        withContext(dispatcher) { mutex.withLock {
            val current = decode(storage.read(key)).filterNot { it.id == shop.id }
            write(listOf(shop.toStored()) + current)
        } }
    }

    override suspend fun remove(shopId: String): Result<Unit> = result {
        require(shopId.isNotBlank()) { "Favorite ID must not be blank" }
        withContext(dispatcher) { mutex.withLock { write(decode(storage.read(key)).filterNot { it.id == shopId }) } }
    }

    override suspend fun clear(): Result<Unit> = result {
        withContext(dispatcher) { mutex.withLock { storage.write(key, null) } }
    }

    private fun decode(raw: String?): List<StoredFavorite> = when {
        raw.isNullOrBlank() || raw.trim() == "null" -> emptyList()
        else -> json.decodeFromString<List<StoredFavorite>>(raw).distinctBy { it.id }
    }

    private suspend fun write(items: List<StoredFavorite>) {
        storage.write(key, if (items.isEmpty()) null else json.encodeToString(items))
    }
}

private suspend inline fun <T> result(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (error: CancellationException) {
    throw error
} catch (error: Exception) {
    Result.failure(error)
}
