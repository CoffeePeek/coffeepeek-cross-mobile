package com.coffeepeek.feature.discovery.data.mapper

import com.coffeepeek.feature.discovery.data.backend.CityDto
import com.coffeepeek.feature.discovery.data.backend.LocationDto
import com.coffeepeek.feature.discovery.data.backend.MapDto
import com.coffeepeek.feature.discovery.data.backend.NamedDto
import com.coffeepeek.feature.discovery.data.backend.NearbyRowDto
import com.coffeepeek.feature.discovery.data.backend.PhotoDto
import com.coffeepeek.feature.discovery.data.backend.ShopRowDto
import com.coffeepeek.feature.discovery.domain.model.CatalogEntry
import com.coffeepeek.feature.discovery.domain.model.CityEntry
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.MapContent
import com.coffeepeek.feature.discovery.domain.model.MapMarker
import com.coffeepeek.feature.discovery.domain.model.MapZone
import com.coffeepeek.feature.discovery.domain.model.PriceLevel
import com.coffeepeek.feature.discovery.domain.model.ShopKind
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private const val TAG_LIMIT = 3

/** A row of the plain search. The favorites of the account decide the flag; what the row says is not used. */
internal fun ShopRowDto.toDomain(favorites: Set<String>) = ShopSummary(
    id = address.slug,
    title = name,
    rating = rating.takeIf { it > 0 },
    checkInCount = checkInCount,
    address = location?.address,
    priceLevel = PriceLevel.fromApi(priceRange.text()),
    photoUrl = photos.firstOrNull()?.card(),
    isOpen = isOpen,
    isNew = isNew,
    isVisited = isVisited,
    isFavorite = address.slug in favorites,
    tags = backendTags(tags, shopTags).ifEmpty { (brewMethods + beans).names() }.take(TAG_LIMIT),
    brewMethods = brewMethods.names(),
    roasterLogos = roasters.mapNotNull { it.photoUrl?.takeIf(String::isNotBlank) }.distinct(),
    kind = ShopKind.fromApi((type.takeUnless { it == null || it is JsonNull } ?: coffeeFocus).text()),
    coordinates = location.coordinates(),
)

/**
 * A row of the nearby search. The server omits the kind and the new flag, so the row carries what was asked for, as the legacy search did: [requestedKind], and
 * [asked] for the visited and new switches that the server applied.
 */
internal fun NearbyRowDto.toDomain(favorites: Set<String>, requestedKind: ShopKind?, visitedAsked: Boolean, newAsked: Boolean) = ShopSummary(
    id = address.slug,
    title = name,
    rating = rating?.takeIf { it > 0 },
    checkInCount = checkInCount,
    address = addressLine,
    photoUrl = coverPhoto?.card(),
    isOpen = isOpen,
    isNew = newAsked,
    isVisited = isVisited == true || visitedAsked,
    isFavorite = address.slug in favorites || isFavorite == true,
    tags = tags.names().take(TAG_LIMIT),
    kind = requestedKind ?: ShopKind.CoffeeBar,
    distanceMeters = distanceMeters,
)

/** What prod shows for a map shop without a title. */
internal const val UNNAMED_SHOP = "Кофейня"

/** The markers come from this answer (asked at the shop zoom), the zones from [zonesFrom] (asked at the visible zoom). */
internal fun MapDto.toContent(zonesFrom: MapDto, kind: ShopKind?): MapContent {
    val markers = shops.map { shop ->
        MapMarker(
            id = shop.address.slug, title = shop.title?.takeIf(String::isNotBlank) ?: UNNAMED_SHOP, coordinates = Coordinates(shop.latitude, shop.longitude),
            kind = ShopKind.fromApi(shop.type.text()), zoneId = shop.primaryZone?.slug,
        )
    }
    return MapContent(
        markers = if (kind == null) markers else markers.filter { it.kind == kind },
        zones = zonesFrom.zones.map { zone ->
            MapZone(
                id = zone.address.slug, name = zone.name, description = zone.description, color = zone.color, center = Coordinates(zone.latitude, zone.longitude),
                radiusMeters = zone.radiusMeters, shopCount = zone.shopCount, polygon = zone.polygon.map { Coordinates(it.latitude, it.longitude) },
            )
        },
        isTruncated = isTruncated,
    )
}

internal fun CityDto.toDomain() = CityEntry(address.slug, name)

internal fun NamedDto.toCatalogEntry() = CatalogEntry(key, name.orEmpty())

private fun PhotoDto.card(): String? = urls?.card?.takeIf(String::isNotBlank) ?: fullUrl?.takeIf(String::isNotBlank)

private fun LocationDto?.coordinates(): Coordinates? {
    val latitude = this?.latitude ?: return null
    val longitude = this.longitude ?: return null
    return Coordinates(latitude, longitude)
}

private fun List<NamedDto>.names() = mapNotNull { it.name?.takeIf(String::isNotBlank) }

private fun JsonElement?.text(): String? = (this as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotEmpty)

/** The tags come as an array of names or of objects with a name, a title, a label or a value, in either `tags` or `shopTags`. */
private fun backendTags(vararg candidates: JsonElement?): List<String> =
    candidates.flatMap { tagNames(it) }.map(String::trim).filter(String::isNotBlank).distinct()

private fun tagNames(raw: JsonElement?): List<String> = (raw as? JsonArray).orEmpty().mapNotNull { item ->
    when (item) {
        is JsonPrimitive -> item.contentOrNull
        is JsonObject -> listOf("name", "title", "label", "value").firstNotNullOfOrNull { key -> (item[key] as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) }
        else -> null
    }
}
