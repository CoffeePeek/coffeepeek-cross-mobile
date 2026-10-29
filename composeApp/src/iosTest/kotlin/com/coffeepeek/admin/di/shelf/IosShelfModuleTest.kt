package com.coffeepeek.admin.di.shelf

import com.coffeepeek.admin.di.coffee.coffeeModule
import com.coffeepeek.api.CoffeePeekClient
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
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class IosShelfModuleTest {
    @Test fun graphResolvesLazilyWithoutSessionsCredentialsOrModels() {
        val client = CoffeePeekClient("https://fixture.invalid", "${NSTemporaryDirectory()}shelf-${NSUUID().UUIDString}", false,
            getToken = { error("Unexpected credentials read") }, saveToken = { error("Unexpected credentials write") })
        val sessions = object : SessionReader {
            override fun peekSession(): Session? = error("Resolving the graph must not read the session")
            override fun isActiveSession(session: Session?): Boolean = error("Unexpected session check")
            override fun observeSession(): Flow<Session?> = error("Unexpected session observation")
            override suspend fun getSession(): Session? = error("Unexpected session load")
        }
        val requested = mutableListOf<String>()
        val app = koinApplication {
            modules(module { single { client }; single<SessionReader> { sessions } }, coffeeModule(),
                iosShelfModule("https://fixture.invalid") { key -> requested += key; "https://models.invalid/$key" })
        }
        try {
            assertSame(app.koin.get<ShelfRepository>(), app.koin.get<ShelfRepository>())
            assertSame(app.koin.get<BagScanner>(), app.koin.get<BagScanner>())
            assertSame(app.koin.get<ShelfEntry>(), app.koin.get<ShelfEntry>())
            app.koin.get<CoffeePicker>()
            app.koin.get<HomePreparationShelfBagReader>()
            assertEquals(listOf("ShelfSegmenterURL", "ShelfInpainterURL"), requested)
        } finally {
            app.close()
            client.client.close(); client.plainClient.close(); client.uploadClient.close()
        }
    }

    @Test fun theTimeZoneOffsetFollowsTheDevice() {
        val offset = IosShelfTimeZone.offsetSeconds(1_761_696_000)
        assertEquals(true, offset in -14 * 3600..14 * 3600)
    }
}
