package com.coffeepeek.feature.shop.domain.model

const val MAX_SHOP_REVIEW_PHOTOS = 5

/** In-memory picked image; draft persistence stores only text and ratings. */
data class ShopReviewPhoto(
    val bytes: ByteArray,
    val fileName: String,
    val contentType: String = "image/jpeg",
) {
    override fun equals(other: Any?): Boolean = this === other || other is ShopReviewPhoto &&
        fileName == other.fileName && contentType == other.contentType && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

/** Keep the existing five-photo selection policy even if a picker returns more than requested. */
fun appendShopReviewPhotos(
    current: List<ShopReviewPhoto>,
    picked: List<ShopReviewPhoto>,
): List<ShopReviewPhoto> = current.take(MAX_SHOP_REVIEW_PHOTOS) +
    picked.take((MAX_SHOP_REVIEW_PHOTOS - current.size).coerceAtLeast(0))
