package com.coffeepeek.data.feature.favorites

import com.coffeepeek.api.feature.favorites.FavoriteEntryDto
import com.coffeepeek.api.feature.favorites.FavoriteDrinkDto
import com.coffeepeek.api.feature.favorites.FavoritesApiService
import com.coffeepeek.api.model.ApiResponse
import com.coffeepeek.api.model.PublicAddressDto
import com.coffeepeek.api.model.response.shop.ShortPhotoMetadataDto
import com.coffeepeek.api.model.response.shop.PhotoUrlsDto
import com.coffeepeek.api.service.ShopApiService
import com.coffeepeek.data.repository.FavoriteRepositoryImpl
import com.coffeepeek.data.util.FileUrlResolver
import com.coffeepeek.domain.feature.favorites.api.FavoriteKind
import com.coffeepeek.domain.model.PublicAddress
import com.coffeepeek.domain.model.Session
import com.coffeepeek.core.session.SessionStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertNull

class ServerFavoritesRepositoryTest {
    @Test fun observersClearPreviousAccountsListBeforeLoadingTheNextAccountsFavorites() = runBlocking {
        val server = FavoriteServer()
        val updates = Channel<List<String>>(Channel.UNLIMITED)
        try {
            val repository = server.repository()
            repository.setFavorite(FavoriteKind.Roaster, address("alice-roast"), true).getOrThrow()
            val observer = launch(start = CoroutineStart.UNDISPATCHED) {
                repository.observeFavorites().collect { result -> updates.send(result.getOrThrow().mapNotNull { it.slug }) }
            }
            try {
                assertTrue(withTimeout(2_000) { updates.receive() }.isEmpty())
                assertEquals(listOf("alice-roast"), withTimeout(2_000) { updates.receive() })
                server.sessions.signIn("bob")
                assertTrue(withTimeout(2_000) { updates.receive() }.isEmpty())
                repository.setFavorite(FavoriteKind.Roaster, address("bob-roast"), true).getOrThrow()
                assertEquals(listOf("bob-roast"), withTimeout(2_000) { updates.receive() })
                server.sessions.signIn(null)
                assertTrue(withTimeout(2_000) { updates.receive() }.isEmpty())
            } finally { observer.cancelAndJoin() }
        } finally {
            updates.close()
            server.client.close()
        }
    }

    @Test fun persistsBothKindsOnTheServerAndRestoresAfterRecreationAndCacheClearing() = runBlocking {
        val server = FavoriteServer()
        try {
            val repository = server.repository()
            repository.setFavorite(FavoriteKind.CoffeeShop, address("shop"), true).getOrThrow()
            repository.setFavorite(FavoriteKind.Roaster, address("roast"), true).getOrThrow()
            assertEquals(setOf("shop", "roast"), server.repository().getFavorites().getOrThrow().map { it.slug }.toSet())
            repository.clearCache()
            assertEquals(2, repository.getFavorites().getOrThrow().size)
            assertTrue(server.methods.none { it == HttpMethod.Delete })
            repository.setFavorite(FavoriteKind.Roaster, address("roast"), false).getOrThrow()
            assertEquals(listOf("shop"), server.repository().getFavorites().getOrThrow().map { it.slug })
        } finally { server.client.close() }
    }

    @Test fun failedMutationsPreserveConfirmedStateAndCanBeRetried() = runBlocking {
        val server = FavoriteServer()
        try {
            val repository = server.repository()
            repository.setFavorite(FavoriteKind.Roaster, address("roast"), true).getOrThrow()
            server.failMutations = true
            assertTrue(repository.setFavorite(FavoriteKind.Roaster, address("roast"), false).isFailure)
            assertEquals(listOf("roast"), repository.getFavorites().getOrThrow().map { it.slug })
            server.failMutations = false
            repository.setFavorite(FavoriteKind.Roaster, address("roast"), false).getOrThrow()
            assertTrue(repository.getFavorites().getOrThrow().isEmpty())
        } finally { server.client.close() }
    }

