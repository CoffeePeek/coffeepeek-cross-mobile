package com.coffeepeek.feature.shop.domain.model

const val MAX_SHOP_CHECK_IN_PHOTOS = 5

data class ShopConsumedDrinkOption(
    val slug: String,
    val nameRu: String,
    val nameEn: String,
)

data class ShopCheckInPhoto(
    val bytes: ByteArray,
    val fileName: String,
    val contentType: String = "image/jpeg",
) {
    override fun equals(other: Any?): Boolean = this === other || other is ShopCheckInPhoto &&
        fileName == other.fileName && contentType == other.contentType && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

data class ShopCheckInCreateInput(
    val shopSlug: String,
    val text: String,
    val rating: ShopRating,
    val visitedAtIso: String,
    val visibility: ShopCheckInVisibility = ShopCheckInVisibility.Private,
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
    val photos: List<ShopCheckInPhoto> = emptyList(),
)
