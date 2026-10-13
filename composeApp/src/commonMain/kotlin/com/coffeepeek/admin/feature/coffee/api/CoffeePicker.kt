package com.coffeepeek.admin.feature.coffee.api

/** Minimal cross-feature contract for selecting a published catalog product. */
internal data class CoffeePickerItem(val slug: String, val name: String, val roasterName: String)

internal interface CoffeePicker {
    suspend fun search(query: String): Result<List<CoffeePickerItem>>
}