    @Test fun concurrentEditsDoNotLoseOtherItemsAndKindsDoNotCollide() = runBlocking {
        val server = FavoriteServer()
        try {
            val repository = server.repository()
            (0 until 12).map { index -> async {
                repository.setFavorite(FavoriteKind.Roaster, address("roast-$index"), true).getOrThrow()
            } }.awaitAll()
            repository.setFavorite(FavoriteKind.CoffeeShop, address("roast-0"), true).getOrThrow()
            repository.setFavorite(FavoriteKind.Roaster, address("roast-0"), false).getOrThrow()
            val saved = server.repository().getFavorites().getOrThrow()
            assertEquals(12, saved.size)
            assertEquals(FavoriteKind.CoffeeShop, saved.single { it.slug == "roast-0" }.kind)
        } finally { server.client.close() }
    }

    @Test fun guestsMakeNoRequestsAndSwitchingAccountsNeverReturnsAnotherAccountsCache() = runBlocking {
        val server = FavoriteServer()
        try {
            val repository = server.repository()
            repository.setFavorite(FavoriteKind.Roaster, address("alice-roast"), true).getOrThrow()
            server.sessions.signIn("bob")
            assertTrue(repository.getFavorites().getOrThrow().isEmpty())
            repository.setFavorite(FavoriteKind.Roaster, address("bob-roast"), true).getOrThrow()
            server.sessions.signIn(null)
            val count = server.methods.size
            assertTrue(repository.getFavorites().getOrThrow().isEmpty())
            assertTrue(repository.setFavorite(FavoriteKind.Roaster, address("guest"), true).isFailure)
            assertEquals(count, server.methods.size)
            server.sessions.signIn("alice")
            assertEquals(listOf("alice-roast"), repository.getFavorites().getOrThrow().map { it.slug })
        } finally { server.client.close() }
    }

    @Test fun lateMutationResponseFromOldAccountIsCancelledAndCannotUpdateTheNewAccount() = runBlocking {
        val server = FavoriteServer()
        try {
            val repository = server.repository()
            repository.getFavorites().getOrThrow()
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            server.beforeMutation = { started.complete(Unit); release.await() }
            val mutation = async { repository.setFavorite(FavoriteKind.Roaster, address("alice-roast"), true) }
            started.await()
            server.sessions.signIn("bob")
            release.complete(Unit)
            assertFailsWith<CancellationException> { mutation.await() }
            assertTrue(repository.getFavorites().getOrThrow().isEmpty())
            server.sessions.signIn("alice")
            assertEquals(listOf("alice-roast"), repository.getFavorites().getOrThrow().map { it.slug })
        } finally { server.client.close() }
    }

    private fun address(slug: String) = PublicAddress(slug, "/roasters/$slug", 1, false)

    @Test fun restoresDrinksAndUnaddressedObjectsWithoutKindCollisionsOrFakeAddresses() = runBlocking {
        val server = FavoriteServer()
        try {
            val photo = ShortPhotoMetadataDto(fullUrl = "https://media.example/original.jpg", urls = PhotoUrlsDto(card = "https://media.example/card.jpg"))
            server.seed(listOf(
                FavoriteEntryDto("coffee_shop", PublicAddressDto("matcha_latte", "/coffee-shops/matcha_latte", 1, false), "2026-10-15T10:00:00Z", "Кофейня"),
                FavoriteEntryDto("roaster", PublicAddressDto("matcha_latte", "/roasters/matcha_latte", 1, false), "2026-10-15T10:00:00Z", "Обжарщик"),
                FavoriteEntryDto("drink", null, "2026-10-15T10:00:00Z", "Матча латте", photo, FavoriteDrinkDto("matcha_latte")),
                FavoriteEntryDto("coffee_shop", null, "2026-10-14T10:00:00Z", "Архивная кофейня"),
                FavoriteEntryDto("coffee_shop", null, "2026-10-14T10:00:00Z", "Архивная кофейня"),
            ))
            val repository = server.repository()
            val saved = repository.getFavorites().getOrThrow()
            assertEquals(5, saved.size)
            assertEquals(5, saved.map { it.id }.toSet().size)
            val drink = saved.single { it.kind == FavoriteKind.Drink }
            assertNull(drink.address)
            assertEquals("matcha_latte", drink.drinkSlug)
            assertEquals("Матча латте", drink.name)
            assertEquals("https://media.example/card.jpg", drink.coverPhotoUrl)
            server.sessions.signIn("bob")
            assertTrue(repository.getFavorites().getOrThrow().isEmpty())
            server.sessions.signIn("alice")
            assertEquals(saved, repository.getFavorites().getOrThrow())
            repository.setFavorite(drink, false).getOrThrow()
            val restored = server.repository().getFavorites().getOrThrow()
            assertEquals(4, restored.size)
            assertTrue(restored.none { it.kind == FavoriteKind.Drink })
            assertEquals(2, restored.count { it.slug == "matcha_latte" })
            repository.setFavorite(drink, true).getOrThrow()
            val added = server.repository().getFavorites().getOrThrow().single { it.kind == FavoriteKind.Drink }
            assertEquals("matcha_latte", added.drinkSlug)
            assertNull(added.address)
        } finally { server.client.close() }
    }

