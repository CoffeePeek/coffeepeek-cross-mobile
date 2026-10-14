package com.coffeepeek.domain.feature.feed

/** Read-only recipe summary carried by the existing public feed contract. */
data class FeedHomePreparation(
    val preparedOn: String,
    val brewMethodCode: String?,
    val beanName: String?,
    val doseGrams: Double?,
    val brewDurationSeconds: Int?,
    val tasteScore: Int?,
)
