package com.coffeepeek.feature.coffee.impl

import com.coffeepeek.feature.coffee.api.CoffeePicker
import com.coffeepeek.feature.coffee.api.CoffeePickerItem
import com.coffeepeek.feature.coffee.domain.model.CoffeeFilters
import com.coffeepeek.feature.coffee.domain.repository.CoffeeRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Application composition supplies the repository; API callers never see it or its models. */
fun createCoffeePicker(repository: CoffeeRepository): CoffeePicker = CoffeePickerApiImpl(repository)

internal class CoffeePickerApiImpl(private val repository: CoffeeRepository) : CoffeePicker {
    override suspend fun search(query: String): Result<List<CoffeePickerItem>> {
        val result = repository.search(query.trim(), CoffeeFilters(), 1)
        currentCoroutineContext().ensureActive()
        // Even a misbehaving port returning cancellation as Result must not turn it into an error row.
        result.exceptionOrNull()?.let { if (it is kotlinx.coroutines.CancellationException) throw it }
        return result.map { page -> page.items.map { CoffeePickerItem(it.slug, it.name, it.roasterName) } }
    }
}
