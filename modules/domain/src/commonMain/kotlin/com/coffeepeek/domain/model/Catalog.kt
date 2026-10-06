package com.coffeepeek.domain.model

data class City(
    val id: String,
    val name: String,
    val address: PublicAddress? = null,
)

data class CatalogItem(
    val id: String,
    val name: String,
    val slug: String = "",
    val photoUrl: String? = null,
    val address: PublicAddress? = null,
)

data class ShopCatalogs(
    val cities: List<City> = emptyList(),
    val beans: List<CatalogItem> = emptyList(),
    val equipment: List<CatalogItem> = emptyList(),
    val roasters: List<CatalogItem> = emptyList(),
    val brewMethods: List<CatalogItem> = emptyList(),
    val shopTags: List<CatalogItem> = emptyList(),
)

data class CreateShopInput(
    val name: String,
    val address: String,
    val cityId: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val description: String? = null,
    val priceRange: Int? = null,
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null,
    val instagram: String? = null,
    val equipmentIds: List<String> = emptyList(),
    val coffeeBeanIds: List<String> = emptyList(),
    val roasterIds: List<String> = emptyList(),
    val brewMethodIds: List<String> = emptyList(),
    val schedules: List<ShopSchedule> = emptyList(),
    val photos: List<PendingPhotoUpload> = emptyList(),
    val menuPhotos: List<PendingPhotoUpload> = emptyList(),
)

data class PendingPhotoUpload(
    val fileName: String,
    val contentType: String,
    val bytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PendingPhotoUpload) return false
        return fileName == other.fileName && contentType == other.contentType && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

data class UploadedPhotoMeta(
    val fileName: String,
    val contentType: String,
    val storageKey: String,
    val size: Long,
)

data class CreateReviewInput(
    val shopId: String,
    val header: String,
    val comment: String,
    val placeRating: Int,
    val serviceRating: Int,
    val coffeeRating: Int,
    val photos: List<PendingPhotoUpload> = emptyList(),
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
)

data class UpdateReviewInput(
    val header: String,
    val comment: String,
    val placeRating: Int,
    val serviceRating: Int,
    val coffeeRating: Int,
    val photos: List<PendingPhotoUpload> = emptyList(),
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
    val clearDrink: Boolean = false,
)

enum class ShopIssueCategory {
    OutdatedMenu,
    ShopClosed,
    IncorrectAddress,
    WrongOpeningHours,
    IncorrectPhotos,
    Other,
}

data class CreateCheckInInput(
    val shopId: String,
    val header: String? = null,
    val note: String? = null,
    val isPublic: Boolean = true,
    val visitedAtIso: String,
    val placeRating: Int? = null,
    val serviceRating: Int? = null,
    val coffeeRating: Int? = null,
    val photos: List<PendingPhotoUpload> = emptyList(),
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
)

data class CheckIn(
    val id: String,
    val shopId: String,
    val shopName: String,
    val note: String,
    val createdAt: String,
    val reviewId: String?,
    val visitedAt: String = "",
    // photoUrls open fullscreen; thumbnails are for small tiles.
    val photoUrls: List<String> = emptyList(),
    val photoThumbnailUrls: List<String> = photoUrls,
    val rating: ReviewRating? = null,
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
    val drinkNameRu: String? = null,
    val drinkNameEn: String? = null,
)
