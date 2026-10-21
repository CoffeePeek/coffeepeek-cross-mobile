package com.coffeepeek.admin.feature.profile.data

import com.coffeepeek.admin.feature.profile.domain.PublicProfileCheckIn
import com.coffeepeek.admin.feature.profile.domain.PublicProfileCheckInPage
import com.coffeepeek.admin.feature.profile.domain.PublicProfileRepository
import com.coffeepeek.core.network.requestResult
import com.coffeepeek.domain.feature.feed.FeedFilters
import com.coffeepeek.domain.feature.feed.FeedRepository
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.CheckInModerationState
import com.coffeepeek.domain.model.CheckInVisibility
import com.coffeepeek.domain.repository.RoasterRepository
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.feature.profile.domain.FavoriteRoasterDetails
import com.coffeepeek.feature.profile.domain.FavoriteRoasterSummary
import com.coffeepeek.feature.profile.domain.FavoriteShopDetails
import com.coffeepeek.feature.profile.domain.PublicProfileData
import com.coffeepeek.feature.profile.domain.PublicProfileFavoriteDetails

/** Profile data comes from the profile data module; the author's check-ins still use the app's timeline model. */
internal class AppPublicProfileRepository(
    private val data: PublicProfileData,
    private val feed: FeedRepository,
) : PublicProfileRepository, PublicProfileData by data {
    override suspend fun getCheckIns(slug: String, cursor: String?): Result<PublicProfileCheckInPage> = requestResult {
        require(slug.isNotBlank() && slug.length <= 100) { "Некорректный адрес профиля" }
        val page = feed.getFeed(20, cursor, FeedFilters(authorSlug = slug)).getOrThrow()
        PublicProfileCheckInPage(page.items.map { it.checkIn }.filter {
            it.authorAddress?.slug == slug && it.visibility == CheckInVisibility.Public && it.moderationState == CheckInModerationState.Approved
        }.map(CheckIn::toPublicProfileCheckIn), page.nextCursor)
    }
}

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

private fun CheckIn.toPublicProfileCheckIn(): PublicProfileCheckIn {
    val recipe = homePreparation
    val drink = customDrinkName?.takeIf(String::isNotBlank) ?: drinkNameRu?.takeIf(String::isNotBlank) ?: drinkNameEn?.takeIf(String::isNotBlank)
    return PublicProfileCheckIn(
        id, shopAddress?.slug?.takeIf(String::isNotBlank)?.takeUnless { isHome },
        if (isHome) "Дома" else shopName.ifBlank { "Кофейня" },
        if (isHome) recipe?.beanName?.takeIf(String::isNotBlank) ?: "Кофе дома" else drink ?: "Чекин в кофейне",
        if (isHome) listOfNotNull(recipe?.brewMethodCode, recipe?.doseGrams?.let { "$it г" }, recipe?.brewDurationSeconds?.let { "$it с" }).joinToString(" · ").takeIf(String::isNotBlank) else drink,
        if (isHome) recipe?.tasteScore?.toDouble() else rating?.average,
        note,
        if (isHome) recipe?.preparedOn?.takeIf(String::isNotBlank) ?: visitedAt.ifBlank { createdAt } else visitedAt.ifBlank { createdAt },
        photoUrls,
        source = this,
    )
}

