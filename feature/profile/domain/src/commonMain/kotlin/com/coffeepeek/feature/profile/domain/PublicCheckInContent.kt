package com.coffeepeek.feature.profile.domain

/** The public record's content, independent of the app's old timeline model. */
data class PublicCheckInScores(val place: Int, val service: Int, val coffee: Int) {
    val average: Double get() = (place + service + coffee) / 3.0
}

data class PublicCheckInDrink(val customName: String?, val nameRu: String?, val nameEn: String?)

data class PublicCheckInPreparation(
    val beanName: String?, val brewMethodCode: String?, val doseGrams: Double?,
    val brewDurationSeconds: Int?, val tasteScore: Int?,
)
