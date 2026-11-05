package com.coffeepeek.admin.di.coffee

import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.feature.coffee.api.CoffeePicker
import com.coffeepeek.feature.coffee.data.repository.createCoffeeRepository
import com.coffeepeek.feature.coffee.domain.repository.CoffeeRepository
import com.coffeepeek.feature.coffee.impl.createCoffeePicker
import org.koin.dsl.module

/** Shared app assembly; both platforms reuse the existing plain client and its policy. */
internal fun coffeeModule() = module {
    single<CoffeeRepository> { createCoffeeRepository(get<ApiClients>().plainClient) }
    single<CoffeePicker> { createCoffeePicker(get()) }
}
