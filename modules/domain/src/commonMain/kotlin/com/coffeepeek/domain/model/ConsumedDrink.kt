package com.coffeepeek.domain.model

data class ConsumedDrinkOption(val slug: String, val nameRu: String, val nameEn: String)

enum class ConsumedDrinkIssue { CustomNameLength, CustomNameOnlyForOther }

fun consumedDrinkIssue(slug: String?, customName: String?): ConsumedDrinkIssue? = when {
    slug == "other" && customName.orEmpty().trim().length !in 1..100 -> ConsumedDrinkIssue.CustomNameLength
    slug != "other" && customName != null -> ConsumedDrinkIssue.CustomNameOnlyForOther
    else -> null
}

/** Technical compatibility errors used by repository preconditions; presentation consumes [consumedDrinkIssue]. */
fun validateConsumedDrink(slug: String?, customName: String?): String? = when (consumedDrinkIssue(slug, customName)) {
    ConsumedDrinkIssue.CustomNameLength -> "Укажите название напитка: от 1 до 100 символов"
    ConsumedDrinkIssue.CustomNameOnlyForOther -> "Название можно указать только для другого напитка"
    null -> null
}

fun savedDrinkName(nameRu: String?, nameEn: String?, customName: String?, language: String = "ru"): String? =
    customName?.takeIf { it.isNotBlank() }
        ?: if (language == "en") {
            nameEn?.takeIf { it.isNotBlank() } ?: nameRu?.takeIf { it.isNotBlank() }
        } else {
            nameRu?.takeIf { it.isNotBlank() } ?: nameEn?.takeIf { it.isNotBlank() }
        }
