package com.coffeepeek.admin.feature.community.ui

import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.CheckInHelpfulVote
import com.coffeepeek.domain.repository.UserRepository
import com.coffeepeek.domain.feature.feed.*
import com.coffeepeek.domain.repository.CheckInRepository
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.ShopRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.*

class CommunityViewModelTest {
    private val own = CheckIn(
        "own", "coffee", "Кофейня", "Кофе", "2026-10-07T10:00:00Z", visitedAt = "2026-10-07T09:00:00Z",
        rating = ReviewRating(4, 4, 5), drinkSlug = "cappuccino", drinkNameRu = "Капучино",
        photoUrls = listOf("https://api.example/private-photo"), authorAddress = null,
        visibility = CheckInVisibility.Public, moderationState = CheckInModerationState.Approved, contentRevision = 1,
    )

    @Test
    fun guestsMakeNoHistoryRequestsAndLogoutClearsOwnPostsAndEditor() = runBlocking {
        var requests = 0
        val sessions = FeedSessions(null)
        val repo = FeedCheckIns().apply { list = { _, _ -> requests++; Result.success(page(listOf(own))) } }
        val vm = CommunityViewModel(repo, sessions, FeedShops(), TestPublicFeed(), FeedUsers())
        try {
            vm.await { it.isLoggedIn == false }
            assertEquals(0, requests)
            sessions.applySession(Session("token", userId = "one"))
            vm.await { it.isLoggedIn == true }
            vm.selectTimeline(CommunityTimeline.Mine)
            vm.await { it.items == listOf(own) && !it.isLoading }
            vm.edit("foreign-id")
            assertNull(vm.state.value.editing)
            vm.edit("own")
            assertEquals(own, vm.state.value.editing?.source)
            sessions.applySession(null)
            val loggedOut = vm.await { it.isLoggedIn == false }
            assertTrue(loggedOut.items.isEmpty())
            assertNull(loggedOut.editing)
            assertTrue(loggedOut.drinks.isEmpty())
            assertEquals(1, requests)
        } finally { vm.close() }
    }

    @Test
    fun editingPreservesDrinkAndReplacesCardWithPendingServerVersion() = runBlocking {
        val sent = Channel<Pair<String, UpdateCheckInInput>>(Channel.UNLIMITED)
        val result = CompletableDeferred<Result<CheckIn>>()
        val repo = FeedCheckIns().apply {
            list = { _, _ -> Result.success(page(listOf(own))) }
            update = { id, input -> sent.send(id to input); result.await() }
        }
        val vm = personalViewModel(repo, FeedSessions(Session("token", userId = "one")))
        try {
            vm.await { it.items.isNotEmpty() && !it.isLoading }
            vm.edit("own")
            val edit = vm.state.value.editing!!
            vm.updateEdit(edit.copy(text = " \n "))
            vm.saveEdit()
            assertNotNull(vm.state.value.editing?.error)
            assertTrue(sent.tryReceive().isFailure)
            vm.updateEdit(edit.copy(text = " Новый текст ", rating = ReviewRating(5, 4, 5)))
            vm.saveEdit()
            val request = withTimeout(5_000) { sent.receive() }
            assertEquals("own", request.first)
            assertEquals("Новый текст", request.second.text)
            assertEquals("cappuccino", request.second.drinkSlug)
            assertNull(request.second.customDrinkName)
            val server = own.copy(note = request.second.text, rating = request.second.rating,
                moderationState = CheckInModerationState.Pending, contentRevision = 2)
            result.complete(Result.success(server))
            val saved = vm.await { it.editing == null && !it.isSaving && it.items.single().contentRevision == 2 }
            assertEquals(server, saved.items.single())
            assertEquals(own.visitedAt, saved.items.single().visitedAt)
            assertEquals(own.photoUrls, saved.items.single().photoUrls)
            assertEquals("На проверке", saved.items.single().publicationLabel())
        } finally { vm.close() }
    }

    @Test
    fun failedSaveKeepsDraftAndVisibilityUsesItsOwnOperation() = runBlocking {
        var editRequests = 0
        val visibilityRequests = Channel<CheckInVisibility>(Channel.UNLIMITED)
        val repo = FeedCheckIns().apply {
            list = { _, _ -> Result.success(page(listOf(own))) }
            update = { _, _ -> editRequests++; Result.failure(IllegalStateException("Повторите позже")) }
            visibility = { _, value ->
                visibilityRequests.send(value)
                Result.success(own.copy(visibility = value, moderationState = CheckInModerationState.NotSubmitted))
            }
        }
        val vm = personalViewModel(repo, FeedSessions(Session("token", userId = "one")))
        try {
            vm.await { !it.isLoading && it.items.isNotEmpty() }
            vm.edit("own")
            vm.updateEdit(vm.state.value.editing!!.copy(text = "Сохраните этот текст", drinkSlug = null, customDrinkName = null))
            vm.saveEdit()
            val failed = vm.await { it.editing?.error == "Повторите позже" && !it.isSaving }
            assertEquals("Сохраните этот текст", failed.editing?.text)
            assertNull(failed.editing?.drinkSlug)
            assertEquals(listOf(own), failed.items)
            vm.dismissEdit()
            vm.toggleVisibility("own")
            assertEquals(CheckInVisibility.Private, withTimeout(5_000) { visibilityRequests.receive() })
            val hidden = vm.await { it.items.single().visibility == CheckInVisibility.Private && it.changingVisibilityId == null }
            assertEquals(1, editRequests)
            assertEquals(1, hidden.items.single().contentRevision)
            assertEquals("Приватный", hidden.items.single().publicationLabel())
        } finally { vm.close() }
    }

