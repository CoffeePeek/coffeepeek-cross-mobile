package com.coffeepeek.feature.roaster.data.backend

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

// Wire representations of the existing endpoints. Names are identical to the legacy DTOs, so a catalog cached
// by either implementation can be read by the other. Never exposed by the factory.
@Serializable
internal data class RoasterEnvelopeDto<T>(
    @SerialName("isSuccess") val isSuccess: Boolean = false,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: T? = null,
)

@Serializable
internal data class RoasterAddressDto(
    @SerialName("slug") val slug: String? = null,
    @SerialName("canonicalPath") val canonicalPath: String? = null,
    @SerialName("revision") @Serializable(with = FlexibleInt::class) val revision: Int = 0,
    @SerialName("isAlias") val isAlias: Boolean = false,
)

@Serializable
internal data class RoasterPhotoUrlsDto(
    @SerialName("thumbnail") val thumbnail: String? = null,
    @SerialName("card") val card: String? = null,
    @SerialName("detail") val detail: String? = null,
    @SerialName("fullscreen") val fullscreen: String? = null,
)

@Serializable
internal data class RoasterShortPhotoDto(
    @SerialName("id") val id: String = "",
    @SerialName("fileName") val fileName: String? = null,
    @SerialName("storageKey") val storageKey: String? = null,
    @SerialName("fullUrl") val fullUrl: String? = null,
    @SerialName("urls") val urls: RoasterPhotoUrlsDto? = null,
    @SerialName("sortIndex") @Serializable(with = FlexibleInt::class) val sortIndex: Int = 0,
    @SerialName("isPrimary") val isPrimary: Boolean = false,
)

@Serializable
internal data class RoasterSummaryDto(
    @SerialName("address") val address: RoasterAddressDto? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("photoUrl") val photoUrl: String? = null,
    @SerialName("coverPhoto") val coverPhoto: RoasterShortPhotoDto? = null,
    @SerialName("tags") val tags: List<RoasterTagDto>? = null,
    @SerialName("coffeeShopsCount") @Serializable(with = FlexibleInt::class) val coffeeShopsCount: Int = 0,
    @SerialName("coffeeProductsCount") @Serializable(with = FlexibleInt::class) val coffeeProductsCount: Int = 0,
    @SerialName("availableCoffeeProducts") @Serializable(with = FlexibleInt::class) val availableCoffeeProducts: Int = 0,
)

@Serializable
internal data class RoasterTagDto(
    @SerialName("slug") val slug: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("sortOrder") @Serializable(with = FlexibleInt::class) val sortOrder: Int = 0,
)

@Serializable
internal data class RoasterDetailsDto(
    @SerialName("address") val address: RoasterAddressDto,
    @SerialName("name") val name: String,
    @SerialName("about") val about: String? = null,
    @SerialName("location") val location: RoasterLocationDto? = null,
    @SerialName("contact") val contact: RoasterContactDto? = null,
    @SerialName("photos") val photos: List<RoasterDetailPhotoDto> = emptyList(),
    @SerialName("shops") val shops: List<RoasterShopDto> = emptyList(),
)

@Serializable
internal data class RoasterLocationDto(
    @SerialName("address") val address: String = "",
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
)

@Serializable
internal data class RoasterContactDto(
    @SerialName("instagramLink") val instagramLink: String? = null,
    @SerialName("siteLink") val siteLink: String? = null,
)

@Serializable
internal data class RoasterDetailPhotoDto(
    @SerialName("id") val id: String = "",
    @SerialName("fileName") val fileName: String = "",
    @SerialName("storageKey") val storageKey: String = "",
    @SerialName("fullUrl") val fullUrl: String? = null,
    @SerialName("sortIndex") val sortIndex: Int = 0,
)

@Serializable
internal data class RoasterShopDto(
    @SerialName("address") val address: RoasterAddressDto,
    @SerialName("name") val name: String,
    @SerialName("coverPhoto") val coverPhoto: RoasterShortPhotoDto? = null,
)

/** The server sends counters/revisions as numbers, strings or null; anything unreadable is zero. */
internal object FlexibleInt : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        if (decoder !is JsonDecoder) return decoder.decodeInt()
        val element = decoder.decodeJsonElement()
        if (element is JsonNull) return 0
        val primitive = element as? JsonPrimitive ?: return 0
        return primitive.intOrNull ?: primitive.content.toIntOrNull() ?: primitive.doubleOrNull?.toInt() ?: 0
    }

    override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value)
}
