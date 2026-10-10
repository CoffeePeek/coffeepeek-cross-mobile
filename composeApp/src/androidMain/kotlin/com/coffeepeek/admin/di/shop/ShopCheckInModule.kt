package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.ui.screen.shop.CheckInDraftStore
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.feature.shop.api.ShopCheckInCreateEntry
import com.coffeepeek.feature.shop.data.repository.createShopCheckInRepository
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository
import com.coffeepeek.feature.shop.impl.createShopCheckInCreateEntry
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDateFormatter
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInPhotoPicker
import org.koin.dsl.module

/** Android composition only. Resolving definitions does not open a draft or send a request. */
internal fun shopCheckInModule() = module {
    single<ShopCheckInDateFormatter> { AndroidShopCheckInDateFormatter() }
    single { AndroidShopCheckInDraftStore(get<CheckInDraftStore>(), get()) }
    single<ShopCheckInPhotoPicker> { AndroidShopCheckInPhotoPicker() }
    single<ShopCheckInRepository> {
        val client = get<CoffeePeekClient>()
        createShopCheckInRepository(apiClient = client.client, uploadClient = client.uploadClient)
    }
    single<ShopCheckInCreateEntry> {
        val drafts = get<AndroidShopCheckInDraftStore>()
        createShopCheckInCreateEntry(
            repository = get(), draftsForShop = drafts::forShop, dates = get(), photos = get(),
        )
    }
}
