package com.coffeepeek.admin.feature.authorblocking

import com.coffeepeek.core.session.SessionStore
import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.*
import com.coffeepeek.domain.feature.feed.*
import kotlinx.coroutines.flow.MutableStateFlow

private fun page(items: List<CheckIn>, number: Int = 1, totalPages: Int = 1) = PagedResult(items, items.size, totalPages, number)

internal class ScreenshotCheckIns : CheckInRepository {
    var list: suspend (Int, Int) -> Result<PagedResult<CheckIn>> = { _, _ -> Result.success(page(emptyList())) }
    var update: suspend (String, UpdateCheckInInput) -> Result<CheckIn> = { _, _ -> error("Unused") }
    var helpful: suspend (String, Boolean) -> Result<CheckInHelpfulVote> = { _, _ -> error("Unused") }
    var report: suspend (String, String) -> Result<Unit> = { _, _ -> error("Unused") }
    override suspend fun setHelpful(id: String, helpful: Boolean) = this.helpful(id, helpful)
    override suspend fun report(id: String, text: String): Result<Unit> = this.report.invoke(id, text)
    var visibility: suspend (String, CheckInVisibility) -> Result<CheckIn> = { _, _ -> error("Unused") }
    override suspend fun getMyCheckIns(page: Int, pageSize: Int) = list(page, pageSize)
    override suspend fun updateCheckIn(id: String, input: UpdateCheckInInput) = update(id, input)
    override suspend fun setVisibility(id: String, visibility: CheckInVisibility) = this.visibility(id, visibility)
    override suspend fun createCheckIn(input: CreateCheckInInput): Result<Unit> = error("Unused")
    override suspend fun getMyCheckIns(from: String, to: String, pageSize: Int): Result<List<CheckIn>> = error("Unused")
}

internal class ScreenshotSessions(initial: Session?) : SessionStore {
    private val session = MutableStateFlow(initial)
    override fun peekSession() = session.value
    override fun applySession(session: Session?) { this.session.value = session }
    override fun isActiveSession(session: Session?) = !session?.accessToken.isNullOrBlank()
    override suspend fun getSession() = session.value
    override suspend fun persistSession(session: Session?) = applySession(session)
    override suspend fun saveSession(session: Session?) = applySession(session)
    override suspend fun warmCache() = Unit
    override fun observeSession() = session
    override suspend fun isLoggedIn() = isActiveSession(session.value)
}

internal class ScreenshotShops : ShopRepository {
    override suspend fun getConsumedDrinks() = Result.success(listOf(ConsumedDrinkOption("cappuccino", "Капучино", "Cappuccino")))
    override suspend fun searchShops(filters: ShopFilters): Result<PagedResult<CoffeeShop>> = error("Unused")
    override suspend fun getShopDetails(id: String): Result<CoffeeShopDetails> = error("Unused")
    override suspend fun getMapContent(bounds: MapBounds, zoom: Float, filters: ShopFilters): Result<MapContent> = error("Unused")
    override suspend fun getCatalogs() = Result.success(ShopCatalogs())
    override suspend fun getMenuDrinks(): Result<List<CoffeeDrinkDefinition>> = error("Unused")
    override suspend fun createShop(input: CreateShopInput): Result<Unit> = error("Unused")
    override suspend fun getMyShopSubmissions(status: ModerationStatus, page: Int, pageSize: Int): Result<PagedResult<ShopSubmission>> = error("Unused")
}

internal class ScreenshotFeed : FeedRepository {
    var load: suspend (Int, String?, FeedFilters) -> Result<FeedPage> = { _, _, _ -> Result.success(FeedPage(emptyList(), null)) }
    override suspend fun getFeed(pageSize: Int, cursor: String?, filters: FeedFilters) = load(pageSize, cursor, filters)
}

internal class ScreenshotUsers : UserRepository {
    var load: suspend () -> Result<UserProfile> = { Result.success(UserProfile("Me", "", null, null, 0, 0, 0, PublicAddress("me", "/users/me", 1, false))) }
    override suspend fun refreshProfile() = load()
    override suspend fun getMe() = load()
    override fun observeProfile() = MutableStateFlow<UserProfile?>(null)
    override suspend fun getPublicAvatarUrl(userId: String): Result<String?> = error("Unused")
    override suspend fun requestAccountDeletion(): Result<AccountDeletionRequest> = error("Unused")
    override suspend fun getAccountDeletionRequest(): Result<AccountDeletionRequest?> = error("Unused")
    override suspend fun updateUsername(username: String): Result<Unit> = error("Unused")
    override suspend fun updateAbout(about: String): Result<Unit> = error("Unused")
    override suspend fun updateAvatar(photo: PendingPhotoUpload): Result<Unit> = error("Unused")
}
