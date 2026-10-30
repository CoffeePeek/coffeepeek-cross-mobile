package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.feature.shop.data.repository.createShopCheckInVoteRepository
import com.coffeepeek.feature.shop.data.repository.createShopDetailsRepository
import com.coffeepeek.feature.shop.api.PublicShopPreviews
import com.coffeepeek.feature.shop.data.repository.createPublicShopPreviews
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInVoteRepository
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import org.koin.dsl.module

/** App-owned assembly, not another Gradle DI module. Existing clients retain auth and upload policy. */
internal fun shopBrowsingModule(fileBaseUrl: String = Constants.BASE_URL) = module {
    single<ShopDetailsRepository> {
        createShopDetailsRepository(get<ApiClients>().client, fileBaseUrl,
            utcOffsetMinutes = ::currentShopUtcOffsetMinutes)
    }
    single<PublicShopPreviews> { createPublicShopPreviews(get<ApiClients>().client, ::currentShopUtcOffsetMinutes) }
    single<ShopCheckInVoteRepository> { createShopCheckInVoteRepository(get<ApiClients>().client) }
}

private fun currentShopUtcOffsetMinutes() = TimeZone.currentSystemDefault().offsetAt(Clock.System.now()).totalSeconds / 60
