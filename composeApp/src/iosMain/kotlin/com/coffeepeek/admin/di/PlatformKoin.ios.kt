package com.coffeepeek.admin.di

import com.coffeepeek.admin.di.coffee.CoffeeShelfTab
import com.coffeepeek.admin.di.coffee.coffeeCatalogueScreenRenderer
import com.coffeepeek.admin.di.coffee.coffeeModule
import com.coffeepeek.admin.di.homecheckin.FeatureCheckInCreateFlowRenderer
import com.coffeepeek.admin.di.homecheckin.homeCheckInModule
import com.coffeepeek.admin.di.shelf.ShelfFeatureScreenRenderer
import com.coffeepeek.admin.di.shelf.shelfBrewForm
import com.coffeepeek.admin.di.shelf.iosShelfEnabled
import com.coffeepeek.admin.di.shelf.iosShelfModule
import com.coffeepeek.admin.locator.Locator
import com.coffeepeek.admin.ui.screen.shelf.UnsupportedShelfScreenRenderer

actual fun initPlatformKoin() {
    val shelf = iosShelfEnabled()
    initKoin(
        platformModules = listOf(coffeeModule(), homeCheckInModule(shelfEnabled = shelf), iosShelfModule()),
        // The Coffee tab is the migrated catalogue; the shelf joins it only where the build enables it (SHELF_ENABLED).
        coffeeRendererFactory = {
            coffeeCatalogueScreenRenderer(get(), CoffeeShelfTab(get(), get(), Locator.database.settingRepository).takeIf { shelf })
        },
        // The "+" sheet is the migrated flow: a visit, a home check-in or, with the shelf, a bag to scan.
        checkInCreateFlowRendererFactory = { get<FeatureCheckInCreateFlowRenderer>() },
        shelfRendererFactory = {
            if (shelf) ShelfFeatureScreenRenderer(get(), Locator.database.settingRepository, shelfBrewForm(get()))
            else UnsupportedShelfScreenRenderer
        },
    )
}
