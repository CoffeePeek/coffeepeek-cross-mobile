package com.coffeepeek.admin.di.discovery

import com.coffeepeek.admin.ui.screen.map.MapContentSource
import com.coffeepeek.domain.model.MapBounds
import com.coffeepeek.domain.model.MapCluster
import com.coffeepeek.domain.model.MapCoffeeZone
import com.coffeepeek.domain.model.MapContent
import com.coffeepeek.domain.model.MapShop
import com.coffeepeek.domain.model.ShopFilters
import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.feature.discovery.data.repository.createDiscoveryRepository
import com.coffeepeek.feature.discovery.domain.model.CatalogEntry
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.DiscoveryCriteria
import com.coffeepeek.feature.discovery.domain.model.DiscoveryFilters
import com.coffeepeek.feature.discovery.domain.model.PriceLevel
import com.coffeepeek.feature.discovery.domain.model.ShopKind
import com.coffeepeek.feature.discovery.domain.repository.DiscoveryRepository
import io.ktor.client.HttpClient

internal fun legacyDiscoveryMapSource(client: HttpClient, favorites: FavoriteRepository, shops: ShopRepository): MapContentSource {
    val repository = createDiscoveryRepository(client, favorites::getFavoriteIds) {
        shops.getCatalogs().map { catalogs -> catalogs.roasters.map { CatalogEntry(it.id, it.name) } }
    }
    return LegacyDiscoveryMapSource(repository)
}

internal class LegacyDiscoveryMapSource(private val repository: DiscoveryRepository) : MapContentSource {
    override suspend fun load(bounds: MapBounds, zoom: Float, filters: ShopFilters): Result<MapContent> =
        repository.mapContent(bounds.toDiscoveryBounds(), zoom, filters.toDiscoveryCriteria()).map { content ->
            MapContent(
                shops = content.markers.map { MapShop(it.id, it.title, it.coordinates.latitude, it.coordinates.longitude, it.kind.legacyId(), it.zoneId) },
                clusters = content.clusters.map { MapCluster(it.id, it.coordinates.latitude, it.coordinates.longitude, it.count, it.bounds.toLegacyBounds()) },
                zones = content.zones.map { MapCoffeeZone(it.id, it.name, it.description, it.color, it.center.latitude, it.center.longitude,
                    it.radiusMeters, it.shopCount, it.polygon.map { point -> point.latitude to point.longitude }) },
                isTruncated = content.isTruncated,
            )
        }
}

internal fun ShopFilters.toDiscoveryCriteria(): DiscoveryCriteria = DiscoveryCriteria(
    text = query.orEmpty(),
    filters = DiscoveryFilters(cityId = cityId, kind = ShopKind.entries.firstOrNull { it.legacyId() == coffeeFocus },
        openOnly = openOnly, newOnly = newOnly, visitedOnly = visitedOnly, favoritesOnly = favoritesOnly,
        nearbyOnly = origin != null, priceLevel = PriceLevel.entries.firstOrNull { it.level == priceRange }, minRating = minRating,
        roasterIds = roasterIds.toSet(), beanIds = beanIds.toSet(), equipmentIds = equipmentIds.toSet(),
        brewMethodIds = brewMethodIds.toSet(), tagIds = tagIds.toSet()),
    origin = origin?.let { point ->
        val latitude = point.latitude
        val longitude = point.longitude
        if (latitude != null && longitude != null) Coordinates(latitude, longitude) else null
    },
)

private fun ShopKind.legacyId(): String = when (this) {
    ShopKind.Specialty -> "specialty"
    ShopKind.CoffeeBar -> "coffee_bar"
    ShopKind.Cafe -> "cafe"
}

private fun MapBounds.toDiscoveryBounds() = com.coffeepeek.feature.discovery.domain.model.MapBounds(minLat, minLon, maxLat, maxLon)
private fun com.coffeepeek.feature.discovery.domain.model.MapBounds.toLegacyBounds() = MapBounds(minLat, minLon, maxLat, maxLon)
