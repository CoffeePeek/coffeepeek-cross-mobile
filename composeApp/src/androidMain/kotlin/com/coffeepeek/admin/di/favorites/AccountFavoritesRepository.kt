package com.coffeepeek.admin.di.favorites

import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.favorites.data.local.FavoritesStorage
import com.coffeepeek.feature.favorites.data.repository.createFavoritesRepository
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Composition adapter: account identity belongs to the application session, not favorites domain. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class AccountFavoritesRepository(
    private val storage: FavoritesStorage,
    private val sessions: SessionReader,
    private val dispatcher: CoroutineDispatcher,
) : FavoritesRepository {
    private val mutex = Mutex()
    private val repositories = mutableMapOf<String, FavoritesRepository>()

    override fun observe(): Flow<Result<List<FavoriteShop>>> = sessions.observeSession()
        .map { it?.userId?.takeIf(String::isNotBlank) }.distinctUntilChanged()
        .flatMapLatest { owner ->
            if (owner == null) flowOf(Result.success(emptyList()))
            else flow {
                // Clear the old account's cards even if the new account's storage cannot be read.
                emit(Result.success(emptyList()))
                emitAll(repository(owner).observe())
            }.catch { error ->
                if (error is CancellationException) throw error
                emit(Result.failure(error))
            }
        }

    override suspend fun read(): Result<List<FavoriteShop>> = result {
        val account = owner() ?: return@result emptyList()
        val items = repository(account).read().getOrThrow()
        if (owner() == account) items else emptyList()
    }

    override suspend fun save(shop: FavoriteShop): Result<Unit> = result {
        val account = owner() ?: error("Sign in to save favorites")
        repository(account).save(shop).getOrThrow()
    }

    override suspend fun remove(shopId: String): Result<Unit> = result {
        val account = owner() ?: error("Sign in to change favorites")
        repository(account).remove(shopId).getOrThrow()
    }

    override suspend fun clear(): Result<Unit> = clearForUser(owner())

    /** Cleanup captures the old owner before publishing a null session. */
    suspend fun clearForUser(owner: String?): Result<Unit> = result {
        owner?.takeIf(String::isNotBlank)?.let { repository(it).clear().getOrThrow() }
        // Unowned pre-migration rows must not be inherited by the next login.
        mutex.withLock { storage.write(LEGACY_KEY, null) }
    }

    private suspend fun owner(): String? = sessions.getSession()?.userId?.takeIf(String::isNotBlank)

    private suspend fun repository(owner: String): FavoritesRepository = mutex.withLock {
        repositories.getOrPut(owner) {
            createFavoritesRepository(storage, dispatcher, "$LEGACY_KEY:$owner")
        }.also {
            // AuthRepository clears unowned rows before changing users. Existing signed-in users
            // retain their saved data; copy before delete also survives process interruption.
            withContext(NonCancellable) {
                val legacy = storage.read(LEGACY_KEY)
                if (legacy != null) {
                    val key = "$LEGACY_KEY:$owner"
                    if (storage.read(key) == null) storage.write(key, legacy)
                    storage.write(LEGACY_KEY, null)
                }
            }
        }
    }

    private companion object { const val LEGACY_KEY = "local_favorite_shops" }
}

private suspend inline fun <T> result(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (error: CancellationException) {
    throw error
} catch (error: Exception) {
    Result.failure(error)
}
