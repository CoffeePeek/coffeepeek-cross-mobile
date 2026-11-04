package com.coffeepeek.admin.di.favorites

import com.coffeepeek.feature.favorites.domain.model.FavoriteAddress
import com.coffeepeek.feature.favorites.domain.model.FavoriteEntry
import com.coffeepeek.feature.favorites.domain.model.FavoriteKind
import com.coffeepeek.feature.favorites.domain.repository.AccountFavorites
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal fun PublicAddress.toFavoriteAddress() = FavoriteAddress(slug, canonicalPath, revision, isAlias)
internal fun FavoriteAddress.toPublicAddress() = PublicAddress(slug, canonicalPath, revision, isAlias)
