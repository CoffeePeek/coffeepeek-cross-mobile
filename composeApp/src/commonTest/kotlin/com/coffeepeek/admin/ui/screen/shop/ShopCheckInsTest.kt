package com.coffeepeek.admin.ui.screen.shop

import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.*

class ShopCheckInsTest {
    private val publicVisit = CheckIn("public", "coffee", "Кофейня", "Отличный кофе", "2026-10-08T12:00:00Z",
        rating = ReviewRating(4, 5, 5), visibility = CheckInVisibility.Public, moderationState = CheckInModerationState.Approved)
    private val ownVisit = publicVisit.copy(id = "own", authorAddress = null)
    private val privateVisit = publicVisit.copy(id = "private", visibility = CheckInVisibility.Private)
    private val details = CoffeeShopDetails(
        shop = CoffeeShop("coffee", "Кофейня", 4.7, reviewCount = 23, cityName = null, priceRange = null, photoUrl = null),
        checkIns = listOf(publicVisit, ownVisit), userCheckIns = listOf(ownVisit, privateVisit),
    )

    @Test
    fun loadsPublicCheckInsAndUsesCheckInHelpfulWhileKeepingCardsStable() = runBlocking {
        val started = Channel<Pair<String, Boolean>>(Channel.UNLIMITED)
        val release = CompletableDeferred<Unit>()
        val repo = ShopTestCheckIns().apply {
            helpful = { id, selected ->
                started.send(id to selected)
                release.await()
                Result.success(CheckInHelpfulVote(true, 8))
            }
        }
        val vm = viewModel(repo, loggedIn = true)
        try {
            val loaded = withTimeout(5_000) { vm.uiState.first { it.details != null && !it.isLoading } }
            val loadedDetails = assertNotNull(loaded.details)
            assertTrue(loadedDetails.reviews.isEmpty())
            assertEquals(details.checkIns, loadedDetails.checkIns)
            assertEquals(23, loadedDetails.shop.reviewCount)
            vm.toggleHelpful("own")
            vm.toggleHelpful("unknown")
            assertNull(vm.uiState.value.helpfulId)
            assertTrue(started.tryReceive().isFailure)
            vm.toggleHelpful("public")
            assertEquals("public" to true, withTimeout(5_000) { started.receive() })
            vm.toggleHelpful("public")
            assertTrue(started.tryReceive().isFailure)
            assertEquals(details.checkIns, vm.uiState.value.details!!.checkIns)
            release.complete(Unit)
            val liked = withTimeout(5_000) { vm.uiState.first { it.helpfulId == null && it.details!!.checkIns.first().helpfulCount == 8 } }
            val likedDetails = assertNotNull(liked.details)
            assertEquals(listOf("public", "own"), likedDetails.checkIns.map { it.id })
            assertTrue(likedDetails.checkIns.first().isHelpfulByCurrentUser)
            assertEquals(ownVisit, likedDetails.checkIns.last())
        } finally { vm.close() }
    }

    @Test
    fun guestKeepsPublicCheckInsAndDoesNotReceivePersonalHistory() = runBlocking {
        val vm = viewModel(ShopTestCheckIns(), loggedIn = false)
        try {
            val loaded = withTimeout(5_000) { vm.uiState.first { it.details != null && !it.isLoading } }
            assertFalse(loaded.isLoggedIn)
            val loadedDetails = assertNotNull(loaded.details)
            assertEquals(details.checkIns, loadedDetails.checkIns)
            assertTrue(loadedDetails.userCheckIns.isEmpty())
        } finally { vm.close() }
    }

