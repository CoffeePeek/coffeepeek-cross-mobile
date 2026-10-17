package com.coffeepeek.admin.di.checkinhistory

import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.feature.checkinhistory.data.repository.createCheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.domain.repository.HomeHistoryReader
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationReader
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import com.coffeepeek.feature.homecheckin.data.repository.createHomePreparationRepository
import org.koin.dsl.module

/** Resolving this graph neither opens an account lease nor reads credentials; Content owns the lease. */
internal fun checkInHistoryModule(apiBaseUrl: String = Constants.BASE_URL) = module {
    single<CheckInHistoryRepository> { createCheckInHistoryRepository(get<CoffeePeekClient>().client, apiBaseUrl) }
    single<HomePreparationRepository> {
        val client = get<CoffeePeekClient>()
        createHomePreparationRepository(client.client, client.uploadClient, apiBaseUrl)
    }
    single<HomePreparationReader> { get<HomePreparationRepository>() }
    single<HomeHistoryReader> { HomeHistoryReaderAdapter(get()) }
    single { AndroidCheckInHistoryCalendar() }
    single<HistoryHomeEditorFactory> { historyHomeEditorFactory(get(), get(), get(), get()) }
    single { AndroidCheckInHistoryScreenRenderer(get(), get(), get(), get(), get()) }
}
