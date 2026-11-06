package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.ui.screen.shop.CheckInDraftStore
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.feature.notifications.api.NotificationFeature
import com.coffeepeek.feature.shop.api.ShopCheckInCreateEntry
import com.coffeepeek.feature.shop.data.repository.createShopCheckInRepository
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDateFormatter
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInPhotoPicker
import org.koin.dsl.module

/** App composition only. Resolving definitions does not open a draft or send a request. */
internal fun shopCheckInModule(dates: ShopCheckInDateFormatter) = module {
    single<ShopCheckInDateFormatter> { dates }
    single { AppShopCheckInDraftStore(get<CheckInDraftStore>(), get()) }
    single<ShopCheckInPhotoPicker> { AppShopCheckInPhotoPicker() }
    single<ShopCheckInRepository> {
        val client = get<ApiClients>()
        createShopCheckInRepository(apiClient = client.client, uploadClient = client.uploadClient)
    }
    single<ShopCheckInCreateEntry> {
        SessionShopCheckInEntry(sessions = get(), repository = get(), drafts = get(), dates = get(),
            photos = get(), onCheckInCreated = { get<NotificationFeature>().onCheckInCreated() })
    }
}
