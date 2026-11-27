package com.coffeepeek.admin.di.discovery

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.ui.screen.main.DiscoveryScreenRenderer
import com.coffeepeek.feature.discovery.impl.ui.NativeDiscoverySession
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.KoinInternalApi
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DiscoveryBindingsTest {
    @OptIn(KoinInternalApi::class)
    @Test
    fun bothContractsUseOneInstanceFactory() {
        val bindings = module { discoveryBindings { FakeRenderer() } }

        assertEquals(2, bindings.mappings.size)
        // R8 can merge the contract classes into one key. Neither key may point to a resolving alias.
        assertEquals(1, bindings.mappings.values.toSet().size)
    }

    @Test
    fun rendererFirstResolvesBothContractsLazilyOnce() {
        verifyResolution(nativeFirst = false)
    }

    @Test
    fun nativeFirstResolvesBothContractsLazilyOnce() {
        verifyResolution(nativeFirst = true)
    }

    private fun verifyResolution(nativeFirst: Boolean) {
        var constructions = 0
        val app = koinApplication {
            modules(module {
                discoveryBindings {
                    constructions++
                    FakeRenderer()
                }
            })
        }
        try {
            assertEquals(0, constructions)
            val first = if (nativeFirst) app.koin.get<NativeDiscoverySessionFactory>() else app.koin.get<DiscoveryScreenRenderer>()
            assertSame(first, app.koin.get<DiscoveryScreenRenderer>())
            assertSame(first, app.koin.get<NativeDiscoverySessionFactory>())
            assertEquals(1, constructions)
        } finally {
            app.close()
        }
    }

    private class FakeRenderer : DiscoveryScreenRenderer, NativeDiscoverySessionFactory {
        @Composable
        override fun Tab(onMapExpandedChange: (Boolean) -> Unit, reselections: Flow<Unit>) = Unit

        override fun createNativeSession(): NativeDiscoverySession = error("No native session is opened by DI resolution")
    }
}
