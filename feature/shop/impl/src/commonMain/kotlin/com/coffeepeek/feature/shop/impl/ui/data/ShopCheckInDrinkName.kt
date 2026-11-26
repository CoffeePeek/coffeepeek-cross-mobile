package com.coffeepeek.feature.shop.impl.ui.data

import com.coffeepeek.feature.shop.domain.model.ShopCheckIn

internal fun ShopCheckIn.displayDrinkName(languageTag: String): String? {
    val english = languageTag.substringBefore('-').equals("en", ignoreCase = true)
    val preferred = if (english) drinkNameEn else drinkNameRu
    val fallback = if (english) drinkNameRu else drinkNameEn
    return customDrinkName?.takeIf(String::isNotBlank)
        ?: preferred?.takeIf(String::isNotBlank)
        ?: fallback?.takeIf(String::isNotBlank)
}
