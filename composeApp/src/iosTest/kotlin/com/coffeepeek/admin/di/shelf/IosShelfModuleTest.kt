package com.coffeepeek.admin.di.shelf

import com.coffeepeek.admin.di.coffee.coffeeModule
import com.coffeepeek.admin.di.coffee.coffeeCatalogueScreenRenderer
import com.coffeepeek.admin.di.coffee.CoffeeShelfTab
import com.coffeepeek.admin.di.checkinhistory.CheckInHistoryFeatureScreenRenderer
import com.coffeepeek.admin.di.checkinhistory.HistoryHomeEditorFactory
import com.coffeepeek.admin.di.checkinhistory.checkInHistoryModule
import com.coffeepeek.admin.feature.catalog.api.BrewMethodCatalog
import com.coffeepeek.admin.di.homecheckin.FeatureCheckInCreateFlowRenderer
import com.coffeepeek.admin.di.homecheckin.homeCheckInModule
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.admin.settings.FakeSettingRepository
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.coffee.api.CoffeePicker
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationShelfBagReader
import com.coffeepeek.feature.shelf.api.ShelfEntry
import com.coffeepeek.feature.shelf.domain.repository.BagScanner
import com.coffeepeek.feature.shelf.domain.repository.ShelfRepository
import kotlinx.coroutines.flow.Flow
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.core.Koin
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertNotNull

class IosShelfModuleTest {
    @Test fun graphResolvesLazilyWithoutSessionsCredentialsOrModels() {
        val client = shelfClient()
        val requested = mutableListOf<String>()
        val app = shelfApplication(client, requested)
        try {
            assertShelfGraph(app.koin)
            assertEquals(listOf("ShelfSegmenterURL", "ShelfInpainterURL"), requested)
        } finally {
            app.close()
            client.client.close()
            client.plainClient.close()
            client.uploadClient.close()
            client.imageClient.close()
        }
    }

    private fun shelfClient() = CoffeePeekClient("https://fixture.invalid", "${NSTemporaryDirectory()}shelf-${NSUUID().UUIDString}", false,
        getToken = { error("Unexpected credentials read") }, saveToken = { error("Unexpected credentials write") })

    private fun unreadSessions() = object : SessionReader {
        override fun peekSession(): Session? = error("Resolving the graph must not read the session")
        override fun isActiveSession(session: Session?): Boolean = error("Unexpected session check")
        override fun observeSession(): Flow<Session?> = error("Unexpected session observation")
        override suspend fun getSession(): Session? = error("Unexpected session load")
    }

    private fun shelfApplication(client: CoffeePeekClient, requested: MutableList<String>) = koinApplication {
        modules(module {
            single { client }
            single<SessionReader> { unreadSessions() }
            single<BrewMethodCatalog> { BrewMethodCatalog { error("Resolving the graph must not read brew methods") } }
        }, coffeeModule(), homeCheckInModule(apiBaseUrl = "https://fixture.invalid"),
            checkInHistoryModule(apiBaseUrl = "https://fixture.invalid"),
            iosShelfModule("https://fixture.invalid") { key ->
                requested += key
                "https://models.invalid/$key"
            })
    }

    private fun assertShelfGraph(koin: Koin) {
        assertSame(koin.get<ShelfRepository>(), koin.get<ShelfRepository>())
        assertSame(koin.get<BagScanner>(), koin.get<BagScanner>())
        assertSame(koin.get<ShelfEntry>(), koin.get<ShelfEntry>())
        koin.get<CoffeePicker>()
        koin.get<HomePreparationShelfBagReader>()
        assertSame(koin.get<FeatureCheckInCreateFlowRenderer>(), koin.get<FeatureCheckInCreateFlowRenderer>())
        assertSame(koin.get<HistoryHomeEditorFactory>(), koin.get<HistoryHomeEditorFactory>())
        assertSame(koin.get<CheckInHistoryFeatureScreenRenderer>(), koin.get<CheckInHistoryFeatureScreenRenderer>())
        val shelf = CoffeeShelfTab(koin.get(), koin.get(), FakeSettingRepository())
        assertNotNull(coffeeCatalogueScreenRenderer(koin.get(), shelf))
    }

    @Test fun theTimeZoneOffsetFollowsTheDevice() {
        val offset = IosShelfTimeZone.offsetSeconds(1_761_696_000)
        assertEquals(true, offset in -14 * 3600..14 * 3600)
    }
}
