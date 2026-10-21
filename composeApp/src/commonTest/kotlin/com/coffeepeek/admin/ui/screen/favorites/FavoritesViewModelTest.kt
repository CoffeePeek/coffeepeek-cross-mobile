package com.coffeepeek.admin.ui.screen.favorites

import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.domain.repository.RoasterRepository
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FavoritesViewModelTest {
    @Test
    fun accountChangesClearFavoritesAndIgnoreLateRequestsFromPreviousAccount() = runBlocking {
        val sessions = FavoriteSessions("alice")
        val requests = Channel<String>(Channel.UNLIMITED)
        val lateResponse = CompletableDeferred<Unit>()
        var holdAlice = false
        fun shop(account: String) = CoffeeShopDetails(CoffeeShop(account, account, null, cityName = null, priceRange = null, photoUrl = null))
        val shops = object : FavoriteRepository {
            override suspend fun getFavoriteIds() = emptySet<String>()
            override suspend fun isFavorite(shopId: String) = false
            override suspend fun getFavorites(): Result<List<CoffeeShopDetails>> {
                val account = checkNotNull(sessions.peekSession()?.userId)
                requests.send(account)
                if (account == "alice" && holdAlice) withContext(NonCancellable) { lateResponse.await() }
                return Result.success(listOf(shop(account)))
            }
            override suspend fun addFavorite(shop: CoffeeShop, address: String?) = Result.success(Unit)
            override suspend fun removeFavorite(shopId: String) = Result.success(Unit)
            override suspend fun clearAll() = Unit
        }
        val saved = MutableStateFlow(listOf(CatalogItem("alice-roast", "Alice roast")))
        val favorites = object : RoasterFavorites {
            override fun observeFavorites() = saved
            override suspend fun setFavorite(roaster: CatalogItem, isFavorite: Boolean) = Result.success(Unit)
        }
        val roasters = object : RoasterRepository {
            override suspend fun getRoasters(): Result<List<RoasterSummary>> = error("Unused")
            override suspend fun getRoaster(id: String): Result<RoasterDetails> = error("Unused")
            override suspend fun submitRoaster(input: CreateRoasterInput): Result<RoasterSubmissionResult> = error("Unused")
            override suspend fun getMyRoasterSubmissions(status: ModerationStatus, page: Int, pageSize: Int): Result<PagedResult<RoasterSubmission>> = error("Unused")
        }
        val vm = FavoritesViewModel(shops, favorites, roasters, sessions = sessions)
        try {
            withTimeout(5_000) {
                assertEquals("alice", requests.receive())
                vm.state.first { it.shops.singleOrNull()?.shop?.id == "alice" && it.roasters.isNotEmpty() }
                holdAlice = true
                vm.load(force = true)
                assertEquals("alice", requests.receive())
                sessions.applySession(null)
                val guest = vm.state.first { it.shops.isEmpty() && it.roasters.isEmpty() && !it.areRoastersLoading }
                assertTrue(guest.drinks.isEmpty())
                assertTrue(guest.roasterDetails.isEmpty())
                assertFalse(guest.isLoading)
                vm.load(force = true)
                assertTrue(requests.tryReceive().isFailure)

                saved.value = emptyList()
                sessions.applySession(Session("bob-token", userId = "bob"))
                assertEquals("bob", requests.receive())
                vm.state.first { it.shops.singleOrNull()?.shop?.id == "bob" && !it.isLoading }
                lateResponse.complete(Unit)
                assertNull(withTimeoutOrNull(200) { vm.state.first { it.shops.any { shop -> shop.shop.id == "alice" } } })
                assertEquals(listOf("bob"), vm.state.value.shops.map { it.shop.id })
            }
        } finally {
            lateResponse.complete(Unit)
            vm.close()
        }
    }

    @Test
    fun displaysSavedRoastersWhileLoadingUsageByPublicSlugAndRemovesTheirDetails() = runBlocking {
        val roast = CatalogItem("catalog-guid", "Roast", address = PublicAddress("roast", "/roasters/roast", 1, false))
        val unavailable = CatalogItem("another-guid", "Unavailable", address = PublicAddress("unavailable", "/roasters/unavailable", 1, false))
        val legacy = CatalogItem("legacy-guid", "Legacy")
        val saved = MutableStateFlow(listOf(roast, unavailable, legacy))
        val favorites = object : RoasterFavorites {
            override fun observeFavorites() = saved
            override suspend fun setFavorite(roaster: CatalogItem, isFavorite: Boolean): Result<Unit> {
                saved.update { it.filterNot { item -> item.id == roaster.id } }
                return Result.success(Unit)
            }
        }
        val requests = Channel<String>(Channel.UNLIMITED)
        val response = CompletableDeferred<RoasterDetails>()
        val roasters = object : RoasterRepository {
            override suspend fun getRoasters(): Result<List<RoasterSummary>> = error("Unused")
            override suspend fun getRoaster(id: String): Result<RoasterDetails> {
                requests.send(id)
                return if (id == "roast") Result.success(response.await()) else Result.failure(IllegalStateException("Unavailable"))
            }
            override suspend fun submitRoaster(input: CreateRoasterInput): Result<RoasterSubmissionResult> = error("Unused")
            override suspend fun getMyRoasterSubmissions(status: ModerationStatus, page: Int, pageSize: Int): Result<PagedResult<RoasterSubmission>> = error("Unused")
        }
        val shops = object : FavoriteRepository {
            override suspend fun getFavoriteIds() = emptySet<String>()
            override suspend fun isFavorite(shopId: String) = false
            override suspend fun getFavorites() = Result.success(emptyList<CoffeeShopDetails>())
            override suspend fun addFavorite(shop: CoffeeShop, address: String?) = Result.success(Unit)
            override suspend fun removeFavorite(shopId: String) = Result.success(Unit)
            override suspend fun clearAll() = Unit
        }
        val vm = FavoritesViewModel(shops, favorites, roasters)
        try {
            withTimeout(5_000) {
                val initial = vm.state.first { !it.areRoastersLoading }
                assertEquals(saved.value, initial.roasters)
                assertEquals(emptyMap(), initial.roasterDetails)
                assertEquals(setOf("roast", "unavailable"), setOf(requests.receive(), requests.receive()))
                val details = RoasterDetails("roast", "Roast", shops = listOf(RoasterShop("shop", "Coffee")))
                response.complete(details)
                val loaded = vm.state.first { it.roasterDetails["roast"] != null }
                assertEquals(details, loaded.roasterDetails["roast"])
                assertEquals(listOf(roast, unavailable, legacy), loaded.roasters)
                vm.removeRoaster(roast)
                val removed = vm.state.first { roast !in it.roasters }
                assertFalse("roast" in removed.roasterDetails)
                assertEquals(listOf(unavailable, legacy), removed.roasters)
            }
        } finally {
            vm.close()
        }
    }
}

private class FavoriteSessions(user: String) : SessionRepository {
    private val current = MutableStateFlow<Session?>(Session("$user-token", userId = user))
    override fun peekSession() = current.value
    override fun applySession(session: Session?) { current.value = session }
    override fun isActiveSession(session: Session?) = session != null && session == current.value
    override suspend fun getSession() = current.value
    override suspend fun persistSession(session: Session?) { current.value = session }
    override suspend fun saveSession(session: Session?) { current.value = session }
    override suspend fun warmCache() = Unit
    override fun observeSession() = current
    override suspend fun isLoggedIn() = current.value != null
}
