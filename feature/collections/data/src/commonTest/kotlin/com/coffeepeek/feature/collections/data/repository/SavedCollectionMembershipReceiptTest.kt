package com.coffeepeek.feature.collections.data.repository

import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.domain.repository.CollectionsAccount
import com.coffeepeek.feature.collections.domain.repository.SavedCollectionsRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.Json
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SavedCollectionMembershipReceiptTest {
    private val dispatcher = StandardTestDispatcher()
    private val id = "12345678-1234-1234-1234-1234567890ab"
    private val target = SavedCollectionTarget(CollectionPlaceKind.CoffeeShop, "place")
    private val account = object : CollectionsAccount {
        override val identities = MutableStateFlow<String?>("owner")
        override fun currentId() = identities.value
        override suspend fun loadId() = identities.value
    }

    @Test fun favoriteHeaderRemainsTrueWhenTheResponseHasNoMemberships() = runTest(dispatcher) {
        val client = client("true", "true")
        try {
            val repository = repository(client)
            val changes = observe(repository)
            val result = repository.changeMemberships(target, setOf(id), emptySet()).getOrThrow()
            assertEquals(SavedCollectionMembershipResult(emptyList(), true, true), result)
            runCurrent()
            assertEquals(1, changes.size)
        } finally { client.close() }
    }

    @Test fun falseFavoriteHeaderIsPreservedAsConfirmedMetadata() = runTest(dispatcher) {
        val client = client("false", "true", library(false))
        try {
            val result = repository(client).changeMemberships(target, emptySet(), setOf(id)).getOrThrow()
            assertEquals(false, result.isFavorite)
            assertEquals(true, result.membershipChanged)
            assertFalse(result.lists.single().containsItem)
        } finally { client.close() }
    }

    @Test fun missingHeadersLeaveMetadataUnknownAndStillPublishChanges() = runTest(dispatcher) {
        assertUnknownMetadata(null, null)
    }

    @Test fun differentlyCasedBooleanHeadersRemainUnknown() = runTest(dispatcher) {
        assertUnknownMetadata("TRUE", "False")
    }

    @Test fun numericBooleanHeadersRemainUnknown() = runTest(dispatcher) {
        assertUnknownMetadata("1", "0")
    }

    @Test fun paddedBooleanHeadersRemainUnknown() = runTest(dispatcher) {
        assertUnknownMetadata(" true ", "false ")
    }

    @Test fun confirmedNoopUpdatesCachedListsWithoutPublishingAMutation() = runTest(dispatcher) {
        var reads = 0
        var current = library(true)
        val client = client("true", "false", library(false)) { reads++; current }
        try {
            val repository = repository(client)
            repository.mine(target).getOrThrow()
            current = library(false)
            val changes = observe(repository)
            val result = repository.changeMemberships(target, emptySet(), setOf(id)).getOrThrow()
            assertEquals(false, result.membershipChanged)
            assertEquals(true, result.isFavorite)
            assertFalse(result.lists.single().containsItem)
            assertEquals(result.lists, repository.cachedFreshMine(target))
            assertEquals(result.lists, repository.mine(target).getOrThrow())
            runCurrent()
            assertTrue(changes.isEmpty())
            assertEquals(2, reads)
        } finally { client.close() }
    }

    @Test fun absoluteMembershipNoopAlsoDoesNotPublishAMutation() = runTest(dispatcher) {
        val client = client("false", "false", library(false))
        try {
            val repository = repository(client)
            val changes = observe(repository)
            val lists = repository.memberships(target, emptySet()).getOrThrow()
            assertFalse(lists.single().containsItem)
            assertEquals(lists, repository.cachedMine(target))
            runCurrent()
            assertTrue(changes.isEmpty())
        } finally { client.close() }
    }

    private suspend fun TestScope.assertUnknownMetadata(favorite: String?, changed: String?) {
        val client = client(favorite, changed, library(false))
        try {
            val repository = repository(client)
            val changes = observe(repository)
            val result = repository.changeMemberships(target, emptySet(), setOf(id)).getOrThrow()
            assertNull(result.isFavorite)
            assertNull(result.membershipChanged)
            assertFalse(result.lists.single().containsItem)
            runCurrent()
            assertEquals(1, changes.size)
        } finally { client.close() }
    }

    private fun TestScope.observe(repository: SavedCollectionsRepository): List<Unit> {
        val changes = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.observeChanges().collect { changes += Unit } }
        return changes
    }

    private fun library(containsItem: Boolean) =
        """{"data":[{"id":"$id","title":"Places","revision":2,"isDefault":true,"containsItem":$containsItem}]}"""

    private fun repository(client: HttpClient) =
        HttpSavedCollectionsRepository(client, client, "https://fixture.invalid", account, { _, _ -> })

    private fun client(favorite: String?, changed: String?, writeBody: String = """{"data":[]}""",
        readBody: () -> String = { library(false) }) = HttpClient(MockEngine) {
        engine {
            dispatcher = this@SavedCollectionMembershipReceiptTest.dispatcher
            addHandler { request ->
                if (request.method == HttpMethod.Get) {
                    assertEquals("/api/v1/saved-collections", request.url.encodedPath)
                    respond(readBody(), headers = responseHeaders())
                } else {
                    assertEquals(HttpMethod.Put, request.method)
                    assertEquals("/api/v1/saved-collections/membership/coffee_shop/place", request.url.encodedPath)
                    respond(writeBody, headers = responseHeaders(favorite, changed))
                }
            }
        }
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private fun responseHeaders(favorite: String? = null, changed: String? = null) = Headers.build {
        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        favorite?.let { append("X-CoffeePeek-Is-Favorite", it) }
        changed?.let { append("X-CoffeePeek-Membership-Changed", it) }
    }
}
