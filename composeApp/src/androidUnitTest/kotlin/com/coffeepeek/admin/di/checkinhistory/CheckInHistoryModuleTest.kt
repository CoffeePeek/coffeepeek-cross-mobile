package com.coffeepeek.admin.di.checkinhistory

import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.admin.di.homecheckin.homeCheckInModule
import com.coffeepeek.admin.di.homecheckin.FeatureCheckInCreateFlowRenderer
import com.coffeepeek.admin.di.shelf.shelfModule
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.admin.feature.catalog.api.BrewMethodCatalog
import com.coffeepeek.feature.coffee.api.CoffeePicker
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.domain.repository.HomeHistoryReader
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationReader
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.core.Koin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CheckInHistoryModuleTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun graphResolvesLazilyWithoutReadingCredentialsOrOpeningAccountLeases() {
        var credentials = 0
        var sessionReads = 0
        val client = CoffeePeekClient("https://fixture.invalid", temporary.newFolder("cache").path, false,
            getToken = { credentials++; error("Unexpected credentials read") },
            saveToken = { error("Unexpected credentials write") })
        val sessions = object : SessionRepository by CheckInTestSessions() {
            override fun peekSession(): com.coffeepeek.domain.model.Session? {
                sessionReads++
                error("Resolving a graph must not open an account lease")
            }
        }
        val app = historyApplication(client, sessions)
        try {
            assertHistoryGraph(app.koin)
            assertEquals(0, credentials)
            assertEquals(0, sessionReads)
        } finally {
            app.close()
            closeClients(client)
        }
    }

    private fun historyApplication(client: CoffeePeekClient, sessions: SessionRepository) = koinApplication {
        modules(module {
            single { client }
            single<SessionRepository> { sessions }
            single<SessionReader> { sessions }
        }, historyCatalogs(), homeCheckInModule(apiBaseUrl = "https://fixture.invalid"),
            checkInHistoryModule(apiBaseUrl = "https://fixture.invalid"),
            shelfModule("https://fixture.invalid", context = { error("Resolving the graph must not touch Context") }))
    }

    private fun historyCatalogs() = module {
        single<CoffeePicker> { object : CoffeePicker {
            override suspend fun search(query: String) = error("Unexpected catalog read")
        } }
        single<BrewMethodCatalog> { BrewMethodCatalog { error("Unexpected methods read") } }
    }

    private fun assertHistoryGraph(koin: Koin) {
        assertSame(koin.get<CheckInHistoryFeatureScreenRenderer>(), koin.get<CheckInHistoryFeatureScreenRenderer>())
        assertSame(koin.get<CheckInHistoryCalendar>(), koin.get<CheckInHistoryCalendar>())
        assertEquals(1, koin.getAll<CheckInHistoryRepository>().size)
        assertEquals(1, koin.getAll<HomePreparationReader>().size)
        assertSame(koin.get<HomePreparationRepository>(), koin.get<HomePreparationReader>())
        assertEquals(1, koin.getAll<HomeHistoryReader>().size)
        assertSame(koin.get<FeatureCheckInCreateFlowRenderer>(), koin.get<FeatureCheckInCreateFlowRenderer>())
    }

    private fun closeClients(client: CoffeePeekClient) {
        client.client.close()
        client.plainClient.close()
        client.uploadClient.close()
        client.imageClient.close()
    }
}
