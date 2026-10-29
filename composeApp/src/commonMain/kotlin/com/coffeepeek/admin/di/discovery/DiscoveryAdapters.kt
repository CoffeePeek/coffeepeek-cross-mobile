package com.coffeepeek.admin.di.discovery

import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.admin.utils.FavoriteSync
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.domain.model.City
import com.coffeepeek.feature.shop.api.PublicShopPreviews
import com.coffeepeek.feature.roaster.api.PublicRoasterCatalog
import com.coffeepeek.feature.discovery.api.DiscoveryAccount
import com.coffeepeek.feature.discovery.api.DiscoveryCity
import com.coffeepeek.feature.discovery.api.DiscoveryFavorites
import com.coffeepeek.feature.discovery.api.DiscoveryShopDetails
import com.coffeepeek.feature.discovery.api.MapShopDetails
import com.coffeepeek.feature.discovery.api.MapOpeningHours
import com.coffeepeek.feature.discovery.domain.model.CatalogEntry
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.CityEntry
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

/** The account key is the user id (else the token) of an active session, null for a guest, as in the other migrated owners. */
internal class SessionDiscoveryAccount(private val sessions: SessionReader) : DiscoveryAccount {
    private fun key(session: Session?): String? = if (session != null && sessions.isActiveSession(session)) session.userId ?: session.accessToken else null
    override val accountKey: Flow<String?> = sessions.observeSession().map(::key)
    override fun currentAccountKey(): String? = key(sessions.peekSession())
}

/** The favorites of the account come from the favorites owner; a failing source keeps the last known ids. A change is announced to the screens that still listen to [FavoriteSync]. */
internal class AppDiscoveryFavorites(private val repository: FavoritesRepository) : DiscoveryFavorites {
    override val ids: Flow<Set<String>> = repository.observeIds().mapNotNull { it.getOrNull() }
    override suspend fun favoriteIds(): Set<String> = repository.observeIds().first().getOrDefault(emptySet())

    override suspend fun setFavorite(shop: ShopSummary, favorite: Boolean): Result<Unit> =
        (if (favorite) repository.save(shop.toFavoriteShop()) else repository.remove(shop.id)).also { if (it.isSuccess) FavoriteSync.notifyChanged(shop.id, favorite) }
}

internal fun ShopSummary.toFavoriteShop() = FavoriteShop(
    id = id,
    title = title,
    rating = rating,
    reviewCount = checkInCount,
    priceRange = priceLevel?.let { "$".repeat(it.level) },
    photoUrl = photoUrl,
    address = address,
    latitude = coordinates?.latitude,
    longitude = coordinates?.longitude,
    isOpen = isOpen,
    tags = tags,
    brewMethods = brewMethods,
    roasterPhotoUrls = roasterLogos,
)

/** The city is shared with the settings through the legacy preference; its ids are the cities' public slugs, as in Discovery's own catalog. */
internal class PreferenceDiscoveryCity(private val preference: CityPreference) : DiscoveryCity {
    override val selected: StateFlow<String?> = preference.selectedCityId
    override suspend fun resolve(cities: List<CityEntry>): String? = preference.resolve(cities.map { City(it.id, it.name) })
}

/** Adapts only public Shop API data; no legacy repository or presentation formatter. */
internal class ShopDiscoveryDetails(private val shops: PublicShopPreviews) : DiscoveryShopDetails {
    override suspend fun details(shopId: String): Result<MapShopDetails> = shops.load(shopId).map { details ->
        MapShopDetails(
            photoUrl = details.coverPhotoUrl,
            rating = details.rating,
            reviewCount = details.reviewCount,
            isOpen = details.isOpen,
            hours = details.hours?.let { MapOpeningHours(it.isClosed, it.openTime, it.closeTime) },
            coordinates = details.coordinates?.let { Coordinates(it.latitude, it.longitude).takeIf { it.isValid } },
        )
    }
}

/** Public slugs and labels come from Roaster's existing persisted catalog. */
internal fun roasterEntries(catalog: PublicRoasterCatalog): suspend () -> Result<List<CatalogEntry>> = {
    catalog.load().map { rows -> rows.map { CatalogEntry(it.slug, it.name, it.photoUrl) } }
}
