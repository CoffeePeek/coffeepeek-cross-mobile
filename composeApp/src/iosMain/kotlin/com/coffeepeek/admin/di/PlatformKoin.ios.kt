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
import com.coffeepeek.admin.di.shelf.iosShelfEnabled
import com.coffeepeek.admin.di.shelf.iosShelfModule
import com.coffeepeek.admin.locator.Locator
import com.coffeepeek.admin.ui.screen.shelf.UnsupportedShelfScreenRenderer

actual fun initPlatformKoin() {
    val shelf = iosShelfEnabled()
    initKoin(
        platformModules = listOf(coffeeModule(), homeCheckInModule(shelfEnabled = shelf), checkInHistoryModule(shelfEnabled = shelf), iosShelfModule()),
        // The Coffee tab is the migrated catalogue; the shelf joins it only where the build enables it (SHELF_ENABLED).
        coffeeRendererFactory = {
            coffeeCatalogueScreenRenderer(get(), CoffeeShelfTab(get(), get(), Locator.database.settingRepository).takeIf { shelf })
        },
        // The "+" sheet is the migrated flow: a visit, a home check-in or, with the shelf, a bag to scan.
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
            if (shelf) ShelfFeatureScreenRenderer(get(), Locator.database.settingRepository, shelfBrewForm(get()))
            else UnsupportedShelfScreenRenderer
        },
    )
}
