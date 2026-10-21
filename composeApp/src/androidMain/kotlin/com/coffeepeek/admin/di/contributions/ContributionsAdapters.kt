package com.coffeepeek.admin.di.contributions

import com.coffeepeek.admin.location.PlatformLocation
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.feature.contributions.api.ContributionCatalogs
import com.coffeepeek.feature.contributions.api.ContributionLocation
import com.coffeepeek.feature.contributions.api.ContributionsAccount
import com.coffeepeek.feature.contributions.api.GeoPoint
import com.coffeepeek.feature.contributions.domain.model.Availability
import com.coffeepeek.feature.contributions.domain.model.CatalogOption
import com.coffeepeek.feature.contributions.domain.model.Catalogs
import com.coffeepeek.feature.contributions.domain.model.Contacts
import com.coffeepeek.feature.contributions.domain.model.DrinkOption
import com.coffeepeek.feature.contributions.domain.model.MenuRow
import com.coffeepeek.feature.contributions.domain.model.ShopPhotoRef
import com.coffeepeek.feature.contributions.domain.model.ShopSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The account key is the user id (else the token) of an active session, null for a guest, as in the other migrated owners. */
internal class SessionContributionsAccount(private val sessions: SessionReader) : ContributionsAccount {
    private fun key(session: Session?): String? = if (session != null && sessions.isActiveSession(session)) session.userId ?: session.accessToken else null
    override val accountKey: Flow<String?> = sessions.observeSession().map(::key)
    override fun currentAccountKey(): String? = key(sessions.peekSession())
}

/** The catalogs, the shop and the drinks the forms read, taken from the legacy shop repository that still owns them. */
internal class ShopContributionCatalogs(private val shops: ShopRepository) : ContributionCatalogs {
    override suspend fun catalogs(): Result<Catalogs> = shops.getCatalogs().map { all ->
        Catalogs(
            cities = all.cities.map { CatalogOption(it.id, it.name) },
            beans = all.beans.options(),
            equipment = all.equipment.options(),
            roasters = all.roasters.options(),
            brewMethods = all.brewMethods.options(),
            tags = all.shopTags.options(),
        )
    }

    override suspend fun shop(slug: String): Result<ShopSnapshot> = shops.getShopDetails(slug).map { it.toSnapshot(slug) }

    override suspend fun drinks(): Result<List<DrinkOption>> =
        shops.getMenuDrinks().map { drinks -> drinks.map { DrinkOption(it.slug, it.nameRu.ifBlank { it.nameEn }.ifBlank { it.slug }) } }
}

private fun List<CatalogItem>.options() = map { CatalogOption(it.id, it.name, it.photoUrl) }

internal fun CoffeeShopDetails.toSnapshot(slug: String) = ShopSnapshot(
    slug = slug,
    title = shop.title,
    description = description,
    contacts = Contacts(contact?.phone, contact?.email, contact?.website, contact?.instagram),
    photos = shopPhotos.map { ShopPhotoRef(it.id, it.previewUrl) },
    tagIds = tagItems.map { it.id },
    roasterIds = roasters.map { it.id },
    equipmentIds = equipmentItems.map { it.id },
    brewMethodIds = brewMethodItems.map { it.id },
    menu = menu?.items.orEmpty().map { MenuRow(it.slug, it.availability.toAvailability(), it.price) },
    menuPhotos = menu?.photos.orEmpty().map { ShopPhotoRef(it.id, it.previewUrl) },
)

private fun String.toAvailability() = when (lowercase()) {
    "present", "1" -> Availability.Present
    "absent", "2" -> Availability.Absent
    else -> Availability.Unknown
}

/** The position is read only when the permission is already granted; the form asks for it through its own effect. */
internal class AppContributionLocation : ContributionLocation {
    override suspend fun lastKnown(): GeoPoint? =
        if (PlatformLocation.hasPermission()) PlatformLocation.getLastKnownLocation()?.let { GeoPoint(it.latitude, it.longitude) } else null

    override suspend fun addressOf(point: GeoPoint): String? = PlatformLocation.reverseGeocode(point.latitude, point.longitude)
}
