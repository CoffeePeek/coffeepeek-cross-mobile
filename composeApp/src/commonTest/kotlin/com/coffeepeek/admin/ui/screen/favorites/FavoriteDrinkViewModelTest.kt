package com.coffeepeek.admin.ui.screen.favorites

import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.domain.feature.favorites.api.FavoriteEntry
import com.coffeepeek.domain.feature.favorites.api.FavoriteKind
import com.coffeepeek.domain.feature.favorites.api.FavoritesRepository
import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.domain.repository.RoasterRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FavoriteDrinkViewModelTest {
    @Test fun namedDrinkRemainsVisibleOnFailureAndRemovalRetriesWithoutDuplicateRequests(): Unit = runBlocking {
        val drink = FavoriteEntry(FavoriteKind.Drink, drinkSlug = "matcha_latte", name = "Матча латте", coverPhotoUrl = "https://media.example/matcha.jpg")
        val entries = MutableStateFlow(Result.success(listOf(drink)))
        val requests = Channel<Pair<FavoriteEntry, Boolean>>(Channel.UNLIMITED)
        var release = CompletableDeferred<Result<Unit>>()
        val server = object : FavoritesRepository {
            override fun observeFavorites() = entries
            override suspend fun getFavorites(refresh: Boolean) = entries.value
            override suspend fun setFavorite(kind: FavoriteKind, address: PublicAddress, favorite: Boolean): Result<Unit> = error("Drink must use its own slug")
            override suspend fun setFavorite(entry: FavoriteEntry, favorite: Boolean): Result<Unit> {
                requests.send(entry to favorite)
                return release.await()
            }
            override fun clearCache() = Unit
        }
        val shops = object : FavoriteRepository {
            override suspend fun getFavoriteIds() = emptySet<String>()
            override suspend fun isFavorite(shopId: String) = false
            override suspend fun getFavorites() = Result.success(emptyList<CoffeeShopDetails>())
            override suspend fun addFavorite(shop: CoffeeShop, address: String?) = Result.success(Unit)
            override suspend fun removeFavorite(shopId: String) = Result.success(Unit)
            override suspend fun clearAll() = Unit
        }
        val roasterFavorites = object : RoasterFavorites {
            override fun observeFavorites() = flowOf(emptyList<CatalogItem>())
            override suspend fun setFavorite(roaster: CatalogItem, isFavorite: Boolean): Result<Unit> = error("Unused")
        }
        val roasters = object : RoasterRepository {
            override suspend fun getRoasters(): Result<List<RoasterSummary>> = error("Unused")
            override suspend fun getRoaster(id: String): Result<RoasterDetails> = error("Unused")
            override suspend fun submitRoaster(input: CreateRoasterInput): Result<RoasterSubmissionResult> = error("Unused")
            override suspend fun getMyRoasterSubmissions(status: ModerationStatus, page: Int, pageSize: Int): Result<PagedResult<RoasterSubmission>> = error("Unused")
        }
        val vm = FavoritesViewModel(shops, roasterFavorites, roasters, server)
        try {
            assertEquals(listOf(drink), withTimeout(5_000) { vm.state.first { it.drinks.isNotEmpty() } }.drinks)
            vm.removeDrink(drink)
            vm.removeDrink(drink)
            assertEquals(drink to false, withTimeout(5_000) { requests.receive() })
            assertTrue(requests.tryReceive().isFailure)
            assertTrue(drink.id in vm.state.value.removingDrinkIds)
            release.complete(Result.failure(IllegalStateException("Offline")))
            val failed = withTimeout(5_000) { vm.state.first { it.actionMessage != null && it.removingDrinkIds.isEmpty() } }
            assertEquals(listOf(drink), failed.drinks)
            release = CompletableDeferred()
            vm.removeDrink(drink)
            assertEquals(drink to false, withTimeout(5_000) { requests.receive() })
            release.complete(Result.success(Unit))
            withTimeout(5_000) { vm.state.first { it.drinks.isEmpty() && it.removingDrinkIds.isEmpty() } }
        } finally {
            release.complete(Result.success(Unit))
            requests.close()
            vm.close()
        }
    }
}
