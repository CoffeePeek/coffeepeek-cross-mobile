package com.coffeepeek.admin.di

import com.coffeepeek.admin.di.coffee.coffeeModule
import com.coffeepeek.admin.di.shelf.iosShelfModule

actual fun initPlatformKoin() {
    // The shelf and the catalogue picker it searches are assembled ahead of the screens that will open them on iOS.
    initKoin(platformModules = listOf(coffeeModule(), iosShelfModule()))
}
