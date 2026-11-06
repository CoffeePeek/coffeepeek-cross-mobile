package com.coffeepeek.admin.di

import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateFlowRenderer
import com.coffeepeek.admin.ui.screen.checkins.CheckInHistoryScreenRenderer
import com.coffeepeek.admin.ui.screen.checkins.CheckInShopPickerRenderer
import androidx.compose.runtime.Composable
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CheckInRendererFactoriesTest {
    @Test fun resolvingCreationDoesNotEagerlyConstructHistory() {
        var historyReads = 0
        val factories = CheckInRendererFactories(history = {
            historyReads++
            History
        }, create = { Creation }, shopPicker = { Picker })
        val app = graph(factories)
        try {
            assertSame(Creation, app.koin.get<CheckInCreateFlowRenderer>())
            assertEquals(0, historyReads)
            assertSame(History, app.koin.get<CheckInHistoryScreenRenderer>())
            assertEquals(1, historyReads)
        } finally {
            app.close()
        }
    }

    private fun graph(factories: CheckInRendererFactories) = koinApplication {
        modules(module {
            single<CheckInCreateFlowRenderer> { factories.create(this) }
            single<CheckInHistoryScreenRenderer> { factories.history(this) }
            single<CheckInShopPickerRenderer> { factories.shopPicker(this) }
        })
    }

    @Test fun resolvingCreationDoesNotConstructShopSearchUntilRequested() {
        var pickerReads = 0
        val factories = CheckInRendererFactories(history = { History }, create = { Creation }, shopPicker = {
            pickerReads++
            Picker
        })
        val app = graph(factories)
        try {
            assertSame(Creation, app.koin.get<CheckInCreateFlowRenderer>())
            assertEquals(0, pickerReads)
            assertSame(Picker, app.koin.get<CheckInShopPickerRenderer>())
            assertSame(Picker, app.koin.get<CheckInShopPickerRenderer>())
            assertEquals(1, pickerReads)
        } finally {
            app.close()
        }
    }

    // Non-renderable DI doubles: no UI host, Activity, instrumentation or account.
    private object History : CheckInHistoryScreenRenderer {
        @Composable override fun Content(initialHomeId: String?) = error("DI tests must not render")
    }

    private object Creation : CheckInCreateFlowRenderer {
        @Composable override fun Content(onDismiss: () -> Unit, onShop: () -> Unit, onHomeSaved: () -> Unit) = error("DI tests must not render")
    }

    private object Picker : CheckInShopPickerRenderer {
        @Composable override fun Content() = error("DI tests must not render")
    }
}
