package com.coffeepeek.domain.feature.feed

/** Read-only recipe summary carried by the existing public feed contract. */
data class FeedHomePreparation(
    val preparedOn: String,
    val brewMethodCode: String?,
    val beanName: String?,
    val doseGrams: Double?,
    val brewDurationSeconds: Int?,
    val tasteScore: Int?,
    val roasterName: String? = null,
    /** The catalogue coffee's current public slug while it is published. */
    val coffeeProductSlug: String? = null,
    /** The entry's own copy of its shelf-bag card, when the author shows the bag. */
    val shelfBagCardUrl: String? = null,
)
