package com.coffeepeek.admin.di.checkinhistory

import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.feature.checkinhistory.data.repository.createCheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.domain.repository.HomeHistoryReader
import org.koin.dsl.module

/** Resolving this graph neither opens an account lease nor reads credentials; Content owns the lease. */
internal fun checkInHistoryModule(apiBaseUrl: String = Constants.BASE_URL) = module {
    single<CheckInHistoryRepository> { createCheckInHistoryRepository(get<CoffeePeekClient>().client, apiBaseUrl) }
    single<HomeHistoryReader> { HomeHistoryReaderAdapter(get()) }
    single { AppCheckInHistoryCalendar() }
    single<HistoryHomeEditorFactory> { historyHomeEditorFactory(get(), get(), get()) }
    single { FeatureCheckInHistoryScreenRenderer(get(), get(), get(), get(), get()) }
}
