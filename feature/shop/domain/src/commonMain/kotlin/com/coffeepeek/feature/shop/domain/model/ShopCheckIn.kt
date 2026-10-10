package com.coffeepeek.feature.shop.domain.model

data class ShopCheckIn(
    val id: String,
    val userId: String,
    val shopId: String,
    val note: String,
    val createdAt: String,
    val visitedAt: String,
    val reviewId: String?,
    val photoUrls: List<String>,
    val photoThumbnailUrls: List<String>,
    val rating: ShopRating?,
    val username: String = "",
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
    val drinkNameRu: String? = null,
    val drinkNameEn: String? = null,
    val visibility: ShopCheckInVisibility = ShopCheckInVisibility.Private,
    val moderationState: ShopCheckInModerationState = ShopCheckInModerationState.NotSubmitted,
    val contentRevision: Int = 0,
    val rejectionReason: String? = null,
    val helpfulCount: Int = 0,
    val isHelpfulByCurrentUser: Boolean = false,
)

enum class ShopCheckInVisibility { Private, Public }

enum class ShopCheckInModerationState { NotSubmitted, Pending, Approved, Rejected, Unknown }
