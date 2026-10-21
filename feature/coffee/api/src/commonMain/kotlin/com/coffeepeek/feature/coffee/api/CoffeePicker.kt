package com.coffeepeek.feature.coffee.api

/** Narrow public capability for selecting a published catalogue product, not its full domain model. */
interface CoffeePicker {
    suspend fun search(query: String): Result<List<CoffeePickerItem>>
}
