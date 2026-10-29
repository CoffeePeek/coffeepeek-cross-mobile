package com.coffeepeek.admin.di.shelf

import com.coffeepeek.feature.coffee.api.CoffeePicker
import com.coffeepeek.feature.shelf.api.ShelfCoffeeOption
import com.coffeepeek.feature.shelf.api.ShelfCoffeeSearch

/** The coffee feature's picker, seen by the shelf only through its own small contract. */
internal class ShelfCatalogSearch(private val picker: CoffeePicker) : ShelfCoffeeSearch {
    override suspend fun search(query: String): Result<List<ShelfCoffeeOption>> =
        picker.search(query).map { items -> items.map { ShelfCoffeeOption(it.slug, it.name, it.roasterName) } }
}
