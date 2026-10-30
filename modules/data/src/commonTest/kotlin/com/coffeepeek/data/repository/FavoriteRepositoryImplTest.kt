package com.coffeepeek.data.repository

import com.coffeepeek.api.service.ShopApiService
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionStore
import com.coffeepeek.data.util.FileUrlResolver
import com.coffeepeek.domain.feature.favorites.api.FavoriteEntry
import com.coffeepeek.domain.feature.favorites.api.FavoriteKind
import com.coffeepeek.domain.feature.favorites.api.FavoritesRepository
import com.coffeepeek.domain.model.PublicAddress
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FavoriteRepositoryImplTest {
    @Test fun shopAdapterRetainsServerNamesAndPhotosWhenDetailsOrPublicAddressAreUnavailable() = runBlocking {
        val rows = listOf(
            FavoriteEntry(FavoriteKind.CoffeeShop, PublicAddress("coffee", "/coffee-shops/coffee", 1, false),
                name = "Кофейня", coverPhotoUrl = "https://media.example/coffee.jpg", createdAtUtc = "2026-10-15T10:00:00Z"),
            FavoriteEntry(FavoriteKind.CoffeeShop, name = "Архивная кофейня", createdAtUtc = "2026-10-14T10:00:00Z", occurrence = 1),
        )
        val favorites = object : FavoritesRepository {
            override fun observeFavorites(): Flow<Result<List<FavoriteEntry>>> = flowOf(Result.success(rows))
            override suspend fun getFavorites(refresh: Boolean) = Result.success(rows)
            override suspend fun setFavorite(kind: FavoriteKind, address: PublicAddress, favorite: Boolean) = Result.success(Unit)
            override fun clearCache() = Unit
        }
        val client = HttpClient(MockEngine {
            respond("""{"isSuccess":false,"data":null}""", HttpStatusCode.NotFound, headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json() } }
        try {
            val adapter = FavoriteRepositoryImpl(favorites, ShopApiService(client), FileUrlResolver("https://api.example"), AliceSessions)
            val shops = adapter.getFavorites().getOrThrow()
            assertEquals(listOf("Кофейня", "Архивная кофейня"), shops.map { it.shop.title })
            assertEquals("https://media.example/coffee.jpg", shops.first().shop.photoUrl)
            assertEquals("coffee", shops.first().shop.publicAddress?.slug)
            assertNull(shops.last().shop.publicAddress)
        } finally { client.close() }
    }
}

private object AliceSessions : SessionStore {
    private val state = MutableStateFlow<Session?>(Session("alice-token", userId = "alice"))
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
