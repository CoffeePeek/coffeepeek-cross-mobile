package com.coffeepeek.domain.model

data class CoffeeShop(
    val id: String,
    val title: String,
    val rating: Double?,
    val reviewCount: Int = 0,
    val cityName: String?,
    val priceRange: String?,
    val photoUrl: String?,
    val address: String? = null,
    val isOpen: Boolean = false,
    val isFavorite: Boolean = false,
    val isNew: Boolean = false,
    val isVisited: Boolean = false,
    val tags: List<String> = emptyList(),
    val brewMethods: List<String> = emptyList(),
    val roasterPhotoUrls: List<String> = emptyList(),
    val type: String = CoffeeShopType.COFFEE_BAR,
    val location: ShopLocation? = null,
    val publicAddress: PublicAddress? = null,
)

data class CoffeeShopDetails(
    val shop: CoffeeShop,
    val cityId: String = "",
    val description: String? = null,
    val location: ShopLocation? = null,
    val isVisited: Boolean = false,
    val isNew: Boolean = false,
    val canCreateReview: Boolean? = null,
    val existingReviewId: String? = null,
    // photos: hero-sized; fullscreenPhotos: same order, for the viewer.
    val photos: List<String> = emptyList(),
    val fullscreenPhotos: List<String> = photos,
    val shopPhotos: List<ShopPhoto> = emptyList(),
    val reviews: List<Review> = emptyList(),
    val userCheckIns: List<CheckIn> = emptyList(),
    val contact: ShopContact? = null,
    val brewMethods: List<String> = emptyList(),
    val brewMethodItems: List<CatalogItem> = emptyList(),
    val coffeeBeans: List<String> = emptyList(),
    val roasters: List<CatalogItem> = emptyList(),
    val equipment: List<String> = emptyList(),
    val equipmentItems: List<CatalogItem> = emptyList(),
    val tagItems: List<CatalogItem> = emptyList(),
    val schedules: List<ShopSchedule> = emptyList(),
    val menu: ShopMenu? = null,
)

data class ShopMenu(
    val capturedAtUtc: String? = null,
    val updatedAtUtc: String? = null,
    val currency: String = "BYN",
    val items: List<ShopMenuItem> = emptyList(),
    val photos: List<ShopMenuPhoto> = emptyList(),
)

data class ShopMenuItem(
    val slug: String,
    val nameRu: String,
    val nameEn: String = "",
    val category: String,
    val availability: String,
    val price: Double? = null,
    val currency: String = "BYN",
    val volumeMl: Int? = null,
)

data class ShopMenuPhoto(
    val id: String,
    val fullUrl: String,
    val previewUrl: String = fullUrl,
    val sortIndex: Int = 0,
)

data class CoffeeDrinkDefinition(
    val slug: String,
    val nameRu: String,
    val nameEn: String = "",
    val category: String,
    val sortOrder: Int,
)

data class ShopLocation(
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

data class ShopSchedule(
    val dayOfWeek: Int,
    val isClosed: Boolean,
    val intervals: List<ScheduleInterval> = emptyList(),
)

data class ScheduleInterval(
    val openTime: String,
    val closeTime: String,
)

data class Review(
    val id: String,
    // Moderation record this review is linked to; null means there's no pending moderation entry to edit.
    val moderationReviewId: String? = null,
    val shopId: String = "",
    val userId: String = "",
    val username: String,
    val header: String,
    val comment: String,
    val rating: ReviewRating,
    val createdAt: String,
    val photoUrls: List<String> = emptyList(),
    val helpfulCount: Int = 0,
    val isHelpfulByCurrentUser: Boolean = false,
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
    val drinkNameRu: String? = null,
    val drinkNameEn: String? = null,
)

data class HelpfulVote(
    val isHelpful: Boolean,
    val helpfulCount: Int,
)

data class ReviewRating(
    val place: Int,
    val service: Int,
    val coffee: Int,
) {
    val average: Double get() = (place + service + coffee) / 3.0
}

data class ShopContact(
    val instagram: String? = null,
    val email: String? = null,
    val website: String? = null,
    val phone: String? = null,
)

data class ShopFilters(
    val query: String? = null,
    val cityId: String? = null,
    val coffeeFocus: String? = null,
    val roasterIds: List<String> = emptyList(),
    val equipmentIds: List<String> = emptyList(),
    val beanIds: List<String> = emptyList(),
    val brewMethodIds: List<String> = emptyList(),
    val tagIds: List<String> = emptyList(),
    val priceRange: Int? = null,
    val minRating: Double? = null,
    val page: Int = 1,
    val pageSize: Int = 20,
)

data class MapBounds(
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double,
)

data class MapShop(
    val id: String,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val type: String = CoffeeShopType.COFFEE_BAR,
    val primaryZoneId: String? = null,
    val publicAddress: PublicAddress? = null,
)

data class MapCluster(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val count: Int,
    val bounds: MapBounds,
)

data class MapCoffeeZone(
    val id: String,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val shopCount: Int,
    // (latitude, longitude); empty → draw a circle from radiusMeters
    val polygon: List<Pair<Double, Double>> = emptyList(),
    val publicAddress: PublicAddress? = null,
)

data class MapContent(
    val shops: List<MapShop> = emptyList(),
    val clusters: List<MapCluster> = emptyList(),
    val zones: List<MapCoffeeZone> = emptyList(),
    val isTruncated: Boolean = false,
)

object CoffeeShopType {
    const val SPECIALTY = "specialty"
    const val COFFEE_BAR = "coffee_bar"
    const val CAFE = "cafe"

    fun fromApi(raw: String?): String = when (raw?.trim()?.lowercase()) {
        "1", "specialty" -> SPECIALTY
        "3", "cafe" -> CAFE
        else -> COFFEE_BAR
    }

    fun toApi(id: String): String = when (id) {
        SPECIALTY -> "Specialty"
        CAFE -> "Cafe"
        else -> "CoffeeBar"
    }
}

data class PagedResult<T>(
    val items: List<T>,
    val totalCount: Int,
    val totalPages: Int,
    val currentPage: Int,
)

/** A shop the current user submitted, with its moderation outcome. */
data class ShopSubmission(
    val id: String,
    val name: String,
    val address: String?,
    val status: ModerationStatus,
    val rejectedReason: String?,
    // Set once the shop is live in the catalog; may lag briefly behind Approved.
    val publishedShopId: String?,
)

data class ReviewSubmission(
    val review: Review,
    val status: ModerationStatus,
    val rejectedReason: String?,
)
