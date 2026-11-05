package com.coffeepeek.admin.di.homecheckin

import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.feature.homecheckin.data.repository.createHomePreparationRepository
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationReader
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import org.koin.dsl.module

/** Shared data required by combined history; global creation retains its own app composition. */
internal fun homeCheckInDataModule(apiBaseUrl: String = Constants.BASE_URL) = module {
    single<HomePreparationRepository> {
        val client = get<CoffeePeekClient>()
        createHomePreparationRepository(client.client, client.uploadClient, apiBaseUrl)
    }
    single<HomePreparationReader> { get<HomePreparationRepository>() }
}
