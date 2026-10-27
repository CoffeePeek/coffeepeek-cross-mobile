package com.coffeepeek.admin.feature.favorites.data

import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.admin.di.favorites.toFavoriteAddress
import com.coffeepeek.admin.di.favorites.toPublicAddress
import com.coffeepeek.feature.favorites.domain.model.FavoriteEntry
import com.coffeepeek.feature.favorites.domain.model.FavoriteKind
import com.coffeepeek.feature.favorites.domain.repository.AccountFavorites
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
    private val favorites: AccountFavorites,
    private val roasters: RoasterRepository,
    private val sessions: com.coffeepeek.core.session.SessionReader? = null,
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
                slug?.let(catalog::get)?.let { item -> item.copy(address = entry.address?.toPublicAddress(),
                    name = entry.name.ifBlank { item.name }, photoUrl = entry.coverPhotoUrl ?: item.photoUrl) } ?: run {
                    val details = slug?.let { requests.withPermit { roasters.getRoaster(it).getOrNull() } }
                    currentCoroutineContext().ensureActive()
                    CatalogItem(
                        id = slug ?: entry.id,
                        slug = slug.orEmpty(),
                        name = entry.name.ifBlank { details?.name ?: slug ?: "Обжарщик" },
                        photoUrl = entry.coverPhotoUrl ?: details?.photos?.firstOrNull()?.fullUrl,
                        address = entry.address?.toPublicAddress(),
                        description = details?.about,
                    )
                }
            } }.awaitAll()
        }
    }

    override suspend fun setFavorite(roaster: CatalogItem, isFavorite: Boolean): Result<Unit> {
        val address = roaster.address
            ?: return Result.failure(IllegalArgumentException("У обжарщика нет публичного адреса"))
        return favorites.setFavorite(FavoriteEntry(FavoriteKind.Roaster, address.toFavoriteAddress(),
            name = roaster.name, coverPhotoUrl = roaster.photoUrl), isFavorite)
    }

    override suspend fun removeForAccount(roaster: CatalogItem, expectedUserId: String): Result<Unit> {
        val ownerSessions = sessions ?: return Result.failure(IllegalStateException("Account guard is unavailable"))
        val session = ownerSessions.peekSession()
        if (session?.userId != expectedUserId || !ownerSessions.isActiveSession(session))
            throw kotlinx.coroutines.CancellationException("Favorites account changed")
        val result = setFavorite(roaster, false)
        currentCoroutineContext().ensureActive()
        if (ownerSessions.peekSession()?.userId != expectedUserId)
            throw kotlinx.coroutines.CancellationException("Favorites account changed")
        return result
    }
}
