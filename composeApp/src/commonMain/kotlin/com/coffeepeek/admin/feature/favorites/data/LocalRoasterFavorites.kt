package com.coffeepeek.admin.feature.favorites.data

import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.admin.feature.favorites.api.roasterFavoriteId
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.PublicAddress
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.room.repository.SettingRepository
import com.coffeepeek.room.repository.readSerializable
import com.coffeepeek.room.repository.readSerializableFlow
import com.coffeepeek.room.repository.saveSerializable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

internal class LocalRoasterFavorites(
    private val settings: SettingRepository,
    private val sessions: SessionRepository,
) : RoasterFavorites {
    private val mutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeFavorites(): Flow<List<CatalogItem>> = sessions.observeSession()
        .map { session -> session?.userId?.takeIf { sessions.isActiveSession(session) } }
        .distinctUntilChanged()
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(emptyList())
            } else {
                settings.readSerializableFlow<List<SavedRoaster>>(key(userId))
                    .map { saved -> saved.orEmpty().map(SavedRoaster::toDomain) }
            }
        }

    override suspend fun setFavorite(roaster: CatalogItem, isFavorite: Boolean): Result<Unit> = runCatching {
        mutex.withLock {
            val session = sessions.getSession()
            check(sessions.isActiveSession(session)) { "Войдите в аккаунт, чтобы сохранять обжарщиков" }
            val userId = requireNotNull(session?.userId)
            val id = roaster.roasterFavoriteId
            require(id.isNotBlank())
            val key = key(userId)
            val remaining = settings.readSerializable<List<SavedRoaster>>(key).orEmpty()
                .filterNot { it.id == id }
            val updated = if (isFavorite) listOf(SavedRoaster.from(roaster)) + remaining else remaining
            settings.saveSerializable(key, updated.takeIf { it.isNotEmpty() })
        }
    }

    private fun key(userId: String) = "local_favorite_roasters:$userId"
}

@Serializable
private data class SavedRoaster(
    val id: String,
    val name: String,
    val photoUrl: String? = null,
    val canonicalPath: String? = null,
    val revision: Int = 0,
    val isAlias: Boolean = false,
    val coffeeShopsCount: Int = 0,
    val coffeeProductsCount: Int = 0,
    val availableCoffeeProducts: Int = 0,
    val tags: List<SavedRoaster> = emptyList(),
    val description: String? = null,
    val sortOrder: Int = 0,
) {
    fun toDomain(): CatalogItem = CatalogItem(
        id = id,
        name = name,
        slug = id,
        photoUrl = photoUrl,
        address = canonicalPath?.let { PublicAddress(id, it, revision, isAlias) },
        coffeeShopsCount = coffeeShopsCount,
        coffeeProductsCount = coffeeProductsCount,
        availableCoffeeProducts = availableCoffeeProducts,
        tags = tags.map(SavedRoaster::toDomain),
        description = description,
        sortOrder = sortOrder,
    )

    companion object {
        fun from(roaster: CatalogItem): SavedRoaster = SavedRoaster(
            id = roaster.roasterFavoriteId,
            name = roaster.name,
            photoUrl = roaster.photoUrl,
            canonicalPath = roaster.address?.canonicalPath,
            revision = roaster.address?.revision ?: 0,
            isAlias = roaster.address?.isAlias ?: false,
            coffeeShopsCount = roaster.coffeeShopsCount,
            coffeeProductsCount = roaster.coffeeProductsCount,
            availableCoffeeProducts = roaster.availableCoffeeProducts,
            tags = roaster.tags.map(::from),
            description = roaster.description,
            sortOrder = roaster.sortOrder,
        )
    }
}
