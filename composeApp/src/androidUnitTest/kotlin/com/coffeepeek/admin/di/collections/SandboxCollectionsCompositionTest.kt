package com.coffeepeek.admin.di.collections

import com.coffeepeek.admin.sandbox.backend.SandboxBackend
import com.coffeepeek.admin.sandbox.di.sandboxCollectionsModule
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.collections.domain.model.CollectionPlaceKind
import com.coffeepeek.feature.collections.domain.repository.CollectionsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.*

class SandboxCollectionsCompositionTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun actualSandboxBindingLoadsTheSameShopAndRoasterMembershipWithoutBearerCredentials() = runTest {
        val backend = SandboxBackend(byteArrayOf(1))
        val client = client(backend)
        val app = koinApplication {
            modules(seed(client), collectionsModule(SandboxBackend.BASE_URL), sandboxCollectionsModule())
        }
        try {
            val repository = app.koin.get<CollectionsRepository>()
            val favorites = repository.favorites().getOrThrow()
            assertEquals(setOf(CollectionPlaceKind.CoffeeShop, CollectionPlaceKind.Roaster), favorites.map { it.kind }.toSet())
            val collection = repository.create("Sandbox", favorites).getOrThrow()
            assertEquals(favorites.map { it.key }, collection.places.map { it.key })
        } finally {
            app.close()
            close(client, backend)
        }
    }

    @Test fun productionBindingStillRejectsMissingBearerCredentials() = runTest {
        val backend = SandboxBackend(byteArrayOf(1))
        val client = client(backend)
        val app = koinApplication { modules(seed(client), collectionsModule(SandboxBackend.BASE_URL)) }
        try {
            assertFailsWith<CancellationException> { app.koin.get<CollectionsRepository>().favorites() }
        } finally {
            app.close()
            close(client, backend)
        }
    }

    private fun client(backend: SandboxBackend) = CoffeePeekClient(SandboxBackend.BASE_URL, temporary.newFolder().path, false,
        getToken = { null }, saveToken = {}, apiClientFactory = backend::apiClient, uploadClientFactory = backend::uploadClient)

    private fun seed(client: CoffeePeekClient) = module {
        single { client }
        single<SessionReader> { Sessions() }
    }

    private fun close(client: CoffeePeekClient, backend: SandboxBackend) {
        client.client.close()
        client.plainClient.close()
        client.imageClient.close()
        client.uploadClient.close()
        backend.close()
    }

    private class Sessions : SessionReader {
        private val current = MutableStateFlow<Session?>(Session("sandbox-only", userId = "sandbox"))
        override fun peekSession() = current.value
        override fun isActiveSession(session: Session?) = session != null && session == current.value
        override fun observeSession() = current
        override suspend fun getSession() = current.value
    }
}
