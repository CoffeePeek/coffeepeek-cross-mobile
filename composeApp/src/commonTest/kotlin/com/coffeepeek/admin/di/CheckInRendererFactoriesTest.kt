package com.coffeepeek.admin.di

import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateFlowRenderer
import com.coffeepeek.admin.ui.screen.checkins.CheckInHistoryScreenRenderer
import com.coffeepeek.admin.ui.screen.checkins.LegacyCheckInCreateFlowRenderer
import com.coffeepeek.admin.ui.screen.checkins.LegacyCheckInHistoryScreenRenderer
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
            LegacyCheckInHistoryScreenRenderer
        })
        val app = graph(factories)
        try {
            assertSame(LegacyCheckInCreateFlowRenderer, app.koin.get<CheckInCreateFlowRenderer>())
            assertEquals(0, historyReads)
            assertSame(LegacyCheckInHistoryScreenRenderer, app.koin.get<CheckInHistoryScreenRenderer>())
            assertEquals(1, historyReads)
        } finally {
            app.close()
        }
    }

    private fun graph(factories: CheckInRendererFactories) = koinApplication {
        modules(module {
            single<CheckInCreateFlowRenderer> { factories.create(this) }
            single<CheckInHistoryScreenRenderer> { factories.history(this) }
        })
    }
}
