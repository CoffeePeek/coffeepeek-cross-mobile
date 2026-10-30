package com.coffeepeek.feature.favorites.data.repository

import com.coffeepeek.core.network.ApiEnvelope
import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionStore
import com.coffeepeek.feature.favorites.data.backend.FavoriteAddressDto
import com.coffeepeek.feature.favorites.data.backend.FavoriteDrinkDto
import com.coffeepeek.feature.favorites.data.backend.FavoriteEntryDto
import com.coffeepeek.feature.favorites.data.backend.FavoritePhotoDto
import com.coffeepeek.feature.favorites.data.backend.FavoritePhotoUrlsDto
import com.coffeepeek.feature.favorites.domain.model.FavoriteAddress
import com.coffeepeek.feature.favorites.domain.model.FavoriteEntry
import com.coffeepeek.feature.favorites.domain.model.FavoriteKind
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountFavoritesTest {
    @Test fun observersClearPreviousAccountsListBeforeLoadingTheNextAccountsFavorites() = runBlocking {
        val server = FavoriteServer()
        val updates = Channel<List<String>>(Channel.UNLIMITED)
        try {
            val repository = server.repository()
            repository.setFavorite(roaster("alice-roast"), true).getOrThrow()
            val observer = launch(start = CoroutineStart.UNDISPATCHED) {
                repository.observeFavorites().collect { result -> updates.send(result.getOrThrow().mapNotNull { it.slug }) }
            }
            try {
                assertTrue(withTimeout(2_000) { updates.receive() }.isEmpty())
                assertEquals(listOf("alice-roast"), withTimeout(2_000) { updates.receive() })
                server.sessions.signIn("bob")
                assertTrue(withTimeout(2_000) { updates.receive() }.isEmpty())
                repository.setFavorite(roaster("bob-roast"), true).getOrThrow()
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
            repository.setFavorite(shop("shop"), true).getOrThrow()
            repository.setFavorite(roaster("roast"), true).getOrThrow()
            assertEquals(setOf("shop", "roast"), server.repository().getFavorites().getOrThrow().map { it.slug }.toSet())
            repository.clearCache()
            assertEquals(2, repository.getFavorites().getOrThrow().size)
            assertTrue(server.methods.none { it == HttpMethod.Delete })
            repository.setFavorite(roaster("roast"), false).getOrThrow()
            assertEquals(listOf("shop"), server.repository().getFavorites().getOrThrow().map { it.slug })
        } finally { server.client.close() }
    }

    @Test fun failedMutationsPreserveConfirmedStateAndCanBeRetried() = runBlocking {
        val server = FavoriteServer()
        try {
            val repository = server.repository()
            repository.setFavorite(roaster("roast"), true).getOrThrow()
            server.failMutations = true
            assertTrue(repository.setFavorite(roaster("roast"), false).isFailure)
            assertEquals(listOf("roast"), repository.getFavorites().getOrThrow().map { it.slug })
            server.failMutations = false
            repository.setFavorite(roaster("roast"), false).getOrThrow()
            assertTrue(repository.getFavorites().getOrThrow().isEmpty())
        } finally { server.client.close() }
    }

    @Test fun concurrentEditsDoNotLoseOtherItemsAndKindsDoNotCollide() = runBlocking {
        val server = FavoriteServer()
        try {
            val repository = server.repository()
            (0 until 12).map { index -> async {
                repository.setFavorite(roaster("roast-$index"), true).getOrThrow()
            } }.awaitAll()
            repository.setFavorite(shop("roast-0"), true).getOrThrow()
            repository.setFavorite(roaster("roast-0"), false).getOrThrow()
            val saved = server.repository().getFavorites().getOrThrow()
            assertEquals(12, saved.size)
            assertEquals(FavoriteKind.CoffeeShop, saved.single { it.slug == "roast-0" }.kind)
        } finally { server.client.close() }
    }

    @Test fun guestsMakeNoRequestsAndSwitchingAccountsNeverReturnsAnotherAccountsCache() = runBlocking {
        val server = FavoriteServer()
        try {
            val repository = server.repository()
            repository.setFavorite(roaster("alice-roast"), true).getOrThrow()
            server.sessions.signIn("bob")
            assertTrue(repository.getFavorites().getOrThrow().isEmpty())
            repository.setFavorite(roaster("bob-roast"), true).getOrThrow()
            server.sessions.signIn(null)
            val count = server.methods.size
            assertTrue(repository.getFavorites().getOrThrow().isEmpty())
            assertTrue(repository.setFavorite(roaster("guest"), true).isFailure)
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
            val mutation = async { repository.setFavorite(roaster("alice-roast"), true) }
            started.await()
            server.sessions.signIn("bob")
            release.complete(Unit)
            assertFailsWith<CancellationException> { mutation.await() }
            assertTrue(repository.getFavorites().getOrThrow().isEmpty())
            server.sessions.signIn("alice")
            assertEquals(listOf("alice-roast"), repository.getFavorites().getOrThrow().map { it.slug })
        } finally { server.client.close() }
    }

    @Test fun restoresDrinksAndUnaddressedObjectsWithoutKindCollisionsOrFakeAddresses() = runBlocking {
        val server = FavoriteServer()
        try {
            val photo = FavoritePhotoDto(fullUrl = "https://media.example/original.jpg", urls = FavoritePhotoUrlsDto(card = "https://media.example/card.jpg"))
            server.seed(listOf(
                FavoriteEntryDto("coffee_shop", FavoriteAddressDto("matcha_latte", "/coffee-shops/matcha_latte", 1, false), "2026-10-15T10:00:00Z", "Кофейня"),
                FavoriteEntryDto("roaster", FavoriteAddressDto("matcha_latte", "/roasters/matcha_latte", 1, false), "2026-10-15T10:00:00Z", "Обжарщик"),
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

    @Test fun theCoverFallsBackToTheFullUrlWhenTheCardVariantIsBlank() = runBlocking {
        val server = FavoriteServer()
        try {
            server.seed(listOf(
                FavoriteEntryDto("coffee_shop", FavoriteAddressDto("a", "/a", 1, false), "2026-10-15T10:00:00Z", "A",
                    FavoritePhotoDto(fullUrl = "https://media.example/a.jpg", urls = FavoritePhotoUrlsDto(card = " "))),
                FavoriteEntryDto("coffee_shop", FavoriteAddressDto("b", "/b", 1, false), "2026-10-15T10:00:00Z", "B", FavoritePhotoDto()),
            ))
            val saved = server.repository().getFavorites().getOrThrow()
            assertEquals("https://media.example/a.jpg", saved.single { it.slug == "a" }.coverPhotoUrl)
            assertNull(saved.single { it.slug == "b" }.coverPhotoUrl)
        } finally { server.client.close() }
    }

    @Test fun unknownKindsAreSkippedAndAnAddresslessMutationIsRejected() = runBlocking {
        val server = FavoriteServer()
        try {
            server.seed(listOf(
                FavoriteEntryDto("planet", null, "2026-10-15T10:00:00Z", "Unknown"),
                FavoriteEntryDto("roaster", FavoriteAddressDto("r", "/r", 1, false), "2026-10-15T10:00:00Z", "R"),
            ))
            val repository = server.repository()
            assertEquals(listOf("r"), repository.getFavorites().getOrThrow().map { it.slug })
            val before = server.methods.size
            assertTrue(repository.setFavorite(FavoriteEntry(FavoriteKind.Roaster, name = "No address"), true).isFailure)
            assertEquals(before, server.methods.size)
        } finally { server.client.close() }
    }

    private fun address(slug: String, kind: String) = FavoriteAddress(slug, "/$kind/$slug", 1, false)
    private fun roaster(slug: String) = FavoriteEntry(FavoriteKind.Roaster, address(slug, "roasters"))
    private fun shop(slug: String) = FavoriteEntry(FavoriteKind.CoffeeShop, address(slug, "coffee-shops"))
}

private class FavoriteServer {
    val sessions = FavoriteSessions()
    val methods = mutableListOf<HttpMethod>()
    var failMutations = false
    var beforeMutation: (suspend () -> Unit)? = null
    private val accounts = mutableMapOf<String, List<FavoriteEntryDto>>()
    fun seed(items: List<FavoriteEntryDto>) { accounts[requireNotNull(sessions.peekSession()?.userId)] = items }
    val client = HttpClientFactory(MockEngine { request ->
        methods += request.method
        val account = requireNotNull(sessions.peekSession()?.userId)
        val current = accounts[account].orEmpty()
        if (request.method == HttpMethod.Get) {
            respond(Json.encodeToString(ApiEnvelope(isSuccess = true, data = current)), headers = headersOf(HttpHeaders.ContentType, "application/json"))
        } else if (failMutations) {
            respond("", HttpStatusCode.ServiceUnavailable)
        } else {
            beforeMutation?.invoke()
            val slug = request.url.encodedPath.substringAfterLast('/')
            val kind = request.url.encodedPath.substringBeforeLast('/').substringAfterLast('/')
            val remaining = current.filterNot { it.kind == kind && (it.address?.slug ?: it.drink?.slug) == slug }
            accounts[account] = if (request.method == HttpMethod.Put) {
                remaining + FavoriteEntryDto(kind,
                    if (kind == "drink") null else FavoriteAddressDto(slug, "/$kind/$slug", 1, false),
                    "2026-10-15T10:00:00Z", name = slug,
                    drink = if (kind == "drink") FavoriteDrinkDto(slug) else null)
            } else remaining
            respond("", HttpStatusCode.NoContent)
        }
    }).api("https://api.example")
    fun repository() = createAccountFavorites(client, sessions)
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
