package com.coffeepeek.feature.homecheckin.domain.model

/** Owner read model. A preparation can produce multiple servings; never infer one cup. */
data class HomePreparation(
    val id: String,
    val preparedOn: String,
    val preparedAtUtc: String,
    val createdAtUtc: String,
    val text: String?,
    val brewMethodCode: String?,
    val coffeeProductId: String?,
    val beanName: String?,
    val doseGrams: String?,
    val brewDurationSeconds: Int?,
    val tasteScore: Int?,
    val drinkName: String?,
    val drinkSlug: String?,
    val customDrinkName: String?,
    val photos: List<HomePreparationPhoto>,
    val drinkPhotoUrl: String?,
    /** Null means unknown, not Private or confirmed Public. */
    val visibility: HomePreparationVisibility?,
    val moderation: HomePreparationModeration,
    val drinkPhotoThumbnailUrl: String? = null,
    val drinkPhotoDetailUrl: String? = null,
)

data class HomePreparationPhoto(val id: String, val url: String,
    val thumbnailUrl: String? = null, val detailUrl: String? = null, val calendarUrl: String? = null)

enum class HomePreparationVisibility { Private, Public }
enum class HomePreparationModeration { NotSubmitted, Pending, Approved, Rejected, Unknown }
