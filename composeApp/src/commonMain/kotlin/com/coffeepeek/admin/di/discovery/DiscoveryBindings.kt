package com.coffeepeek.admin.di.discovery

import com.coffeepeek.admin.ui.screen.main.DiscoveryScreenRenderer
import org.koin.core.module.Module
import org.koin.core.scope.Scope
import org.koin.dsl.binds

/** Both entries resolve the same composition-owned factory and repository adapters. */
internal fun Module.discoveryBindings(factory: Scope.() -> DiscoveryScreenRenderer) {
    // R8 may merge both contract classes. Share the factory so a collapsed key cannot resolve itself.
    single<DiscoveryScreenRenderer> { factory() }.binds(arrayOf(NativeDiscoverySessionFactory::class))
}
