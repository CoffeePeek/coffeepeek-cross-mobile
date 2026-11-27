package com.coffeepeek.admin.di.discovery

import org.koin.core.annotation.KoinInternalApi
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals

class DiscoveryBindingsTest {
    @OptIn(KoinInternalApi::class)
    @Test
    fun bothContractsUseOneInstanceFactory() {
        val bindings = module { discoveryBindings { error("Registering bindings must not construct the renderer") } }

        assertEquals(2, bindings.mappings.size)
        // R8 can merge the contract classes into one key. Neither key may point to a resolving alias.
        assertEquals(1, bindings.mappings.values.toSet().size)
    }
}
