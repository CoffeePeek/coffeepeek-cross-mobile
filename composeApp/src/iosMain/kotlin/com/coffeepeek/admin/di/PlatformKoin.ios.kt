package com.coffeepeek.admin.di

import com.coffeepeek.admin.di.checkinhistory.CheckInHistoryFeatureScreenRenderer
import com.coffeepeek.admin.di.checkinhistory.checkInHistoryModule
import com.coffeepeek.admin.di.coffee.CoffeeShelfTab
import com.coffeepeek.admin.di.coffee.coffeeCatalogueScreenRenderer
import com.coffeepeek.admin.di.coffee.coffeeModule
import com.coffeepeek.admin.di.community.checkInDetailFeatureScreenRenderer
import com.coffeepeek.admin.di.community.communityFeatureScreenRenderer
import com.coffeepeek.admin.di.homecheckin.FeatureCheckInCreateFlowRenderer
import com.coffeepeek.admin.di.homecheckin.homeCheckInModule
import com.coffeepeek.admin.di.profile.accountFeatureScreenRenderer
import com.coffeepeek.admin.di.profile.ownProfileFeatureScreenRenderer
import com.coffeepeek.admin.di.profile.publicProfileFeatureScreenRenderer
import com.coffeepeek.admin.di.shelf.ShelfFeatureScreenRenderer
import com.coffeepeek.admin.di.shelf.shelfBrewForm
import com.coffeepeek.admin.di.shelf.iosShelfModule
import com.coffeepeek.admin.locator.Locator

actual fun initPlatformKoin() {
    initKoin(
        platformModules = listOf(coffeeModule(), homeCheckInModule(), checkInHistoryModule(), iosShelfModule()),
        // Both platforms expose the shelf and catalogue through the same Coffee tab.
        coffeeRendererFactory = {
            coffeeCatalogueScreenRenderer(get(), CoffeeShelfTab(get(), get(), Locator.database.settingRepository))
        },
        // The "+" sheet offers a visit, a home check-in or a bag to scan.
        checkInCreateFlowRendererFactory = { get<FeatureCheckInCreateFlowRenderer>() },
        // Profiles, their edit screens and the check-in history are the migrated profile and history features.
        checkInHistoryRendererFactory = { get<CheckInHistoryFeatureScreenRenderer>() },
        publicProfileRendererFactory = { publicProfileFeatureScreenRenderer(get(), get(), get(), get(), get()) },
        ownProfileRendererFactory = { ownProfileFeatureScreenRenderer(get(), get(), get(), get(), get(), get(), get(), get()) },
        accountRendererFactory = { accountFeatureScreenRenderer(get(), get()) },
        // The feed and a post's own screen are the migrated community entries, home posts with their bag cards included.
        communityRendererFactory = { communityFeatureScreenRenderer(get(), get(), get(), get(), get(), get()) },
        checkInDetailRendererFactory = { checkInDetailFeatureScreenRenderer(get(), get(), get(), get()) },
        shelfRendererFactory = {
            ShelfFeatureScreenRenderer(get(), Locator.database.settingRepository, shelfBrewForm(get()))
        },
    )
}
