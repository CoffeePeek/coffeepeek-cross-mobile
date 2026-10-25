package com.coffeepeek.admin.di.homecheckin

import com.coffeepeek.admin.analytics.CheckInSource
import com.coffeepeek.admin.feature.catalog.api.BrewMethodCatalog
import com.coffeepeek.feature.coffee.api.CoffeePicker
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import org.koin.dsl.module

/** Home data is shared by history and global creation; resolving definitions performs no work. */
internal fun homeCheckInModule(apiBaseUrl: String = Constants.BASE_URL) = module {
    includes(homeCheckInDataModule(apiBaseUrl))
    single {
        val repository = get<HomePreparationRepository>()
        val picker = get<CoffeePicker>()
        val methods = get<BrewMethodCatalog>()
        FeatureCheckInCreateFlowRenderer(get()) { lease, tracker ->
            appHomeCheckInEditor(lease, repository, picker, methods, CheckInSource.Global, tracker)
        }
    }
}
