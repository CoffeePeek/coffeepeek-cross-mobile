package com.coffeepeek.admin.di.checkinhistory

import com.coffeepeek.admin.di.shelf.ShelfHomeCheckInBags
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.feature.checkinhistory.data.repository.createCheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.domain.repository.HomeHistoryReader
import org.koin.dsl.module

/** Resolving this graph neither opens an account lease nor reads credentials; Content owns the lease. */
internal fun checkInHistoryModule(shelfEnabled: Boolean, apiBaseUrl: String = Constants.BASE_URL) = module {
    single<CheckInHistoryRepository> { createCheckInHistoryRepository(get<CoffeePeekClient>().client, apiBaseUrl) }
    single<HomeHistoryReader> { HomeHistoryReaderAdapter(get()) }
    single { AppCheckInHistoryCalendar() }
    single<HistoryHomeEditorFactory> {
        historyHomeEditorFactory(get(), get(), get(), if (shelfEnabled) ShelfHomeCheckInBags(get()) else null)
    }
    single { FeatureCheckInHistoryScreenRenderer(get(), get(), get(), get(), get()) }
    single { FeatureProfileActivityRenderer(get(), get(), get(), get()) }
}
