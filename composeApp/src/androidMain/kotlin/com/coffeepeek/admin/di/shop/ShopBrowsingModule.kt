package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.feature.shop.data.repository.createShopCheckInVoteRepository
import com.coffeepeek.feature.shop.data.repository.createShopDetailsRepository
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInVoteRepository
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import java.time.OffsetDateTime
import org.koin.dsl.module

/** App-owned assembly, not another Gradle DI module. Existing clients retain auth and upload policy. */
internal fun shopBrowsingModule(fileBaseUrl: String = Constants.BASE_URL) = module {
    single<ShopDetailsRepository> {
        createShopDetailsRepository(get<CoffeePeekClient>().client, fileBaseUrl,
            utcOffsetMinutes = { OffsetDateTime.now().offset.totalSeconds / 60 })
    }
    single<ShopCheckInVoteRepository> { createShopCheckInVoteRepository(get<CoffeePeekClient>().client) }
}
