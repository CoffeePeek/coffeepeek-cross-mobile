package com.coffeepeek.admin.ui.favorites

import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.admin.ui.screen.feed.FeedViewModel
import com.coffeepeek.admin.ui.screen.shop.CheckInDraftStore
import com.coffeepeek.admin.ui.screen.shop.ShopDetailViewModel
import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.*
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.feature.favorites.domain.usecase.ObserveFavoriteIdsUseCase
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavoriteMembershipObservationTest {
    private val shop = CoffeeShop("shop", "Shop", null, cityName = null,
        priceRange = null, photoUrl = null)

    private class Membership : FavoritesRepository {
        val values = MutableStateFlow<Result<List<FavoriteShop>>>(Result.success(emptyList()))
        override fun observe(): Flow<Result<List<FavoriteShop>>> = values
        override suspend fun read(): Result<List<FavoriteShop>> = error("unused")
        override suspend fun save(shop: FavoriteShop): Result<Unit> = error("unused")
        override suspend fun remove(shopId: String): Result<Unit> = error("unused")
        override suspend fun clear(): Result<Unit> = error("unused")

        fun set(vararg ids: String) {
            values.value = Result.success(ids.map { FavoriteShop(it, it) })
        }
    }

    private class Shops : ShopRepository {
        val search = CompletableDeferred<PagedResult<CoffeeShop>>()
        val details = CompletableDeferred<CoffeeShopDetails>()

        override suspend fun getCatalogs() = Result.success(ShopCatalogs())
        override suspend fun getConsumedDrinks(): Result<List<ConsumedDrinkOption>> = error("unused")
        override suspend fun searchShops(filters: ShopFilters) = Result.success(search.await())
        override suspend fun getShopDetails(id: String) = Result.success(details.await())
        override suspend fun getMapContent(bounds: MapBounds, zoom: Float, filters: ShopFilters): Result<MapContent> = error("unused")
        override suspend fun getMenuDrinks(): Result<List<CoffeeDrinkDefinition>> = error("unused")
        override suspend fun createShop(input: CreateShopInput): Result<Unit> = error("unused")
        override suspend fun getMyShopSubmissions(status: ModerationStatus, page: Int, pageSize: Int): Result<PagedResult<ShopSubmission>> = error("unused")
    }

    private object LegacyFavorites : FavoriteRepository {
        override suspend fun getFavoriteIds() = emptySet<String>()
        override suspend fun isFavorite(shopId: String) = false
        override suspend fun getFavorites() = Result.success(emptyList<CoffeeShopDetails>())
        override suspend fun addFavorite(shop: CoffeeShop, address: String?) = Result.success(Unit)
        override suspend fun removeFavorite(shopId: String) = Result.success(Unit)
        override suspend fun clearAll() = Unit
    }

    private object Sessions : SessionRepository {
        override fun peekSession(): Session? = null
        override fun applySession(session: Session?) = Unit
        override fun isActiveSession(session: Session?) = false
        override suspend fun getSession(): Session? = null
        override suspend fun persistSession(session: Session?) = Unit
        override suspend fun saveSession(session: Session?) = Unit
        override suspend fun warmCache() = Unit
        override fun observeSession(): Flow<Session?> = emptyFlow()
        override suspend fun isLoggedIn() = false
    }

    private object Settings : SettingRepository {
        override suspend fun save(model: Setting) = Unit
        override suspend fun read(key: String): Setting? = null
        override fun readFlow(key: String): Flow<Setting?> = emptyFlow()
        override suspend fun readAll() = emptyList<Setting>()
        override fun readAllFlow(): Flow<List<Setting>> = emptyFlow()
        override suspend fun delete(key: String) = Unit
    }

    private object CheckIns : CheckInRepository {
        override suspend fun createCheckIn(input: CreateCheckInInput): Result<Unit> = error("unused")
        override suspend fun getMyCheckIns(page: Int, pageSize: Int): Result<PagedResult<CheckIn>> = error("unused")
        override suspend fun getMyCheckIns(from: String, to: String, pageSize: Int): Result<List<CheckIn>> = error("unused")
    }

    private object Reviews : ReviewRepository {
        override suspend fun submitReviewReport(reviewId: String, text: String): Result<String> = error("unused")
        override suspend fun createReview(input: CreateReviewInput): Result<Unit> = error("unused")
        override suspend fun updateReview(reviewId: String, input: UpdateReviewInput): Result<Unit> = error("unused")
        override suspend fun getUserReviews(userId: String, page: Int, pageSize: Int): Result<PagedResult<Review>> = error("unused")
        override suspend fun getMyReviewSubmissions(status: ModerationStatus, page: Int, pageSize: Int): Result<PagedResult<ReviewSubmission>> = error("unused")
        override suspend fun setReviewHelpful(reviewId: String, helpful: Boolean): Result<HelpfulVote> = error("unused")
    }

    private object Users : UserRepository {
        override fun observeProfile() = MutableStateFlow<UserProfile?>(null)
        override suspend fun refreshProfile(): Result<UserProfile> = error("unused")
        override suspend fun getMe(): Result<UserProfile> = error("unused")
        override suspend fun getPublicAvatarUrl(userId: String): Result<String?> = error("unused")
        override suspend fun requestAccountDeletion(): Result<AccountDeletionRequest> = error("unused")
        override suspend fun getAccountDeletionRequest(): Result<AccountDeletionRequest?> = error("unused")
        override suspend fun updateUsername(username: String): Result<Unit> = error("unused")
        override suspend fun updateAbout(about: String): Result<Unit> = error("unused")
        override suspend fun updateAvatar(photo: PendingPhotoUpload): Result<Unit> = error("unused")
    }

    @Test fun feedAppliesMembershipEvenWhenCatalogResponseArrivesLater() = runBlocking {
        val membership = Membership()
        val shops = Shops()
        val vm = FeedViewModel(shops, LegacyFavorites, CityPreference(Settings), Sessions,
            ObserveFavoriteIdsUseCase(membership))
        try {
            membership.set("shop")
            shops.search.complete(PagedResult(listOf(shop), 1, 1, 1))
            withTimeout(5_000) { vm.uiState.first { !it.isLoading && it.shops.singleOrNull()?.isFavorite == true } }
            membership.set()
            withTimeout(5_000) { vm.uiState.first { it.shops.singleOrNull()?.isFavorite == false } }
            membership.values.value = Result.failure(IllegalStateException("read failed"))
            assertFalse(vm.uiState.value.shops.single().isFavorite)
        } finally { vm.close() }
    }

    @Test fun detailAppliesMembershipEvenWhenDetailsResponseArrivesLater() = runBlocking {
        val membership = Membership()
        val shops = Shops()
        val vm = ShopDetailViewModel("shop", shops, LegacyFavorites, CheckIns, Reviews, Sessions,
            CheckInDraftStore { 0L }, Users, ObserveFavoriteIdsUseCase(membership))
        try {
            membership.set("shop")
            shops.details.complete(CoffeeShopDetails(shop))
            withTimeout(5_000) { vm.uiState.first { it.details?.shop?.isFavorite == true } }
            membership.set()
            withTimeout(5_000) { vm.uiState.first { it.details?.shop?.isFavorite == false } }
            assertEquals("shop", vm.uiState.value.details?.shop?.id)
        } finally { vm.close() }
    }

    @Test fun mappingChangesOnlyLegacyFavoriteFlag() {
        val details = CoffeeShopDetails(shop.copy(isFavorite = false), description = "description")
        val updated = details.withFavoriteMembership(setOf("shop"))
        assertTrue(updated.shop.isFavorite)
        assertEquals("description", updated.description)
        assertEquals(shop.title, updated.shop.title)
    }
}
