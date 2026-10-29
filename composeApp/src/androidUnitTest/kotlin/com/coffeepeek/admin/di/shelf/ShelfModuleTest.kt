package com.coffeepeek.admin.di.shelf

import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.coffee.api.CoffeePicker
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationShelfBagReader
import com.coffeepeek.feature.shelf.api.ShelfEntry
import com.coffeepeek.feature.shelf.domain.repository.BagScanner
import com.coffeepeek.feature.shelf.domain.repository.ShelfRepository
import kotlinx.coroutines.flow.Flow
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ShelfModuleTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun graphResolvesLazilyWithoutSessionsCredentialsContextOrModels() {
        val client = CoffeePeekClient("https://fixture.invalid", temporary.newFolder("cache").path, false,
            getToken = { error("Unexpected credentials read") }, saveToken = { error("Unexpected credentials write") })
        val sessions = object : SessionReader {
            override fun peekSession(): Session? = error("Resolving the graph must not read the session")
            override fun isActiveSession(session: Session?): Boolean = error("Unexpected session check")
            override fun observeSession(): Flow<Session?> = error("Unexpected session observation")
            override suspend fun getSession(): Session? = error("Unexpected session load")
        }
        val app = koinApplication {
            modules(module {
                single { client }; single<SessionReader> { sessions }
                single<CoffeePicker> {
                    object : CoffeePicker { override suspend fun search(query: String) = error("Resolving the graph must not search the catalogue") }
                }
                single<HomePreparationShelfBagReader> {
                    HomePreparationShelfBagReader { _, _, _ -> error("Resolving the graph must not read check-ins") }
                }
            },
                shelfModule("https://fixture.invalid", context = { error("Resolving the graph must not touch the Context") }))
        }
        try {
            assertSame(app.koin.get<ShelfRepository>(), app.koin.get<ShelfRepository>())
            assertSame(app.koin.get<BagScanner>(), app.koin.get<BagScanner>())
            assertSame(app.koin.get<ShelfEntry>(), app.koin.get<ShelfEntry>())
            assertEquals(1, app.koin.getAll<ShelfRepository>().size)
        } finally {
            app.close()
            client.client.close(); client.plainClient.close(); client.uploadClient.close()
        }
    }
}
