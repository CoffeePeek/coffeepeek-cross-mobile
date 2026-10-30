package com.coffeepeek.admin.di.shelf

import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.core.network.SessionCredentials
import com.coffeepeek.core.network.SessionTokens
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
        val client = ApiClients("https://fixture.invalid", temporary.newFolder("cache").path, false,
            credentials = object : SessionCredentials {
                override suspend fun load(): SessionTokens? { error("Unexpected credentials read") }
                override suspend fun save(tokens: SessionTokens?) { error("Unexpected credentials write") }
                override suspend fun replace(expected: SessionTokens, tokens: SessionTokens?): Boolean = false
            },
            refreshWith = { { Result.failure(IllegalStateException("unused")) } })
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
