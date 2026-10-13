package com.coffeepeek.domain.model

enum class CheckInVisibility { Private, Public }

enum class CheckInModerationState { NotSubmitted, Pending, Approved, Rejected, Unknown }

data class UpdateCheckInInput(
    val text: String,
    val rating: ReviewRating? = null,
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
)

fun validateCheckInContent(text: String, rating: ReviewRating?, drinkSlug: String?, customDrinkName: String?): String? = when {
    text.trim().length > 1000 -> "Текст должен быть не длиннее 1000 символов"
    rating != null && listOf(rating.coffee, rating.service, rating.place).any { it !in 1..5 } -> "Оцените кофе, сервис и атмосферу от 1 до 5"
    else -> validateConsumedDrink(drinkSlug, customDrinkName)
}
