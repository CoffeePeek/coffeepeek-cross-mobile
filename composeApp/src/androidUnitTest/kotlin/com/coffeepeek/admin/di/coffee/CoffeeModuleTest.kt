package com.coffeepeek.admin.di.coffee

import com.coffeepeek.admin.sandbox.backend.SandboxBackend
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.core.network.SessionCredentials
import com.coffeepeek.core.network.SessionTokens
import com.coffeepeek.admin.di.homecheckin.homeCheckInModule
import com.coffeepeek.admin.di.shelf.shelfModule
import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.admin.settings.FakeSettingRepository
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.core.session.SessionStore
import com.coffeepeek.feature.coffee.api.CoffeePicker
import com.coffeepeek.feature.coffee.domain.model.CoffeeFilters
import com.coffeepeek.feature.coffee.domain.repository.CoffeeRepository
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.time.LocalDate
import kotlin.test.*

class CoffeeModuleTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun moduleBuildsMigratedRepositoryPickerAndRendererOverThePlainClient() = runTest {
        val backend = SandboxBackend(byteArrayOf(1), today = LocalDate.parse("2026-10-18"))
        val client = ApiClients(SandboxBackend.BASE_URL, temporary.newFolder().path, false,
            credentials = object : SessionCredentials {
                override suspend fun load(): SessionTokens? { return null }
                override suspend fun save(tokens: SessionTokens?) { error("No credential writes") }
                override suspend fun replace(expected: SessionTokens, tokens: SessionTokens?): Boolean = false
            },
            refreshWith = { { Result.failure(IllegalStateException("unused")) } },
            apiClientFactory = backend::apiClient, uploadClientFactory = backend::uploadClient)
        val app = coffeeShelfApplication(client)
        try {
            val repository = app.koin.get<CoffeeRepository>()
            assertSame(repository, app.koin.get<CoffeeRepository>())
            assertEquals("Sandbox Ethiopia", repository.search("", CoffeeFilters(), 1).getOrThrow().items.first().name)
            val picker = app.koin.get<CoffeePicker>()
            assertEquals(listOf("sandbox-kenya"), picker.search("  Kenya ").getOrThrow().map { it.slug })
            val shelf = CoffeeShelfTab(app.koin.get(), app.koin.get(), FakeSettingRepository())
            assertNotNull(featureCoffeeScreenRenderer(repository, shelf))
        } finally {
            app.close()
            closeClients(client, backend)
        }
    }

    private fun coffeeShelfApplication(client: ApiClients) = koinApplication {
        modules(module {
            single { client }
            single<SessionStore> { CheckInTestSessions() }
            single<SessionReader> { get<SessionStore>() }
        }, coffeeModule(), homeCheckInModule(apiBaseUrl = SandboxBackend.BASE_URL),
            shelfModule(SandboxBackend.BASE_URL, context = { error("Resolving the shelf must not touch Context") }))
    }

    private fun closeClients(client: ApiClients, backend: SandboxBackend) {
        client.client.close()
        client.plainClient.close()
        client.uploadClient.close()
        backend.close()
    }
}
