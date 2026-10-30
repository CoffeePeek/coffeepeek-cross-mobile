package com.coffeepeek.admin.di.shelf

import android.content.Context
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.locator.Locator
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.shelf.api.ShelfEntry
import com.coffeepeek.feature.shelf.data.repository.createShelfRepository
import com.coffeepeek.feature.shelf.data.scan.createAndroidBagScanner
import com.coffeepeek.feature.shelf.domain.repository.BagScanner
import com.coffeepeek.feature.shelf.domain.repository.ShelfRepository
import com.coffeepeek.feature.shelf.impl.ShelfPorts
import com.coffeepeek.feature.shelf.impl.createShelfEntry
import org.koin.dsl.module

/** Shelf data, scanner and screens; resolving definitions performs no network, session, file or model work. */
internal fun shelfModule(
    apiBaseUrl: String = Constants.BASE_URL,
    context: () -> Context = { Locator.appContext },
    models: (() -> Context) -> ShelfScannerModels = ::productionShelfModels,
) = module {
    single<ShelfRepository> {
        val client = get<ApiClients>()
        createShelfRepository(client.client, client.uploadClient, apiBaseUrl, SessionShelfAccount(get<SessionReader>()))
    }
    single<BagScanner> {
        val chosen = models(context)
        createAndroidBagScanner(context, chosen.segmenter, chosen.inpainter)
    }
    single<ShelfEntry> {
        val brews = ShelfHomeBrews(get(), SessionShelfAccount(get()))
        createShelfEntry(get(), ShelfPorts(get(), ShelfCatalogSearch(get()), ShelfCardImage, AndroidShelfGallery, AndroidShelfCamera,
            AndroidShelfTimeZone, brews))
    }
}