    @Test fun shopAdapterRetainsServerNamesAndPhotosWhenDetailsOrPublicAddressAreUnavailable() = runBlocking {
        val server = FavoriteServer()
        try {
            server.seed(listOf(
                FavoriteEntryDto("coffee_shop", PublicAddressDto("coffee", "/coffee-shops/coffee", 1, false), "2026-10-15T10:00:00Z", "Кофейня", ShortPhotoMetadataDto(fullUrl = "https://media.example/coffee.jpg")),
                FavoriteEntryDto("coffee_shop", null, "2026-10-14T10:00:00Z", "Архивная кофейня"),
            ))
            val adapter = FavoriteRepositoryImpl(server.repository(), ShopApiService(server.client), FileUrlResolver("https://api.example"), server.sessions)
            val shops = adapter.getFavorites().getOrThrow()
            assertEquals(listOf("Кофейня", "Архивная кофейня"), shops.map { it.shop.title })
            assertEquals("https://media.example/coffee.jpg", shops.first().shop.photoUrl)
            assertEquals("coffee", shops.first().shop.publicAddress?.slug)
            assertNull(shops.last().shop.publicAddress)
        } finally { server.client.close() }
    }
}

private class FavoriteServer {
    val sessions = FavoriteSessions()
    val methods = mutableListOf<HttpMethod>()
    var failMutations = false
    var beforeMutation: (suspend () -> Unit)? = null
    private val accounts = mutableMapOf<String, List<FavoriteEntryDto>>()
    fun seed(items: List<FavoriteEntryDto>) { accounts[requireNotNull(sessions.peekSession()?.userId)] = items }
    val client = HttpClient(MockEngine { request ->
        methods += request.method
        val account = requireNotNull(sessions.peekSession()?.userId)
        val current = accounts[account].orEmpty()
        if (request.url.encodedPath.startsWith("/api/CoffeeShops/")) {
            respond("""{"isSuccess":false,"data":null}""", HttpStatusCode.NotFound, headersOf(HttpHeaders.ContentType, "application/json"))
        } else if (request.method == HttpMethod.Get) {
            respond(Json.encodeToString(ApiResponse(isSuccess = true, data = current)), headers = headersOf(HttpHeaders.ContentType, "application/json"))
        } else if (failMutations) {
            respond("", HttpStatusCode.ServiceUnavailable)
        } else {
            beforeMutation?.invoke()
            val slug = request.url.encodedPath.substringAfterLast('/')
            val kind = request.url.encodedPath.substringBeforeLast('/').substringAfterLast('/')
            val remaining = current.filterNot { it.kind == kind && (it.address?.slug ?: it.drink?.slug) == slug }
            accounts[account] = if (request.method == HttpMethod.Put) {
                remaining + FavoriteEntryDto(kind,
                    if (kind == "drink") null else PublicAddressDto(slug, "/$kind/$slug", 1, false),
                    "2026-10-15T10:00:00Z", name = slug,
                    drink = if (kind == "drink") FavoriteDrinkDto(slug) else null)
            } else remaining
            respond("", HttpStatusCode.NoContent)
        }
    }) { install(ContentNegotiation) { json() } }
    fun repository() = ServerFavoritesRepository(FavoritesApiService(client), sessions)
}

private class FavoriteSessions : SessionStore {
    private val state = MutableStateFlow<Session?>(Session("alice-token", userId = "alice"))
    fun signIn(userId: String?) { state.value = userId?.let { Session("$it-token", userId = it) } }
    override fun peekSession() = state.value
    override fun applySession(session: Session?) { state.value = session }
    override fun isActiveSession(session: Session?) = session != null
    override suspend fun getSession() = state.value
    override suspend fun persistSession(session: Session?) { state.value = session }
    override suspend fun saveSession(session: Session?) { state.value = session }
    override suspend fun warmCache() = Unit
    override fun observeSession() = state
    override suspend fun isLoggedIn() = state.value != null
}
