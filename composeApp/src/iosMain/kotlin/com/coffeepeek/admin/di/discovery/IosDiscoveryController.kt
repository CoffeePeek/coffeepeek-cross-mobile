package com.coffeepeek.admin.di.discovery

import platform.UIKit.UIViewController

internal lateinit var discoveryControllerFactory: (IosDiscovery) -> UIViewController

internal fun configureNativeDiscovery(factory: (IosDiscovery) -> UIViewController) {
    discoveryControllerFactory = factory
}
