package com.coffeepeek.admin.ui.screen.shop

import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.admin.utils.FavoriteSync
import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.*

class ShopCheckInsTest {
    private fun ShopDetailUiState.checkInMessage(): String? = checkInFeedback?.message(feedbackLabels) ?: checkInError

    private val feedbackLabels = ShopDetailLabels("Не удалось загрузить избранное", "Не удалось загрузить блокировки. Повторите попытку.", "Не удалось определить адрес кофейни", "Войдите в аккаунт, чтобы создать чекин", "Не удалось отправить чекин. Попробуйте ещё раз", "Не удалось изменить избранное", "Номер скопирован")

    private val publicVisit = CheckIn("public", "coffee", "Кофейня", "Отличный кофе", "2026-10-08T12:00:00Z",
        rating = ReviewRating(4, 5, 5), visibility = CheckInVisibility.Public, moderationState = CheckInModerationState.Approved)
    private val ownVisit = publicVisit.copy(id = "own", authorAddress = null)
    private val privateVisit = publicVisit.copy(id = "private", visibility = CheckInVisibility.Private)
    private val details = CoffeeShopDetails(
        shop = CoffeeShop("coffee", "Кофейня", 4.7, reviewCount = 23, cityName = null, priceRange = null, photoUrl = null,
            publicAddress = PublicAddress("coffee", "/shops/coffee", 1, false)),
        checkIns = listOf(publicVisit, ownVisit), userCheckIns = listOf(ownVisit, privateVisit),
    )

    @Test
    fun preparedFormOpeningDoesNotOpenLegacyDraftOrLoadDrinksTwice() = runBlocking {
        val drafts = CheckInDraftStore()
        val existing = drafts.open("another-shop").copy(note = "Unchanged legacy draft")
        drafts.save(existing)
        var drinkRequests = 0
        val vm = viewModel(ShopTestCheckIns(), loggedIn = true, draftStore = drafts,
            drinks = { drinkRequests++; Result.success(emptyList()) })
        try {
            withTimeout(5_000) { vm.uiState.first { it.details != null && !it.isLoading } }
            vm.openCheckInSheet(prepareLegacyDraft = false)
            val opened = withTimeout(5_000) { vm.uiState.first { it.showCheckInSheet } }
            assertNull(opened.checkInDraft)
            assertSame(existing, drafts.peek())
            assertEquals(0, drinkRequests)
            vm.dismissCheckInSheet()
            assertFalse(vm.uiState.value.showCheckInSheet)
        } finally { vm.close() }
    }

    @Test
    fun acceptedPreparedCheckInRefreshesDetailsWithoutClearingDraftOrHidingConfirmation() = runBlocking {
        val drafts = CheckInDraftStore()
        val retained = drafts.open("coffee").copy(note = "Draft owned by feature")
        drafts.save(retained)
        var loads = 0
        val vm = viewModel(ShopTestCheckIns(), loggedIn = true, draftStore = drafts,
            detailsRequest = {
                loads++
                Result.success(details.copy(shop = details.shop.copy(reviewCount = loads)))
            })
        try {
            withTimeout(5_000) { vm.uiState.first { it.details != null && !it.isLoading } }
            vm.openCheckInSheet(prepareLegacyDraft = false)
            withTimeout(5_000) { vm.uiState.first { it.showCheckInSheet } }
            vm.refreshAfterCheckIn()
            val refreshed = withTimeout(5_000) { vm.uiState.first { it.details?.shop?.reviewCount == 2 } }
            assertTrue(refreshed.showCheckInSheet)
            assertFalse(refreshed.isLoading)
            assertSame(retained, drafts.peek())
            assertEquals(2, loads)
        } finally { vm.close() }
    }

