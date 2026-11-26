package com.coffeepeek.admin.di.discovery

import com.coffeepeek.admin.ui.screen.main.DiscoveryScreenRenderer
import org.koin.core.module.Module
import org.koin.core.scope.Scope

/** Both entries resolve the same composition-owned factory and repository adapters. */
internal fun Module.discoveryBindings(factory: Scope.() -> DiscoveryScreenRenderer) {
    single<DiscoveryScreenRenderer> { factory() }
    single<NativeDiscoverySessionFactory> { get<DiscoveryScreenRenderer>() as NativeDiscoverySessionFactory }
}
