package com.coffeepeek.admin.di.coffee

import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.feature.coffee.api.CoffeePicker
import com.coffeepeek.feature.coffee.data.repository.createCoffeeRepository
import com.coffeepeek.feature.coffee.domain.repository.CoffeeRepository
import com.coffeepeek.feature.coffee.impl.createCoffeePicker
import org.koin.dsl.module

/** App-owned assembly of the migrated catalogue; the plain client keeps its existing lifetime and policy. */
internal fun coffeeModule() = module {
    single<CoffeeRepository> { createCoffeeRepository(get<CoffeePeekClient>().plainClient) }
    single<CoffeePicker> { createCoffeePicker(get()) }
}