    @Test
    fun paginationRetriesFailedPageAndDeduplicatesWithoutReorderingHistory() = runBlocking {
        val pages = mutableListOf<Int>()
        val second = own.copy(id = "second", visitedAt = "2026-11-01T00:00:00Z")
        var fail = true
        val repo = FeedCheckIns().apply { list = { number, _ ->
            pages += number
            when {
                number == 1 -> Result.success(page(listOf(own), number = 1, totalPages = 2))
                fail -> { fail = false; Result.failure(IllegalStateException("Offline")) }
                else -> Result.success(page(listOf(own, second), number = 2, totalPages = 2))
            }
        } }
        val vm = personalViewModel(repo, FeedSessions(Session("token", userId = "one")))
        try {
            vm.await { it.page == 1 && !it.isLoading }
            vm.loadMore()
            vm.await { it.error == "Offline" }
            vm.retry()
            val all = vm.await { it.page == 2 && !it.isLoadingMore }
            assertEquals(listOf("own", "second"), all.items.map { it.id })
            assertEquals(listOf(1, 2, 2), pages)
            assertFalse(all.hasMore)
        } finally { vm.close() }
    }

    @Test
    fun delayedResponseForPreviousAccountCannotRestorePrivatePosts() = runBlocking {
        val sessions = FeedSessions(Session("old-token", userId = "one"))
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        val newVisit = own.copy(id = "new-user-visit")
        val repo = FeedCheckIns().apply { list = { _, _ ->
            if (sessions.peekSession()?.userId == "one") {
                withContext(NonCancellable) {
                    started.complete(Unit); release.await(); finished.complete(Unit)
                    Result.success(page(listOf(own)))
                }
            } else Result.success(page(listOf(newVisit)))
        } }
        val vm = personalViewModel(repo, sessions)
        try {
            withTimeout(5_000) { started.await() }
            sessions.applySession(Session("new-token", userId = "two"))
            vm.await { it.items == listOf(newVisit) && !it.isLoading }
            release.complete(Unit)
            withTimeout(5_000) { finished.await() }
            assertEquals(listOf(newVisit), vm.state.value.items)
        } finally { release.complete(Unit); vm.close() }
    }

    private suspend fun CommunityViewModel.await(predicate: (CommunityUiState) -> Boolean) = withTimeout(5_000) { state.first(predicate) }
}

private fun page(items: List<CheckIn>, number: Int = 1, totalPages: Int = 1) = PagedResult(items, items.size, totalPages, number)

internal class FeedCheckIns : CheckInRepository {
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

internal class FeedSessions(initial: Session?) : SessionRepository {
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

internal class FeedShops : ShopRepository {
    override suspend fun getConsumedDrinks() = Result.success(listOf(ConsumedDrinkOption("cappuccino", "Капучино", "Cappuccino")))
    override suspend fun searchShops(filters: ShopFilters): Result<PagedResult<CoffeeShop>> = error("Unused")
    override suspend fun getShopDetails(id: String): Result<CoffeeShopDetails> = error("Unused")
    override suspend fun getMapContent(bounds: MapBounds, zoom: Float, filters: ShopFilters): Result<MapContent> = error("Unused")
    override suspend fun getCatalogs(): Result<ShopCatalogs> = error("Unused")
    override suspend fun getMenuDrinks(): Result<List<CoffeeDrinkDefinition>> = error("Unused")
    override suspend fun createShop(input: CreateShopInput): Result<Unit> = error("Unused")
    override suspend fun getMyShopSubmissions(status: ModerationStatus, page: Int, pageSize: Int): Result<PagedResult<ShopSubmission>> = error("Unused")
}

private suspend fun personalViewModel(repo: CheckInRepository, sessions: FeedSessions): CommunityViewModel {
    val vm = CommunityViewModel(repo, sessions, FeedShops(), TestPublicFeed(), FeedUsers())
    withTimeout(5_000) { vm.state.first { it.isLoggedIn == true } }
    vm.selectTimeline(CommunityTimeline.Mine)
    return vm
}

internal class TestPublicFeed : FeedRepository {
    var load: suspend (Int, String?, FeedFilters) -> Result<FeedPage> = { _, _, _ -> Result.success(FeedPage(emptyList(), null)) }
    override suspend fun getFeed(pageSize: Int, cursor: String?, filters: FeedFilters) = load(pageSize, cursor, filters)
}

internal class FeedUsers : UserRepository {
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