    @Test
    fun preparedFormStillRoutesGuestsToAuthWithoutOpeningDraftOrReadingDrinks() = runBlocking {
        val drafts = CheckInDraftStore()
        var drinkRequests = 0
        val vm = viewModel(ShopTestCheckIns(), loggedIn = false, draftStore = drafts,
            drinks = { drinkRequests++; Result.success(emptyList()) })
        try {
            withTimeout(5_000) { vm.uiState.first { it.details != null && !it.isLoading } }
            val navigation = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(5_000) { Navigator.navigationEvents.first() }
            }
            vm.openCheckInSheet(prepareLegacyDraft = false)
            assertEquals(Navigator.NavEvent.NavigateTo(Navigator.Screen.Auth), navigation.await())
            assertFalse(vm.uiState.value.showCheckInSheet)
            assertNull(drafts.peek())
            assertEquals(0, drinkRequests)
        } finally { vm.close() }
    }

    @Test
    fun completedWriteRefreshCannotCommitDetailsAfterAccountReplacement() = runBlocking {
        var session = Session("token", userId = "owner")
        val rejectedCommit = CompletableDeferred<Unit>()
        var loads = 0
        val vm = viewModel(ShopTestCheckIns(), loggedIn = true,
            sessionValue = {
                session.also { if (it.userId == "other") rejectedCommit.complete(Unit) }
            }, detailsRequest = {
                if (++loads == 1) Result.success(details) else {
                    session = Session("changed", userId = "other")
                    Result.success(details.copy(shop = details.shop.copy(title = "Stale response")))
                }
            })
        try {
            withTimeout(5_000) { vm.uiState.first { it.details != null && !it.isLoading } }
            vm.refreshAfterCheckIn()
            withTimeout(5_000) { rejectedCommit.await() }
            assertEquals(details.shop.title, vm.uiState.value.details!!.shop.title)
            assertFalse(vm.uiState.value.isLoading)
        } finally { vm.close() }
    }

    @Test
    fun removesFavoriteByCanonicalPublicSlugWhenDetailWasOpenedThroughAnAlias() = runBlocking {
        val removed = Channel<String>(Channel.UNLIMITED)
        val vm = viewModel(
            ShopTestCheckIns(), loggedIn = true, routeId = "old-coffee-alias",
            shopDetails = details.copy(shop = details.shop.copy(id = "catalog-guid", isFavorite = true)),
            onRemoveFavorite = { slug -> removed.send(slug); Result.success(Unit) },
        )
        val change = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(5_000) { FavoriteSync.changes.first() }
        }
        try {
            withTimeout(5_000) { vm.uiState.first { it.details != null && !it.isLoading } }
            vm.toggleFavorite()
            assertEquals("coffee", withTimeout(5_000) { removed.receive() })
            assertEquals(FavoriteSync.FavoriteChange("coffee", false), change.await())
            val updated = withTimeout(5_000) { vm.uiState.first { it.details?.shop?.isFavorite == false } }
            assertEquals("coffee", updated.details?.shop?.publicAddress?.slug)
        } finally {
            change.cancel()
            removed.close()
            vm.close()
        }
    }

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
    fun ownPublishedCheckInCanBeLikedAndUnlikedWithoutChangingOtherCards() = runBlocking {
        val requests = Channel<Pair<String, Boolean>>(Channel.UNLIMITED)
        val repo = ShopTestCheckIns().apply {
            helpful = { id, helpful ->
                requests.send(id to helpful)
                Result.success(CheckInHelpfulVote(helpful, if (helpful) 8 else 3))
            }
        }
        val vm = viewModel(repo, loggedIn = true)
        try {
            val loaded = withTimeout(5_000) { vm.uiState.first { it.details != null && !it.isLoading } }
            assertTrue(loaded.details!!.ownsCheckIn(ownVisit, loaded.currentUserId))
            vm.toggleHelpful(ownVisit.id)
            assertEquals(ownVisit.id to true, withTimeout(5_000) { requests.receive() })
            val liked = withTimeout(5_000) { vm.uiState.first { it.helpfulId == null && it.details!!.checkIns.last().isHelpfulByCurrentUser } }
            assertEquals(8, liked.details!!.checkIns.last().helpfulCount)
            vm.toggleHelpful(ownVisit.id)
            assertEquals(ownVisit.id to false, withTimeout(5_000) { requests.receive() })
            val unliked = withTimeout(5_000) { vm.uiState.first { it.helpfulId == null && !it.details!!.checkIns.last().isHelpfulByCurrentUser } }
            assertEquals(3, unliked.details!!.checkIns.last().helpfulCount)
            assertEquals(listOf("public", "own"), unliked.details!!.checkIns.map { it.id })
            assertEquals(publicVisit, unliked.details!!.checkIns.first())
        } finally { vm.close(); requests.close() }
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

    @Test
    fun submissionBlocksDuplicateAndDismissAndKeepsSuccessVisibleUntilAcknowledged() = runBlocking {
        for (visibility in CheckInVisibility.entries) {
            val created = Channel<Unit>(Channel.UNLIMITED)
            val started = Channel<CreateCheckInInput>(Channel.UNLIMITED)
            val release = CompletableDeferred<Unit>()
            val visitTime = kotlin.time.Instant.parse("2026-10-14T18:42:17.123Z")
            val store = CheckInDraftStore { visitTime.toEpochMilliseconds() }
            val repo = ShopTestCheckIns().apply { create = { input ->
                started.send(input)
                release.await()
                Result.success(Unit)
            } }
            val vm = viewModel(repo, loggedIn = true, draftStore = store, onCheckInCreated = { created.trySend(Unit) })
            try {
                withTimeout(5_000) { vm.uiState.first { it.details != null && !it.isLoading } }
                vm.openCheckInSheet()
                val opened = withTimeout(5_000) { vm.uiState.first { it.showCheckInSheet } }
                val draft = opened.checkInDraft!!.copy(isPublic = visibility == CheckInVisibility.Public)
                vm.updateCheckInDraft(draft)
                vm.checkIn(draft)
                assertTrue(vm.uiState.value.isCheckInLoading)
                val input = withTimeout(5_000) { started.receive() }
                assertEquals(visibility, input.visibility)
                assertEquals("", input.text)
                assertEquals(ReviewRating(place = 4, service = 4, coffee = 4), input.rating)
                assertEquals("2026-10-14T18:42:17.123Z", input.visitedAtIso,
                    "Both public and private shop check-ins must send the actual visit time, not midnight")
                vm.checkIn(draft)
                vm.dismissCheckInSheet()
                vm.updateCheckInDraft(draft.copy(note = "Changed during submission"))
                assertTrue(vm.uiState.value.showCheckInSheet)
                assertEquals(draft, vm.uiState.value.checkInDraft)
                assertTrue(started.tryReceive().isFailure)
                assertTrue(created.tryReceive().isFailure)
                release.complete(Unit)
                withTimeout(5_000) { created.receive() }
                val success = withTimeout(5_000) { vm.uiState.first { it.submittedCheckInVisibility != null } }
                assertFalse(success.isCheckInLoading)
                assertTrue(success.showCheckInSheet)
                assertEquals(visibility, success.submittedCheckInVisibility)
                assertEquals("", store.open("coffee").note)
                vm.checkIn(draft)
                assertTrue(started.tryReceive().isFailure)
                assertTrue(created.tryReceive().isFailure)
                vm.dismissCheckInSheet()
                assertFalse(vm.uiState.value.showCheckInSheet)
                assertNull(vm.uiState.value.submittedCheckInVisibility)
                assertNull(vm.uiState.value.checkInDraft)
            } finally { release.complete(Unit); vm.close(); created.close() }
        }
    }

    @Test
    fun failedSubmissionKeepsDraftAndCanRetry() = runBlocking {
        val created = Channel<Unit>(Channel.UNLIMITED)
        val store = CheckInDraftStore()
        var attempts = 0
        val repo = ShopTestCheckIns().apply { create = { input ->
            assertEquals(ReviewRating(place = 5, service = 3, coffee = 2), input.rating)
            if (++attempts == 1) Result.failure(IllegalStateException("Нет сети")) else Result.success(Unit)
        } }
        val vm = viewModel(repo, loggedIn = true, draftStore = store, onCheckInCreated = { created.trySend(Unit) })
        try {
            withTimeout(5_000) { vm.uiState.first { it.details != null && !it.isLoading } }
            vm.openCheckInSheet()
            val opened = withTimeout(5_000) { vm.uiState.first { it.showCheckInSheet } }
            val draft = opened.checkInDraft!!.copy(note = "Попробовать ещё раз",
                placeRating = 5, serviceRating = 3, coffeeRating = 2,
                photos = listOf(PickedImage(byteArrayOf(1, 2, 3), "coffee.jpg")))
            vm.updateCheckInDraft(draft)
            vm.checkIn(draft)
            val failed = withTimeout(5_000) { vm.uiState.first { it.checkInMessage() != null && !it.isCheckInLoading } }
            assertEquals("Нет сети", failed.checkInError)
            assertEquals(draft, failed.checkInDraft)
            assertEquals(draft, store.open("coffee"))
            assertTrue(failed.showCheckInSheet)
            assertNull(failed.submittedCheckInVisibility)
            assertTrue(created.tryReceive().isFailure)
            vm.checkIn(draft)
            withTimeout(5_000) { created.receive() }
            val success = withTimeout(5_000) { vm.uiState.first { it.submittedCheckInVisibility != null } }
            assertNull(success.checkInError)
            assertEquals(2, attempts)
        } finally { vm.close(); created.close() }
    }

    @Test
    fun invalidDraftsAndMissingShopAddressNeverSendARequest() = runBlocking {
        var creations = 0
        val repo = ShopTestCheckIns().apply { create = { creations++; Result.failure(IllegalStateException("unexpected creation")) } }
        val vm = viewModel(repo, loggedIn = true)
        try {
            withTimeout(5_000) { vm.uiState.first { it.details != null && !it.isLoading } }
            val draft = CheckInDraft("coffee", visitMillis = 1000, note = "Кофе")
            for (invalid in listOf(
                draft.copy(note = "a".repeat(1001)), draft.copy(visitMillis = 0),
                draft.copy(visitMillis = Long.MAX_VALUE), draft.copy(coffeeRating = 0), draft.copy(serviceRating = 6),
                draft.copy(placeRating = 0), draft.copy(drinkSlug = "other", customDrinkName = " "),
                draft.copy(photos = List(6) { PickedImage(byteArrayOf(1), "coffee.jpg") }),
            )) {
                vm.checkIn(invalid)
                assertFalse(vm.uiState.value.isCheckInLoading)
                assertNotNull(vm.uiState.value.checkInValidationError)
                assertNull(vm.uiState.value.submittedCheckInVisibility)
            }
            assertEquals(0, creations)
        } finally { vm.close() }
        val missingAddress = viewModel(repo, loggedIn = true,
            shopDetails = details.copy(shop = details.shop.copy(publicAddress = null)))
        try {
            withTimeout(5_000) { missingAddress.uiState.first { it.details != null && !it.isLoading } }
            missingAddress.checkIn(CheckInDraft("coffee", visitMillis = 1000, note = "Кофе"))
            assertEquals("Не удалось определить адрес кофейни", missingAddress.uiState.value.checkInMessage())
            assertFalse(missingAddress.uiState.value.isCheckInLoading)
            assertEquals(0, creations)
        } finally { missingAddress.close() }
    }

    @Test
    fun emptyErrorAndUnexpectedExceptionRestoreEditableDraftWithVisibleMessage() = runBlocking {
        for (failure in listOf(IllegalStateException(), IllegalStateException(""), IllegalStateException("   "))) {
            for (throws in listOf(false, true)) {
                val repo = ShopTestCheckIns().apply { create = { if (throws) throw failure else Result.failure(failure) } }
                val vm = viewModel(repo, loggedIn = true)
                try {
                    withTimeout(5_000) { vm.uiState.first { it.details != null && !it.isLoading } }
                    vm.openCheckInSheet()
                    val opened = withTimeout(5_000) { vm.uiState.first { it.showCheckInSheet } }
                    val draft = opened.checkInDraft!!.copy(note = "Сохранить", photos = listOf(PickedImage(byteArrayOf(1), "coffee.jpg")))
                    vm.updateCheckInDraft(draft)
                    vm.checkIn(draft)
                    val failed = withTimeout(5_000) { vm.uiState.first { !it.isCheckInLoading && it.checkInMessage() != null } }
                    assertTrue(failed.checkInMessage()?.isNotBlank() == true)
                    assertEquals(draft, failed.checkInDraft)
                    assertTrue(failed.showCheckInSheet)
                    assertNull(failed.submittedCheckInVisibility)
                } finally { vm.close() }
            }
        }
    }

    @Test
    fun sessionEndingBeforeSubmitPreventsCreationAndRetainsDraft() = runBlocking {
        var signedIn = true
        val store = CheckInDraftStore()
        val vm = viewModel(ShopTestCheckIns(), loggedIn = true, draftStore = store, sessionActive = { signedIn })
        try {
            withTimeout(5_000) { vm.uiState.first { it.details != null && !it.isLoading } }
            vm.openCheckInSheet()
            val opened = withTimeout(5_000) { vm.uiState.first { it.showCheckInSheet } }
            val draft = opened.checkInDraft!!.copy(note = "Кофе")
            vm.updateCheckInDraft(draft)
            signedIn = false
            vm.checkIn(draft)
            val failed = withTimeout(5_000) { vm.uiState.first { it.checkInMessage() != null && !it.isCheckInLoading } }
            assertEquals("Войдите в аккаунт, чтобы создать чекин", failed.checkInMessage())
            assertEquals(draft, store.open("coffee"))
            assertNull(failed.submittedCheckInVisibility)
        } finally { vm.close() }
    }

    @Test
    fun drinkCatalogFailureCanRetryWithoutDiscardingDraft() = runBlocking {
        var attempts = 0
        val drink = ConsumedDrinkOption("cappuccino", "Капучино", "Cappuccino")
        val vm = viewModel(ShopTestCheckIns(), loggedIn = true, drinks = {
            if (++attempts == 1) Result.failure(IllegalStateException("Нет сети")) else Result.success(listOf(drink))
        })
        try {
            withTimeout(5_000) { vm.uiState.first { it.details != null && !it.isLoading } }
            vm.openCheckInSheet()
            val failed = withTimeout(5_000) { vm.uiState.first { it.showCheckInSheet && it.drinksError != null } }
            val draft = failed.checkInDraft!!.copy(note = "Сохранить заметку")
            vm.updateCheckInDraft(draft); vm.loadDrinks()
            val restored = withTimeout(5_000) { vm.uiState.first { it.drinks == listOf(drink) && it.drinksError == null } }
            assertEquals(draft, restored.checkInDraft)
            assertTrue(restored.showCheckInSheet)
        } finally { vm.close() }
    }

    @Test
    fun authenticationCheckExceptionDoesNotLeaveSubmissionLoading() = runBlocking {
        var fail = false
        val vm = viewModel(ShopTestCheckIns(), loggedIn = true, sessionActive = {
            if (fail) throw IllegalStateException("Не удалось проверить вход") else true
        })
        try {
            withTimeout(5_000) { vm.uiState.first { it.details != null && !it.isLoading } }
            vm.openCheckInSheet()
            val opened = withTimeout(5_000) { vm.uiState.first { it.showCheckInSheet } }
            val draft = opened.checkInDraft!!.copy(note = "Кофе")
            vm.updateCheckInDraft(draft); fail = true; vm.checkIn(draft)
            val failed = withTimeout(5_000) { vm.uiState.first { !it.isCheckInLoading && it.checkInMessage() != null } }
            assertEquals("Не удалось проверить вход", failed.checkInError)
            assertEquals(draft, failed.checkInDraft)
            assertTrue(failed.showCheckInSheet)
            assertNull(failed.submittedCheckInVisibility)
        } finally { vm.close() }
    }

    @Test
    fun submitsOriginalPhotosAfterReopeningDraft() = runBlocking {
        val original = PickedImage(byteArrayOf(1, 2, 3), "coffee.jpg")
        val submitted = CompletableDeferred<CreateCheckInInput>()
        val store = CheckInDraftStore()
        val vm = viewModel(ShopTestCheckIns().apply { create = {
            submitted.complete(it)
            Result.success(Unit)
        } }, loggedIn = true, draftStore = store)
        try {
            withTimeout(5_000) { vm.uiState.first { it.details != null && !it.isLoading } }
            vm.openCheckInSheet()
            val draft = withTimeout(5_000) { vm.uiState.first { it.showCheckInSheet } }.checkInDraft!!
            vm.updateCheckInDraft(draft.copy(photos = listOf(original)))
            vm.dismissCheckInSheet()
            vm.openCheckInSheet()
            val reopened = withTimeout(5_000) { vm.uiState.first { it.showCheckInSheet } }.checkInDraft!!
            assertEquals(listOf(original), reopened.photos)
            vm.checkIn(reopened)
            val input = withTimeout(5_000) { submitted.await() }
            assertEquals(listOf("coffee.jpg"), input.photos.map { it.fileName })
            assertContentEquals(original.bytes, input.photos.single().bytes)
        } finally { vm.close() }
    }

    @Test
    fun preparedFeedbackAndShareCopyAreBelarusianWithoutChangingTheShopTitle() {
        assertEquals("Нумар скапіяваны", ShopDetailFeedback.PhoneCopied.message(feedbackLabels.copy(phoneCopied = "Нумар скапіяваны")))
        val share = ShopShareLabels("Кавярня «%1\$s»: %2\$s", "Кавярня: %1\$s")
        assertEquals("Кавярня «Кава %2\$s»: https://coffeepeek.by/shop", share.text("Кава %2\$s", "https://coffeepeek.by/shop"))
        assertEquals("Кавярня: https://coffeepeek.by/shop", share.text(null, "https://coffeepeek.by/shop"))
    }

    private fun viewModel(
        checkIns: CheckInRepository, loggedIn: Boolean, draftStore: CheckInDraftStore = CheckInDraftStore(),
        shopDetails: CoffeeShopDetails = details, sessionActive: () -> Boolean = { loggedIn },
        drinks: suspend () -> Result<List<ConsumedDrinkOption>> = { Result.success(emptyList()) },
        detailsRequest: suspend () -> Result<CoffeeShopDetails> = { Result.success(shopDetails) },
        sessionValue: () -> Session? = { if (loggedIn) Session("token", userId = "owner") else null },
        routeId: String = "coffee",
        onRemoveFavorite: suspend (String) -> Result<Unit> = { error("unused") },
        onCheckInCreated: () -> Unit = {},
    ): ShopDetailViewModel = ShopDetailViewModel(
        shopId = routeId,
        shopRepository = object : ShopRepository {
            override suspend fun getShopDetails(id: String) = detailsRequest()
            override suspend fun searchShops(filters: ShopFilters): Result<PagedResult<CoffeeShop>> = error("unused")
            override suspend fun getMapContent(bounds: MapBounds, zoom: Float, filters: ShopFilters): Result<MapContent> = error("unused")
            override suspend fun getCatalogs(): Result<ShopCatalogs> = error("unused")
            override suspend fun getConsumedDrinks(): Result<List<ConsumedDrinkOption>> = drinks()
            override suspend fun getMenuDrinks(): Result<List<CoffeeDrinkDefinition>> = error("unused")
            override suspend fun createShop(input: CreateShopInput): Result<Unit> = error("unused")
            override suspend fun getMyShopSubmissions(status: ModerationStatus, page: Int, pageSize: Int): Result<PagedResult<ShopSubmission>> = error("unused")
        }, favoriteRepository = object : FavoriteRepository {
            override suspend fun getFavoriteIds(): Set<String> = error("unused")
            override suspend fun isFavorite(shopId: String): Boolean = error("unused")
            override suspend fun getFavorites(): Result<List<CoffeeShopDetails>> = error("unused")
            override suspend fun addFavorite(shop: CoffeeShop, address: String?): Result<Unit> = error("unused")
            override suspend fun removeFavorite(shopId: String): Result<Unit> = onRemoveFavorite(shopId)
            override suspend fun clearAll() = Unit
        }, checkInRepository = checkIns,
        sessionRepository = object : SessionRepository {
            override fun peekSession() = sessionValue()
            override fun applySession(session: Session?) = Unit
            override fun isActiveSession(session: Session?) = session != null
            override suspend fun getSession() = sessionValue()
            override suspend fun persistSession(session: Session?) = Unit
            override suspend fun saveSession(session: Session?) = Unit
            override suspend fun warmCache() = Unit
            override fun observeSession() = flowOf(sessionValue())
            override suspend fun isLoggedIn() = sessionActive()
        }, checkInDraftStore = draftStore,
        userRepository = object : UserRepository {
            override fun observeProfile() = MutableStateFlow<UserProfile?>(null)
            override suspend fun getMe(): Result<UserProfile> = Result.failure(IllegalStateException("No public author address"))
            override suspend fun refreshProfile(): Result<UserProfile> = error("unused")
            override suspend fun getPublicAvatarUrl(userId: String): Result<String?> = error("unused")
            override suspend fun requestAccountDeletion(): Result<AccountDeletionRequest> = error("unused")
            override suspend fun getAccountDeletionRequest(): Result<AccountDeletionRequest?> = error("unused")
            override suspend fun updateUsername(username: String): Result<Unit> = error("unused")
            override suspend fun updateAbout(about: String): Result<Unit> = error("unused")
            override suspend fun updateAvatar(photo: PendingPhotoUpload): Result<Unit> = error("unused")
        }, roasterFavorites = object : RoasterFavorites {
            override fun observeFavorites() = flowOf(emptyList<CatalogItem>())
            override suspend fun setFavorite(roaster: CatalogItem, isFavorite: Boolean): Result<Unit> = error("unused")
        },
        onCheckInCreated = onCheckInCreated,
    )
}

private class ShopTestCheckIns : CheckInRepository {
    var create: suspend (CreateCheckInInput) -> Result<Unit> = { error("unexpected creation") }
    var helpful: suspend (String, Boolean) -> Result<CheckInHelpfulVote> = { _, _ -> error("unexpected vote") }
    override suspend fun setHelpful(id: String, helpful: Boolean) = this.helpful(id, helpful)
    override suspend fun createCheckIn(input: CreateCheckInInput): Result<Unit> = create(input)
    override suspend fun getMyCheckIns(page: Int, pageSize: Int): Result<PagedResult<CheckIn>> = error("unused")
    override suspend fun getMyCheckIns(from: String, to: String, pageSize: Int): Result<List<CheckIn>> = error("unused")
    override suspend fun updateCheckIn(id: String, input: UpdateCheckInInput): Result<CheckIn> = error("unused")
    override suspend fun setVisibility(id: String, visibility: CheckInVisibility): Result<CheckIn> = error("unused")
    override suspend fun report(id: String, text: String): Result<Unit> = error("unused")
}
