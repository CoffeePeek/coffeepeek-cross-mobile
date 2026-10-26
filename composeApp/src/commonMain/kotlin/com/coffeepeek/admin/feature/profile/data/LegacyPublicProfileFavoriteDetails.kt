package com.coffeepeek.admin.feature.profile.data

import com.coffeepeek.domain.repository.RoasterRepository
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.feature.profile.domain.FavoriteRoasterDetails
import com.coffeepeek.feature.profile.domain.FavoriteRoasterSummary
import com.coffeepeek.feature.profile.domain.FavoriteShopDetails
import com.coffeepeek.feature.profile.domain.PublicProfileFavoriteDetails

/** Legacy catalog repositories answering the profile's favorite-enrichment port. Failures mean "no extra data". */
internal class LegacyPublicProfileFavoriteDetails(
    private val shops: ShopRepository?,
    private val roasters: RoasterRepository?,
) : PublicProfileFavoriteDetails {
    override suspend fun roasterSummaries(): List<FavoriteRoasterSummary> =
        roasters?.getRoasters()?.getOrNull().orEmpty().mapNotNull { summary ->
            summary.publicAddress?.slug?.let {
                FavoriteRoasterSummary(it, summary.photoUrl, summary.tags.map { tag -> tag.name },
                    summary.coffeeShopsCount, summary.coffeeProductsCount, summary.availableCoffeeProducts)
            }
        }

    override suspend fun shop(slug: String): FavoriteShopDetails? = shops?.getShopDetails(slug)?.getOrNull()?.let { details ->
        FavoriteShopDetails(details.shop.photoUrl, details.description, details.location?.address ?: details.shop.address,
            details.shop.rating, details.shop.reviewCount, details.shop.isOpen, details.shop.tags)
    }

    override suspend fun roaster(slug: String): FavoriteRoasterDetails? = roasters?.getRoaster(slug)?.getOrNull()?.let { details ->
        FavoriteRoasterDetails(details.photos.firstOrNull()?.fullUrl, details.about)
    }
}
