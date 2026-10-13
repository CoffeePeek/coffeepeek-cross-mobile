package com.coffeepeek.admin.feature.coffee.data

import com.coffeepeek.admin.feature.coffee.api.CoffeePicker
import com.coffeepeek.admin.feature.coffee.api.CoffeePickerItem
import com.coffeepeek.admin.feature.coffee.domain.CoffeeFilters
import com.coffeepeek.admin.feature.coffee.domain.CoffeeRepository

internal class CoffeePickerAdapter(private val repository: CoffeeRepository) : CoffeePicker {
    override suspend fun search(query: String): Result<List<CoffeePickerItem>> =
        repository.search(query.trim(), CoffeeFilters(), 1).map { page ->
            page.items.map { CoffeePickerItem(it.slug, it.name, it.roasterName) }
        }
}
