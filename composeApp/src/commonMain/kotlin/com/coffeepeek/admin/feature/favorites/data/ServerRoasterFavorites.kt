package com.coffeepeek.admin.feature.favorites.data

import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.domain.feature.favorites.api.FavoriteKind
import com.coffeepeek.domain.feature.favorites.api.FavoriteEntry
import com.coffeepeek.domain.feature.favorites.api.FavoritesRepository
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.repository.RoasterRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal class ServerRoasterFavorites(
    private val favorites: FavoritesRepository,
    private val roasters: RoasterRepository,
) : RoasterFavorites {
    private val errors = MutableStateFlow<String?>(null)
    override fun observeErrors(): Flow<String?> = errors.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeFavorites(): Flow<List<CatalogItem>> = favorites.observeFavorites().mapLatest { result ->
        errors.value = if (result.isFailure) "Не удалось загрузить избранных обжарщиков" else null
        val entries = result.getOrDefault(emptyList()).filter { it.kind == FavoriteKind.Roaster }
        if (entries.isEmpty()) return@mapLatest emptyList()
        val catalog = roasters.getRoasters().getOrDefault(emptyList()).mapNotNull { roaster ->
            roaster.publicAddress?.slug?.let { it to roaster.toCatalogItem() }
        }.toMap()
        val requests = Semaphore(4)
        coroutineScope {
            entries.map { entry -> async {
                val slug = entry.address?.slug
                slug?.let(catalog::get)?.let { item -> item.copy(address = entry.address,
                    name = entry.name.ifBlank { item.name }, photoUrl = entry.coverPhotoUrl ?: item.photoUrl) } ?: run {
                    val details = slug?.let { requests.withPermit { roasters.getRoaster(it).getOrNull() } }
                    currentCoroutineContext().ensureActive()
                    CatalogItem(
                        id = slug ?: entry.id,
                        slug = slug.orEmpty(),
                        name = entry.name.ifBlank { details?.name ?: slug ?: "Обжарщик" },
                        photoUrl = entry.coverPhotoUrl ?: details?.photos?.firstOrNull()?.fullUrl,
                        address = entry.address,
                        description = details?.about,
                    )
                }
            } }.awaitAll()
        }
    }

    override suspend fun setFavorite(roaster: CatalogItem, isFavorite: Boolean): Result<Unit> {
        val address = roaster.address
            ?: return Result.failure(IllegalArgumentException("У обжарщика нет публичного адреса"))
        return favorites.setFavorite(FavoriteEntry(FavoriteKind.Roaster, address,
            name = roaster.name, coverPhotoUrl = roaster.photoUrl), isFavorite)
    }
}
