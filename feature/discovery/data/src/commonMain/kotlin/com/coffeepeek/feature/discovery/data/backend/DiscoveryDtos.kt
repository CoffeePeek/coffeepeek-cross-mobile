@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.coffeepeek.feature.discovery.data.backend

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/** The server sends counters as a number, a string or null. */
internal object FlexibleIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        if (decoder !is JsonDecoder) return decoder.decodeInt()
        val primitive = decoder.decodeJsonElement() as? JsonPrimitive ?: return 0
        if (primitive is JsonNull) return 0
        return primitive.intOrNull ?: primitive.content.toIntOrNull() ?: primitive.doubleOrNull?.toInt() ?: 0
    }

    override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value)
}

@Serializable
internal data class AddressDto(@SerialName("slug") val slug: String = "")

@Serializable
internal data class PhotoUrlsDto(@SerialName("card") val card: String? = null)

@Serializable
internal data class PhotoDto(@SerialName("fullUrl") val fullUrl: String? = null, @SerialName("urls") val urls: PhotoUrlsDto? = null)

@Serializable
internal data class LocationDto(
    @SerialName("address") val address: String? = null,
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
)

/** A catalog item, as a list of the catalogs, a tag, a roaster or a brew method of a shop row. */
@Serializable
internal data class NamedDto(
    @SerialName("name") val name: String? = null,
    @SerialName("slug") val slug: String? = null,
    @SerialName("address") val address: AddressDto? = null,
    @SerialName("photoUrl") val photoUrl: String? = null,
) {
    val key: String get() = address?.slug?.takeIf(String::isNotBlank) ?: slug.orEmpty()
}

@Serializable
internal data class CityDto(@SerialName("address") val address: AddressDto, @SerialName("name") val name: String = "")

@Serializable
internal data class ShopRowDto(
    @SerialName("address") val address: AddressDto,
    @SerialName("name") val name: String = "",
    @SerialName("photos") val photos: List<PhotoDto> = emptyList(),
    @SerialName("rating") val rating: Double = 0.0,
    @SerialName("checkInCount") @Serializable(with = FlexibleIntSerializer::class) val checkInCount: Int = 0,
    @SerialName("isVisited") val isVisited: Boolean = false,
    @SerialName("isNew") val isNew: Boolean = false,
    @SerialName("isOpen") val isOpen: Boolean = false,
    @SerialName("priceRange") val priceRange: JsonElement? = null,
    @SerialName("type") val type: JsonElement? = null,
    @SerialName("coffeeFocus") val coffeeFocus: JsonElement? = null,
    @SerialName("location") val location: LocationDto? = null,
    @SerialName("beans") val beans: List<NamedDto> = emptyList(),
    @SerialName("roasters") val roasters: List<NamedDto> = emptyList(),
    @SerialName("brewMethods") val brewMethods: List<NamedDto> = emptyList(),
    @SerialName("tags") val tags: JsonElement? = null,
    @SerialName("shopTags") val shopTags: JsonElement? = null,
)

@Serializable
internal data class ShopListDto(
    @SerialName("coffeeShops") val coffeeShops: List<ShopRowDto> = emptyList(),
    @SerialName("currentPage") val currentPage: Int = 1,
    @SerialName("totalItems") val totalItems: Int = 0,
    @SerialName("totalPages") val totalPages: Int = 0,
)

@Serializable
internal data class NearbyRowDto(
    @SerialName("address") val address: AddressDto,
    @SerialName("name") val name: String = "",
    @SerialName("addressLine") val addressLine: String? = null,
    @SerialName("coverPhoto") val coverPhoto: PhotoDto? = null,
    @SerialName("rating") val rating: Double? = null,
    @SerialName("checkInCount") @Serializable(with = FlexibleIntSerializer::class) val checkInCount: Int = 0,
    @SerialName("isOpen") val isOpen: Boolean = false,
    @SerialName("isVisited") val isVisited: Boolean? = null,
    @SerialName("isFavorite") val isFavorite: Boolean? = null,
    @SerialName("distanceMeters") val distanceMeters: Double? = null,
    @SerialName("tags") val tags: List<NamedDto> = emptyList(),
)

@Serializable
internal data class NearbyPageDto(
    @SerialName("items") val items: List<NearbyRowDto> = emptyList(),
    @SerialName("totalItems") val totalItems: Int = 0,
    @SerialName("totalPages") val totalPages: Int = 0,
    @SerialName("currentPage") val currentPage: Int = 1,
)

