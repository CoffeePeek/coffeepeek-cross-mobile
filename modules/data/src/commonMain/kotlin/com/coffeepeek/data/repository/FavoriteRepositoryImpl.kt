package com.coffeepeek.data.repository

import com.coffeepeek.api.utils.requestResult
import com.coffeepeek.data.local.LocalFavoriteShopDto
import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import com.coffeepeek.room.DatabaseCore
import com.coffeepeek.room.repository.readSerializable
import com.coffeepeek.room.repository.saveSerializable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FavoriteRepositoryImpl(
    private val database: DatabaseCore,
    private val sessions: SessionRepository? = null,
) : FavoriteRepository {

    private val settings = database.settingRepository
    private val mutex = Mutex()

    override suspend fun getFavoriteIds(): Set<String> =
        mutex.withLock { readAll().map { it.id }.toSet() }

    override suspend fun isFavorite(shopId: String): Boolean =
        mutex.withLock { readAll().any { it.id == shopId } }

    override suspend fun getFavorites(): Result<List<CoffeeShopDetails>> = requestResult {
        mutex.withLock { readAll().map { it.toDomain() } }
    }

    override suspend fun addFavorite(shop: CoffeeShop, address: String?): Result<Unit> = requestResult {
        mutex.withLock {
            val key = currentKey() ?: error("Sign in to save favorites")
            val current = readAt(key).filterNot { it.id == shop.id }
            writeAt(key, listOf(LocalFavoriteShopDto.from(shop, address)) + current)
        }
    }

    override suspend fun removeFavorite(shopId: String): Result<Unit> = requestResult {
        mutex.withLock {
            val key = currentKey() ?: error("Sign in to change favorites")
            writeAt(key, readAt(key).filterNot { it.id == shopId })
        }
    }

    override suspend fun clearAll() = clearForUser(sessions?.getSession()?.userId)

    override suspend fun clearForUser(userId: String?) {
        mutex.withLock {
            if (sessions != null && !userId.isNullOrBlank()) writeAt("$KEY:$userId", emptyList())
            writeAt(KEY, emptyList())
        }
    }

    private suspend fun currentKey(): String? {
        if (sessions == null) return KEY
        val owner = sessions.getSession()?.userId?.takeIf(String::isNotBlank) ?: return null
        val key = "$KEY:$owner"
        withContext(NonCancellable) {
            val old = settings.read(KEY)
            if (old != null) {
                if (settings.read(key) == null) settings.save(old.copy(key = key))
                settings.delete(KEY)
            }
        }
        return key
    }

    private suspend fun readAll(): List<LocalFavoriteShopDto> = currentKey()?.let { readAt(it) }.orEmpty()
    private suspend fun readAt(key: String): List<LocalFavoriteShopDto> =
        settings.readSerializable<List<LocalFavoriteShopDto>>(key).orEmpty()

    private suspend fun writeAt(key: String, items: List<LocalFavoriteShopDto>) {
        settings.saveSerializable(key, items.takeIf { it.isNotEmpty() })
    }

    private companion object {
        const val KEY = "local_favorite_shops"
    }
}
