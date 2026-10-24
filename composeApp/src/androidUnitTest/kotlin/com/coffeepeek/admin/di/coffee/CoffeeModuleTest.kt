package com.coffeepeek.admin.di.coffee

import com.coffeepeek.admin.sandbox.backend.SandboxBackend
import com.coffeepeek.api.CoffeePeekClient
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
        val app = koinApplication { modules(module { single { client } }, coffeeModule()) }
        try {
            val repository = app.koin.get<CoffeeRepository>()
            assertSame(repository, app.koin.get<CoffeeRepository>())
            assertEquals("Sandbox Ethiopia", repository.search("", CoffeeFilters(), 1).getOrThrow().items.first().name)
            val picker = app.koin.get<CoffeePicker>()
            assertEquals(listOf("sandbox-kenya"), picker.search("  Kenya ").getOrThrow().map { it.slug })
            assertNotNull(featureCoffeeScreenRenderer(repository))
        } finally {
            app.close(); client.client.close(); client.plainClient.close(); client.uploadClient.close(); client.imageClient.close(); backend.close()
        }
    }
}
