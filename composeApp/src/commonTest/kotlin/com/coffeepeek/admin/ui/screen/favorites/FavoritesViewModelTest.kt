package com.coffeepeek.admin.ui.screen.favorites

import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.domain.repository.RoasterRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class FavoritesViewModelTest {
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
