package com.coffeepeek.admin.sandbox.backend

import com.coffeepeek.feature.collections.data.repository.createCollectionsRepository
import com.coffeepeek.feature.collections.domain.model.CollectionPlaceKind
import com.coffeepeek.feature.collections.domain.repository.CollectionsAccount
import io.ktor.client.request.delete
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import kotlin.test.*

class SandboxCollectionsContractTest {
    @Test fun actualClientCanCreateReadAndRevokeMixedCollectionWithoutLiveTransport() = runTest {
        val backend = SandboxBackend(byteArrayOf(1), LocalDate.of(2026, 11, 2))
        val client = backend.gateway
        val account = object : CollectionsAccount {
            override val identities = MutableStateFlow<String?>("sandbox")
            override fun currentId() = "sandbox"
            override suspend fun loadId() = currentId()
        }
        try {
            val repository = createCollectionsRepository(client, client, SandboxBackend.BASE_URL, account)
            val favorites = repository.favorites().getOrThrow()
            assertEquals(setOf(CollectionPlaceKind.CoffeeShop, CollectionPlaceKind.Roaster), favorites.map { it.kind }.toSet())
            val created = repository.create("Кофейный Sandbox", favorites).getOrThrow()
            client.delete("${SandboxBackend.BASE_URL}/api/v1/favorites/coffee_shop/sandbox-cafe")
            assertEquals(favorites.map { it.key }, repository.read(created.id).getOrThrow().places.map { it.key })
            assertEquals(created.id, repository.mine().getOrThrow().single().id)
            assertTrue(repository.revoke(created.id).isSuccess)
            assertTrue(repository.read(created.id).isFailure)
        } finally { backend.close() }
    }
}
