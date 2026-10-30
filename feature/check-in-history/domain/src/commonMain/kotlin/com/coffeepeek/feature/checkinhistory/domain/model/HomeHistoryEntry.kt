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
    val visibility: HistoryVisibility?,
    val moderation: HistoryModeration,
    /** The entry's own copy of its shelf-bag card, when the author shows the bag. */
    val shelfBagCard: HomeHistoryPhoto? = null,
)

data class HomeHistoryRecipe(
    val brewMethodCode: String?,
    val coffeeProductId: String?,
    val beanName: String?,
    val doseGrams: String?,
    val brewDurationSeconds: Int?,
    val tasteScore: Int?,
    val roasterName: String? = null,
    /** The catalogue coffee's current public slug while it is published. */
    val coffeeProductSlug: String? = null,
)

data class HomeHistoryPhoto(val id: String, val url: String,
    val thumbnailUrl: String? = null, val detailUrl: String? = null, val calendarUrl: String? = null)
