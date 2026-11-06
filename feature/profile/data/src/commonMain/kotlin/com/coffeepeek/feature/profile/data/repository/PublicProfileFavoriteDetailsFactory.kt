package com.coffeepeek.feature.profile.data.repository

import com.coffeepeek.feature.profile.domain.FavoriteRoasterDetails
import com.coffeepeek.feature.profile.domain.FavoriteRoasterSummary
import com.coffeepeek.feature.profile.domain.FavoriteShopDetails
import com.coffeepeek.feature.profile.domain.PublicProfileFavoriteDetails
import com.coffeepeek.feature.roaster.api.PublicRoasterDetails
import com.coffeepeek.feature.shop.api.PublicShopDetails
import kotlinx.coroutines.CancellationException

/** Optional enrichment of the viewed author's entries; never reads or writes the viewer's favorites. */
fun createPublicProfileFavoriteDetails(shops: PublicShopDetails, roasters: PublicRoasterDetails): PublicProfileFavoriteDetails =
    object : PublicProfileFavoriteDetails {
        override suspend fun roasterSummaries() = roasters.summaries().optional().orEmpty().map {
            FavoriteRoasterSummary(it.slug, it.photoUrl, it.tags, it.coffeeShopsCount, it.coffeeProductsCount, it.availableCoffeeProducts)
        }
        override suspend fun shop(slug: String) = shops.load(slug).optional()?.let {
            FavoriteShopDetails(it.photoUrl, it.description, it.address, it.rating, it.reviewCount, it.isOpen, it.tags)
        }
        override suspend fun roaster(slug: String) = roasters.load(slug).optional()?.let { FavoriteRoasterDetails(it.photoUrl, it.about) }
    }

private fun <T> Result<T>.optional(): T? {
    exceptionOrNull()?.let { if (it is CancellationException) throw it }
    return getOrNull()
}
