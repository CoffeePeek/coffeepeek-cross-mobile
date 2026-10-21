package com.coffeepeek.admin.di.roaster

import com.coffeepeek.admin.feature.favorites.api.FavoriteInteractions
import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.admin.feature.favorites.api.roasterFavoriteId
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.PublicAddress
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.feature.roaster.api.RoasterFavoriteTarget
import com.coffeepeek.feature.roaster.api.RoasterFavoritesPort
import com.coffeepeek.feature.roaster.api.RoasterShopPhotos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Adapts the server favorites store. Authentication gating, in-flight de-duplication, account guards and the
 * favorite analytics stay in [FavoriteInteractions], exactly as for the legacy roaster screens.
 */
internal class RoasterFavoritesAdapter(
    private val favorites: RoasterFavorites,
    private val scope: CoroutineScope,
) : RoasterFavoritesPort {
    override fun observeKeys(): Flow<Set<String>> = favorites.observeFavorites().map { rows -> rows.mapTo(mutableSetOf()) { it.roasterFavoriteId } }
    override fun observeErrors(): Flow<String?> = favorites.observeErrors()

    override fun change(target: RoasterFavoriteTarget, favorite: Boolean, onFailed: () -> Unit) {
        val item = CatalogItem(
            id = target.key, name = target.name, slug = target.key, photoUrl = target.photoUrl,
            address = PublicAddress(target.key, target.canonicalPath, target.revision, target.isAlias),
        )
        FavoriteInteractions.change(scope, item.roasterFavoriteId, true, favorite,
            action = { favorites.setFavorite(item, favorite) },
            completed = { result -> result.onFailure { onFailed() } })
    }
}

/** Shop thumbnails for a roaster's shops come from the Shop details, as before; any failure means "no photo". */
internal class RoasterShopPhotosAdapter(private val shops: ShopRepository) : RoasterShopPhotos {
    override suspend fun firstPhoto(shopSlug: String): String? {
        val details = shops.getShopDetails(shopSlug).getOrNull() ?: return null
        return details.photos.firstOrNull(String::isNotBlank) ?: details.shop.photoUrl?.takeIf(String::isNotBlank)
    }
}
