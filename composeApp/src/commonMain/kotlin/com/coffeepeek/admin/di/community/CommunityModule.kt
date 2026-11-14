package com.coffeepeek.admin.di.community

import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.admin.locator.Constants
import org.koin.dsl.module

internal fun communityModule() = module {
    single<com.coffeepeek.feature.community.domain.repository.CommunityRepository> {
        com.coffeepeek.feature.community.data.repository.createCommunityRepository(get<ApiClients>().client, Constants.BASE_URL)
    }
    single<com.coffeepeek.feature.community.api.CommunityAccount> { com.coffeepeek.admin.di.community.SessionCommunityAccount(get(), get()) }
    single<com.coffeepeek.feature.community.api.ConsumedDrinks> { com.coffeepeek.admin.di.community.ShopCommunityDrinks(get()) }
    single<com.coffeepeek.feature.community.api.CommunityAnalytics> { com.coffeepeek.admin.di.community.AppCommunityAnalytics() }
    single<com.coffeepeek.feature.community.api.CommunitySavedPosts> { com.coffeepeek.feature.community.impl.createCommunitySavedPosts() }
}
