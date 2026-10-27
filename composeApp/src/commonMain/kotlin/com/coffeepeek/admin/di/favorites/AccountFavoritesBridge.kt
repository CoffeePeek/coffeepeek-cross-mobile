package com.coffeepeek.admin.di.favorites

import com.coffeepeek.domain.feature.favorites.api.FavoriteEntry as LegacyEntry
import com.coffeepeek.domain.feature.favorites.api.FavoriteKind as LegacyKind
import com.coffeepeek.domain.feature.favorites.api.FavoritesRepository as LegacyFavorites
import com.coffeepeek.domain.model.PublicAddress
import com.coffeepeek.feature.favorites.domain.model.FavoriteAddress
import com.coffeepeek.feature.favorites.domain.model.FavoriteEntry
import com.coffeepeek.feature.favorites.domain.model.FavoriteKind
import com.coffeepeek.feature.favorites.domain.repository.AccountFavorites
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal fun PublicAddress.toFavoriteAddress() = FavoriteAddress(slug, canonicalPath, revision, isAlias)
internal fun FavoriteAddress.toPublicAddress() = PublicAddress(slug, canonicalPath, revision, isAlias)

/** The legacy favorites contract over the feature-owned account source, for the legacy consumers that remain. */
internal class LegacyAccountFavorites(private val favorites: AccountFavorites) : LegacyFavorites {
    override fun observeFavorites(): Flow<Result<List<LegacyEntry>>> =
        favorites.observeFavorites().map { result -> result.map { rows -> rows.map { it.toLegacy() } } }

    override suspend fun getFavorites(refresh: Boolean): Result<List<LegacyEntry>> =
        favorites.getFavorites(refresh).map { rows -> rows.map { it.toLegacy() } }

    override suspend fun setFavorite(kind: LegacyKind, address: PublicAddress, favorite: Boolean): Result<Unit> =
        setFavorite(LegacyEntry(kind, address), favorite)

    override suspend fun setFavorite(entry: LegacyEntry, favorite: Boolean): Result<Unit> =
        favorites.setFavorite(entry.toFeature(), favorite)

    override fun clearCache() = favorites.clearCache()
}

private fun FavoriteEntry.toLegacy() = LegacyEntry(
    kind = when (kind) {
        FavoriteKind.CoffeeShop -> LegacyKind.CoffeeShop
        FavoriteKind.Roaster -> LegacyKind.Roaster
        FavoriteKind.Drink -> LegacyKind.Drink
    },
    address = address?.toPublicAddress(), drinkSlug = drinkSlug, name = name, coverPhotoUrl = coverPhotoUrl,
    createdAtUtc = createdAtUtc, occurrence = occurrence,
)

private fun LegacyEntry.toFeature() = FavoriteEntry(
    kind = when (kind) {
        LegacyKind.CoffeeShop -> FavoriteKind.CoffeeShop
        LegacyKind.Roaster -> FavoriteKind.Roaster
        LegacyKind.Drink -> FavoriteKind.Drink
    },
    address = address?.toFavoriteAddress(), drinkSlug = drinkSlug, name = name, coverPhotoUrl = coverPhotoUrl,
    createdAtUtc = createdAtUtc, occurrence = occurrence,
)
