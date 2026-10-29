package com.coffeepeek.admin.di.shelf

import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.homecheckin.data.repository.createHomePreparationShelfBagReader
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationShelfBagReader
import com.coffeepeek.feature.shelf.api.ShelfEntry
import com.coffeepeek.feature.shelf.api.ShelfTimeZone
import com.coffeepeek.feature.shelf.data.repository.createShelfRepository
import com.coffeepeek.feature.shelf.data.scan.IosBagScannerModel
import com.coffeepeek.feature.shelf.data.scan.createIosBagScanner
import com.coffeepeek.feature.shelf.domain.repository.BagScanner
import com.coffeepeek.feature.shelf.domain.repository.ShelfRepository
import com.coffeepeek.feature.shelf.impl.ShelfPorts
import com.coffeepeek.feature.shelf.impl.createShelfEntry
import org.koin.dsl.module
import platform.Foundation.NSBundle
import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone

/**
 * Shelf data, scanner and screens for iOS; resolving definitions performs no network, session, file or model work. Nothing
 * on iOS opens the shelf yet: its entry points live in screens that iOS still renders with the legacy implementation.
 */
internal fun iosShelfModule(apiBaseUrl: String = Constants.BASE_URL, modelUrl: (String) -> String = ::infoPlistString) = module {
    single<ShelfRepository> {
        val client = get<CoffeePeekClient>()
        createShelfRepository(client.client, client.uploadClient, apiBaseUrl, SessionShelfAccount(get<SessionReader>()))
    }
    single<HomePreparationShelfBagReader> { createHomePreparationShelfBagReader(get<CoffeePeekClient>().client, apiBaseUrl) }
    // Both models are downloaded on the first scan from the build's Info.plist addresses and checked against the pinned files.
    single<BagScanner> {
        createIosBagScanner(
            IosBagScannerModel(SEGMENTER_FILE, SEGMENTER_BYTES, SEGMENTER_SHA256, modelUrl("ShelfSegmenterURL")),
            IosBagScannerModel(INPAINTER_FILE, INPAINTER_BYTES, INPAINTER_SHA256, modelUrl("ShelfInpainterURL")),
        )
    }
    single<ShelfEntry> {
        val brews = ShelfHomeBrews(get(), SessionShelfAccount(get()))
        createShelfEntry(get(), ShelfPorts(get(), ShelfCatalogSearch(get()), ShelfCardImage, IosShelfGallery, IosShelfCamera,
            IosShelfTimeZone, brews))
    }
}

/** Read on every call, so a time-zone change while the app runs is picked up. */
internal val IosShelfTimeZone = ShelfTimeZone { epochSeconds ->
    NSTimeZone.localTimeZone.secondsFromGMTForDate(NSDate.dateWithTimeIntervalSince1970(epochSeconds.toDouble())).toInt()
}

private fun infoPlistString(key: String): String = (NSBundle.mainBundle.objectForInfoDictionaryKey(key) as? String).orEmpty().trim()
