package com.coffeepeek.feature.checkinhistory.domain.model

/** History-owned snapshot: never depends on another feature's domain or legacy models. */
data class HomeHistoryEntry(
    val id: String,
    val preparedOn: String,
    val preparedAtUtc: String,
    val createdAtUtc: String,
    val note: String?,
    val recipe: HomeHistoryRecipe,
    val drinkName: String?,
    val drinkSlug: String?,
    val customDrinkName: String?,
    val photos: List<HomeHistoryPhoto>,
    val drinkPhotoUrl: String?,
    val visibility: HistoryVisibility?,
    val moderation: HistoryModeration,
)

data class HomeHistoryRecipe(
    val brewMethodCode: String?,
    val coffeeProductId: String?,
    val beanName: String?,
    val doseGrams: String?,
    val brewDurationSeconds: Int?,
    val tasteScore: Int?,
)

data class HomeHistoryPhoto(val id: String, val url: String)