@Serializable
internal data class NearbyRequestDto(
    @SerialName("filters") val filters: NearbyFiltersDto,
    @SerialName("sort") val sort: String = "distance_asc",
    @SerialName("page") val page: Int,
    @SerialName("pageSize") val pageSize: Int,
    // The gateway rejects a null or blank q: it is left out without a text.
    @EncodeDefault(EncodeDefault.Mode.NEVER) @SerialName("q") val q: String? = null,
)

@Serializable
internal data class NearbyFiltersDto(
    @SerialName("origin") val origin: OriginDto,
    @SerialName("radiusKm") val radiusKm: Int = 3,
    @SerialName("city") val city: String? = null,
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("roasters") val roasters: List<String> = emptyList(),
    @SerialName("equipments") val equipments: List<String> = emptyList(),
    @SerialName("beans") val beans: List<String> = emptyList(),
    @SerialName("brewMethods") val brewMethods: List<String> = emptyList(),
    @SerialName("isOpen") val isOpen: Boolean? = null,
    @SerialName("isNew") val isNew: Boolean? = null,
    @SerialName("visitedOnly") val visitedOnly: Boolean = false,
    @SerialName("favoritesOnly") val favoritesOnly: Boolean = false,
    @SerialName("minRating") val minRating: Double? = null,
    @SerialName("priceRange") val priceRange: String? = null,
    @SerialName("type") val type: String? = null,
)

@Serializable
internal data class OriginDto(@SerialName("latitude") val latitude: Double, @SerialName("longitude") val longitude: Double)

/** The map answers with PascalCase names on some deployments. */
@Serializable
internal data class MapShopDto(
    @SerialName("address") val address: AddressDto,
    @SerialName("latitude") @JsonNames("Latitude") val latitude: Double,
    @SerialName("longitude") @JsonNames("Longitude") val longitude: Double,
    @SerialName("title") @JsonNames("Title") val title: String? = null,
    @SerialName("type") @JsonNames("Type") val type: JsonElement? = null,
    @SerialName("primaryZone") val primaryZone: AddressDto? = null,
)

@Serializable
internal data class ZonePointDto(
    @SerialName("latitude") @JsonNames("Latitude") val latitude: Double,
    @SerialName("longitude") @JsonNames("Longitude") val longitude: Double,
)

@Serializable
internal data class ZoneDto(
    @SerialName("address") val address: AddressDto,
    @SerialName("name") @JsonNames("Name") val name: String = "",
    @SerialName("description") @JsonNames("Description") val description: String = "",
    @SerialName("color") @JsonNames("Color") val color: String? = null,
    @SerialName("latitude") @JsonNames("Latitude") val latitude: Double,
    @SerialName("longitude") @JsonNames("Longitude") val longitude: Double,
    @SerialName("radiusMeters") @JsonNames("RadiusMeters") val radiusMeters: Double,
    @SerialName("shopCount") @JsonNames("ShopCount") val shopCount: Int,
    @SerialName("polygon") @JsonNames("Polygon") val polygon: List<ZonePointDto> = emptyList(),
)

@Serializable
internal data class ClusterBoundsDto(
    @SerialName("minLatitude") @JsonNames("MinLatitude") val minLatitude: Double,
    @SerialName("minLongitude") @JsonNames("MinLongitude") val minLongitude: Double,
    @SerialName("maxLatitude") @JsonNames("MaxLatitude") val maxLatitude: Double,
    @SerialName("maxLongitude") @JsonNames("MaxLongitude") val maxLongitude: Double,
)

@Serializable
internal data class ClusterDto(
    @SerialName("id") @JsonNames("Id") val id: String? = null,
    @SerialName("latitude") @JsonNames("Latitude") val latitude: Double,
    @SerialName("longitude") @JsonNames("Longitude") val longitude: Double,
    @SerialName("count") @JsonNames("Count") val count: Int,
    @SerialName("bounds") @JsonNames("Bounds") val bounds: ClusterBoundsDto,
)

@Serializable
internal data class MapDto(
    @SerialName("shops") @JsonNames("Shops") val shops: List<MapShopDto> = emptyList(),
    @SerialName("clusters") @JsonNames("Clusters") val clusters: List<ClusterDto> = emptyList(),
    @SerialName("zones") @JsonNames("Zones") val zones: List<ZoneDto> = emptyList(),
    @SerialName("isTruncated") @JsonNames("IsTruncated") val isTruncated: Boolean = false,
)
