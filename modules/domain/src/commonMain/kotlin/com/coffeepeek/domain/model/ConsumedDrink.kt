package com.coffeepeek.domain.model

data class ConsumedDrinkOption(val slug: String, val nameRu: String, val nameEn: String)

fun validateConsumedDrink(slug: String?, customName: String?): String? = when {
    slug == "other" && customName.orEmpty().trim().length !in 1..100 -> "Укажите название напитка: от 1 до 100 символов"
    slug != "other" && customName != null -> "Название можно указать только для другого напитка"
    else -> null
}

fun savedDrinkName(nameRu: String?, nameEn: String?, customName: String?): String? =
    customName?.takeIf { it.isNotBlank() } ?: nameRu?.takeIf { it.isNotBlank() } ?: nameEn?.takeIf { it.isNotBlank() }
