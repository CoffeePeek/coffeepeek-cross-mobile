package com.coffeepeek.admin.di.homecheckin

import com.coffeepeek.BuildConfig
import com.coffeepeek.admin.analytics.CheckInSource
import com.coffeepeek.admin.feature.catalog.api.BrewMethodCatalog
import com.coffeepeek.feature.coffee.api.CoffeePicker
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.feature.homecheckin.data.repository.createHomePreparationRepository
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationReader
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import org.koin.dsl.module

/** Home data is shared by history and global creation; resolving definitions performs no work. */
internal fun homeCheckInModule(apiBaseUrl: String = Constants.BASE_URL) = module {
    single<HomePreparationRepository> {
        val client = get<CoffeePeekClient>()
        createHomePreparationRepository(client.client, client.uploadClient, apiBaseUrl)
    }
    single<HomePreparationReader> { get<HomePreparationRepository>() }
    single {
        val repository = get<HomePreparationRepository>()
        val picker = get<CoffeePicker>()
        val methods = get<BrewMethodCatalog>()
        AndroidCheckInCreateFlowRenderer(get(), BuildConfig.SHELF_ENABLED) { lease, tracker ->
            androidHomeCheckInEditor(lease, repository, picker, methods, CheckInSource.Global, tracker)
        }
    }
}
