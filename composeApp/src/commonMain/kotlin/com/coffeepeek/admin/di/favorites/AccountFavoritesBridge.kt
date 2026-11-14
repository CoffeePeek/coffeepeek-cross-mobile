package com.coffeepeek.admin.di.favorites

import com.coffeepeek.feature.favorites.domain.model.FavoriteAddress

internal fun PublicAddress.toFavoriteAddress() = FavoriteAddress(slug, canonicalPath, revision, isAlias)
internal fun FavoriteAddress.toPublicAddress() = PublicAddress(slug, canonicalPath, revision, isAlias)
