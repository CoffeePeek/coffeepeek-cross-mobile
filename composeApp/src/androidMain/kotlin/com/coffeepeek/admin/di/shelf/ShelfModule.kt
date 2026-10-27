package com.coffeepeek.admin.di.shelf

import android.content.Context
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.locator.Locator
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.shelf.data.repository.createShelfRepository
import com.coffeepeek.feature.shelf.data.scan.createAndroidBagScanner
import com.coffeepeek.feature.shelf.domain.repository.BagScanner
import com.coffeepeek.feature.shelf.domain.repository.ShelfRepository
import org.koin.dsl.module

/** Shelf data and scanner; resolving definitions performs no network, session, file or model work. */
internal fun shelfModule(
    apiBaseUrl: String = Constants.BASE_URL,
    context: () -> Context = { Locator.appContext },
    models: (() -> Context) -> ShelfScannerModels = ::productionShelfModels,
) = module {
    single<ShelfRepository> {
        val client = get<CoffeePeekClient>()
        createShelfRepository(client.client, client.uploadClient, apiBaseUrl, SessionShelfAccount(get<SessionReader>()))
    }
    single<BagScanner> {
        val chosen = models(context)
        createAndroidBagScanner(context, chosen.segmenter, chosen.inpainter)
    }
}
