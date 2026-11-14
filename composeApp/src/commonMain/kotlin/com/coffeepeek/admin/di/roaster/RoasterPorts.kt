package com.coffeepeek.admin.di.roaster

import com.coffeepeek.admin.di.favorites.FavoriteInteractions
import com.coffeepeek.admin.di.collections.FavoriteCollectionsPrompt
import com.coffeepeek.admin.di.favorites.RoasterFavorites
import com.coffeepeek.admin.di.favorites.roasterFavoriteId
import com.coffeepeek.admin.di.favorites.CatalogItem
import com.coffeepeek.admin.di.favorites.PublicAddress
import com.coffeepeek.feature.shop.api.PublicShopPreviews
import com.coffeepeek.feature.favorites.domain.model.FavoriteKind
import com.coffeepeek.feature.favorites.domain.repository.AccountFavorites
import com.coffeepeek.feature.roaster.api.RoasterFavoriteTarget
import com.coffeepeek.feature.roaster.api.RoasterFavoritesPort
import com.coffeepeek.feature.roaster.api.RoasterShopPhotos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

/**
 * Adapts the server favorites store. Authentication gating, in-flight de-duplication, account guards and the
 * favorite analytics stay in [FavoriteInteractions], as for every roaster screen.
 */
internal class RoasterFavoritesAdapter(
    private val favorites: RoasterFavorites,
    private val scope: CoroutineScope,
    private val favoriteCollections: FavoriteCollectionsPrompt? = null,
    private val serverFavorites: AccountFavorites? = null,
) : RoasterFavoritesPort {
    override fun observeKeys(): Flow<Set<String>> = serverFavorites?.takeIf { favoriteCollections != null }?.observeMemberships()?.mapNotNull { result ->
        result.getOrNull()?.filter { it.kind == FavoriteKind.Roaster }?.map { it.slug }?.toSet()
    } ?: favorites.observeFavorites().map { rows -> rows.mapTo(mutableSetOf()) { it.roasterFavoriteId } }
    override fun observeErrors(): Flow<String?> = favorites.observeErrors()

    override fun change(target: RoasterFavoriteTarget, favorite: Boolean, onFailed: () -> Unit) {
        favoriteCollections?.let { it.roaster(target.key); return }
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
internal class RoasterShopPhotosAdapter(private val shops: PublicShopPreviews) : RoasterShopPhotos {
    override suspend fun firstPhoto(shopSlug: String): String? {
        val result = shops.load(shopSlug)
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        return result.getOrNull()?.detailPhotoUrl?.takeIf(String::isNotBlank)
    }
}