    private fun viewModel(checkIns: CheckInRepository, loggedIn: Boolean): ShopDetailViewModel = ShopDetailViewModel(
        "coffee", object : ShopRepository {
            override suspend fun getShopDetails(id: String) = Result.success(details)
            override suspend fun searchShops(filters: ShopFilters): Result<PagedResult<CoffeeShop>> = error("unused")
            override suspend fun getMapContent(bounds: MapBounds, zoom: Float, filters: ShopFilters): Result<MapContent> = error("unused")
            override suspend fun getCatalogs(): Result<ShopCatalogs> = error("unused")
            override suspend fun getConsumedDrinks(): Result<List<ConsumedDrinkOption>> = error("unused")
            override suspend fun getMenuDrinks(): Result<List<CoffeeDrinkDefinition>> = error("unused")
            override suspend fun createShop(input: CreateShopInput): Result<Unit> = error("unused")
            override suspend fun getMyShopSubmissions(status: ModerationStatus, page: Int, pageSize: Int): Result<PagedResult<ShopSubmission>> = error("unused")
        }, object : FavoriteRepository {
            override suspend fun getFavoriteIds(): Set<String> = error("unused")
            override suspend fun isFavorite(shopId: String): Boolean = error("unused")
            override suspend fun getFavorites(): Result<List<CoffeeShopDetails>> = error("unused")
            override suspend fun addFavorite(shop: CoffeeShop, address: String?): Result<Unit> = error("unused")
            override suspend fun removeFavorite(shopId: String): Result<Unit> = error("unused")
            override suspend fun clearAll() = Unit
        }, checkIns, object : SessionRepository {
            private val session = if (loggedIn) Session("token") else null
            override fun peekSession() = session
            override fun applySession(session: Session?) = Unit
            override fun isActiveSession(session: Session?) = session != null
            override suspend fun getSession() = session
            override suspend fun persistSession(session: Session?) = Unit
            override suspend fun saveSession(session: Session?) = Unit
            override suspend fun warmCache() = Unit
            override fun observeSession() = flowOf(session)
            override suspend fun isLoggedIn() = loggedIn
        }, CheckInDraftStore(), object : UserRepository {
            override fun observeProfile() = MutableStateFlow<UserProfile?>(null)
            override suspend fun getMe(): Result<UserProfile> = Result.failure(IllegalStateException("No public author address"))
            override suspend fun refreshProfile(): Result<UserProfile> = error("unused")
            override suspend fun getPublicAvatarUrl(userId: String): Result<String?> = error("unused")
            override suspend fun requestAccountDeletion(): Result<AccountDeletionRequest> = error("unused")
            override suspend fun getAccountDeletionRequest(): Result<AccountDeletionRequest?> = error("unused")
            override suspend fun updateUsername(username: String): Result<Unit> = error("unused")
            override suspend fun updateAbout(about: String): Result<Unit> = error("unused")
            override suspend fun updateAvatar(photo: PendingPhotoUpload): Result<Unit> = error("unused")
        }, object : RoasterFavorites {
            override fun observeFavorites() = flowOf(emptyList<CatalogItem>())
            override suspend fun setFavorite(roaster: CatalogItem, isFavorite: Boolean): Result<Unit> = error("unused")
        },
    )
}

private class ShopTestCheckIns : CheckInRepository {
    var helpful: suspend (String, Boolean) -> Result<CheckInHelpfulVote> = { _, _ -> error("unexpected vote") }
    override suspend fun setHelpful(id: String, helpful: Boolean) = this.helpful(id, helpful)
    override suspend fun createCheckIn(input: CreateCheckInInput): Result<Unit> = error("unused")
    override suspend fun getMyCheckIns(page: Int, pageSize: Int): Result<PagedResult<CheckIn>> = error("unused")
    override suspend fun getMyCheckIns(from: String, to: String, pageSize: Int): Result<List<CheckIn>> = error("unused")
    override suspend fun updateCheckIn(id: String, input: UpdateCheckInInput): Result<CheckIn> = error("unused")
    override suspend fun setVisibility(id: String, visibility: CheckInVisibility): Result<CheckIn> = error("unused")
    override suspend fun report(id: String, text: String): Result<Unit> = error("unused")
}
