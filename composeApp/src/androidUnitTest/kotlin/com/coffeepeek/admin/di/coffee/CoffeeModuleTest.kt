package com.coffeepeek.admin.di.coffee

import com.coffeepeek.admin.sandbox.backend.SandboxBackend
import com.coffeepeek.admin.di.homecheckin.homeCheckInModule
import com.coffeepeek.admin.di.shelf.shelfModule
import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.admin.settings.FakeSettingRepository
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.core.session.SessionReader
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
        val client = CoffeePeekClient(SandboxBackend.BASE_URL, temporary.newFolder().path, false,
            getToken = { null }, saveToken = { error("No credential writes") },
            apiClientFactory = backend::apiClient, uploadClientFactory = backend::uploadClient)
        val app = coffeeShelfApplication(client)
        try {
            val repository = app.koin.get<CoffeeRepository>()
            assertSame(repository, app.koin.get<CoffeeRepository>())
            assertEquals("Sandbox Ethiopia", repository.search("", CoffeeFilters(), 1).getOrThrow().items.first().name)
            val picker = app.koin.get<CoffeePicker>()
            assertEquals(listOf("sandbox-kenya"), picker.search("  Kenya ").getOrThrow().map { it.slug })
            val shelf = CoffeeShelfTab(app.koin.get(), app.koin.get(), FakeSettingRepository())
            assertNotNull(coffeeCatalogueScreenRenderer(repository, shelf))
        } finally {
            app.close()
            closeClients(client, backend)
        }
    }

    private fun coffeeShelfApplication(client: CoffeePeekClient) = koinApplication {
        modules(module {
            single { client }
            single<SessionReader> { CheckInTestSessions() }
        }, coffeeModule(), homeCheckInModule(apiBaseUrl = SandboxBackend.BASE_URL),
            shelfModule(SandboxBackend.BASE_URL, context = { error("Resolving the shelf must not touch Context") }))
    }

    private fun closeClients(client: CoffeePeekClient, backend: SandboxBackend) {
        client.client.close()
        client.plainClient.close()
        client.uploadClient.close()
        client.imageClient.close()
        backend.close()
    }
}
