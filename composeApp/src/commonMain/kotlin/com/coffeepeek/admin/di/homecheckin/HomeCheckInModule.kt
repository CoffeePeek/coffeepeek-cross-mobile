package com.coffeepeek.admin.di.homecheckin

import com.coffeepeek.admin.analytics.CheckInSource
import com.coffeepeek.admin.feature.catalog.api.BrewMethodCatalog
import com.coffeepeek.feature.coffee.api.CoffeePicker
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.feature.homecheckin.data.repository.createHomePreparationRepository
import com.coffeepeek.feature.homecheckin.data.repository.createHomePreparationShelfBagReader
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationReader
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import com.coffeepeek.feature.notifications.api.NotificationFeature
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationShelfBagReader
import com.coffeepeek.admin.di.shelf.ShelfHomeCheckInBags
import org.koin.dsl.module

/** Home data is shared by history and global creation; with [shelfEnabled] its forms offer shelf bags. Resolving performs no work. */
internal fun homeCheckInModule(shelfEnabled: Boolean, apiBaseUrl: String = Constants.BASE_URL) = module {
    single<HomePreparationRepository> {
        val client = get<CoffeePeekClient>()
        createHomePreparationRepository(client.client, client.uploadClient, apiBaseUrl)
    }
    single<HomePreparationReader> { get<HomePreparationRepository>() }
    single<HomePreparationShelfBagReader> { createHomePreparationShelfBagReader(get<CoffeePeekClient>().client, apiBaseUrl) }
    single {
        val repository = get<HomePreparationRepository>()
        val picker = get<CoffeePicker>()
        val methods = get<BrewMethodCatalog>()
        val shelfBags = if (shelfEnabled) ShelfHomeCheckInBags(get()) else null
        FeatureCheckInCreateFlowRenderer(get(), shelfEnabled, onCreated = { get<NotificationFeature>().onCheckInCreated() }) { lease, tracker ->
            appHomeCheckInEditor(lease, repository, picker, methods, CheckInSource.Global, tracker, shelfBags)
        }
    }
}
