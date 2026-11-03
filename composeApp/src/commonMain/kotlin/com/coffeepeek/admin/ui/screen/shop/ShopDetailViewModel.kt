package com.coffeepeek.admin.ui.screen.shop

import com.coffeepeek.admin.analytics.*

import com.coffeepeek.domain.model.ConsumedDrinkOption
import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.admin.feature.favorites.api.roasterFavoriteId
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.favorites.withFavoriteMembership
import com.coffeepeek.admin.utils.ClipboardHelper
import com.coffeepeek.admin.utils.FavoriteSync
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.admin.utils.ReviewSync
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.admin.utils.epochMillisToUtcIsoInstant
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.CheckInVisibility
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.CreateCheckInInput
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.coffeepeek.domain.repository.CheckInRepository
import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.feature.favorites.domain.usecase.ObserveFavoriteIdsUseCase
import kotlinx.coroutines.CancellationException
import com.coffeepeek.domain.repository.UserRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.updateAndGet
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility

data class ShopDetailUiState(
    val drinks: List<ConsumedDrinkOption> = emptyList(),
    val drinksError: String? = null,
    val details: CoffeeShopDetails? = null,
    val isLoggedIn: Boolean = false,
    val currentUserId: String? = null,
    val isLoading: Boolean = false,
    val isFavoriteLoading: Boolean = false,
    val isCheckInLoading: Boolean = false,
    val helpfulId: String? = null,
    val showCheckInSheet: Boolean = false,
    val checkInDraft: CheckInDraft? = null,
    val submittedCheckInVisibility: CheckInVisibility? = null,
    val checkInError: String? = null,
    val checkInValidationError: CheckInDraftValidationError? = null,
    val actionMessage: String? = null,
    val actionFeedback: ShopDetailFeedback? = null,
    val loadFeedback: ShopDetailFeedback? = null,
    val checkInFeedback: ShopDetailFeedback? = null,
    val error: String? = null,
    val favoriteRoasterIds: Set<String> = emptySet(),
    val savingRoasterFavoriteIds: Set<String> = emptySet(),
)

class ShopDetailViewModel internal constructor(
    private val shopId: String,
    private val shopRepository: ShopRepository,
    private val favoriteRepository: FavoriteRepository,
    private val checkInRepository: CheckInRepository,
    private val sessionRepository: SessionRepository,
    private val checkInDraftStore: CheckInDraftStore,
    private val userRepository: UserRepository,
    private val observeFavoriteIds: ObserveFavoriteIdsUseCase? = null,
    private val roasterFavorites: RoasterFavorites,
    private val visibility: AuthorVisibility = AuthorVisibility.None,
    private val onCheckInCreated: () -> Unit = {},
) : BaseViewModel() {

    internal val authorVisibility = visibility.visibility
    private var loadJob: Job? = null
    private val requestSequence = MutableStateFlow(0L)
    private fun accountKey() = sessionRepository.peekSession()?.takeIf(sessionRepository::isActiveSession)?.let { it.userId ?: it.accessToken }
    private val _uiState = MutableStateFlow(ShopDetailUiState())
    val uiState = _uiState.asStateFlow()
    private var checkInAnalytics: CheckInTracker? = null

    private val favoriteIds = MutableStateFlow<Set<String>?>(null)
    private val favoriteMembershipMutex = Mutex()
    private val favoriteMutationMutex = Mutex()

    init {
        observeFavoriteIds?.invoke()
            ?.onEach { result ->
                result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
                val ids = result.getOrNull() ?: return@onEach
                favoriteMembershipMutex.withLock {
                    favoriteIds.value = ids
                    _uiState.update { state ->
                        state.copy(details = state.details?.withFavoriteMembership(ids))
                    }
                }
            }
            ?.launchIn(workScope)
        ReviewSync.changes
            .onEach { changedShopId ->
                if (changedShopId == shopId) {
                    _uiState.update { it.copy(actionMessage = "Отзыв отправлен на модерацию") }
                    refreshDetails(showLoading = false)
                }
            }
            .launchIn(workScope)
        roasterFavorites.observeErrors()
            .onEach { error -> if (error != null) _uiState.update { it.copy(actionMessage = error, actionFeedback = null) } }
            .launchIn(workScope)
        roasterFavorites.observeFavorites()
            .onEach { favorites -> _uiState.update { it.copy(favoriteRoasterIds = favorites.map { it.roasterFavoriteId }.toSet()) } }
            .catch { error ->
                if (error is CancellationException) throw error
                _uiState.update { it.copy(actionMessage = null, actionFeedback = ShopDetailFeedback.FavoritesLoadFailed) }
            }
            .launchIn(workScope)
        load()
        workScope.launch {
            combine(sessionRepository.observeSession().map { session -> session?.takeIf(sessionRepository::isActiveSession)?.let { it.userId ?: it.accessToken } }.distinctUntilChanged(), visibility.visibility) { account, snapshot -> account to snapshot.revision }
                .drop(1).collect {
                    requestSequence.update { it + 1 }
                    _uiState.update { state -> state.copy(details = state.details?.copy(checkIns = emptyList(), userCheckIns = emptyList()),
                        isLoggedIn = accountKey() != null, currentUserId = null) }
                    load()
                }
        }
    }

    fun load() {
        loadJob?.cancel()
        loadJob = workScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, loadFeedback = null) }
            refreshDetails(showLoading = true)
        }
    }

    private suspend fun refreshDetails(showLoading: Boolean, isCurrent: () -> Boolean = { true }) {
        if (!isCurrent()) return
        val request = requestSequence.updateAndGet { it + 1 }
        val account = accountKey()
        val revision = visibility.visibility.value.revision
        fun current() = isCurrent() && requestSequence.value == request && accountKey() == account &&
            visibility.visibility.value.revision == revision
        if (!visibility.visibility.value.ready && visibility.refresh().isFailure) {
            currentCoroutineContext().ensureActive()
            if (current()) _uiState.update { it.copy(isLoading = false, error = null, loadFeedback = ShopDetailFeedback.BlocksLoadFailed) }
            return
        }
        val isLoggedIn = sessionRepository.isLoggedIn()
        val currentUserId = if (isLoggedIn) userRepository.getMe().getOrNull()?.address?.slug else null
        val result = try {
            shopRepository.getShopDetails(shopId).map { enrichWithReviewAccess(it) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        result.onSuccess { details ->
            currentCoroutineContext().ensureActive()
            // Membership and details must be committed together: an observer update
            // before details exist can otherwise leave the state equal and evade CAS retries.
            favoriteMembershipMutex.withLock {
                if (!current()) return@withLock
                val visibleDetails = details.copy(checkIns = details.checkIns.filterNot {
                    visibility.visibility.value.hides(it.authorId, it.authorAddress?.slug)
                })
                _uiState.update {
                    it.copy(
                        details = favoriteIds.value?.let(visibleDetails::withFavoriteMembership) ?: visibleDetails,
                        isLoggedIn = isLoggedIn,
                        currentUserId = currentUserId,
                        isLoading = false,
                    )
                }
            }
        }.onFailure { e ->
            currentCoroutineContext().ensureActive()
            if (!current()) return@onFailure
            _uiState.update {
                it.copy(
                    isLoading = if (showLoading) false else it.isLoading,
                    error = if (showLoading) e.message else it.error,
                )
            }
        }
    }

    private suspend fun enrichWithReviewAccess(details: CoffeeShopDetails): CoffeeShopDetails {
        if (!sessionRepository.isLoggedIn()) {
            // userCheckIns are the signed-in user's own — never show them to a logged-out viewer.
            return details.copy(existingReviewId = null, userCheckIns = emptyList())
        }
        return details
    }

    fun toggleFavorite() {
        val details = _uiState.value.details ?: return
        com.coffeepeek.admin.feature.favorites.api.FavoriteInteractions.change(workScope,
            details.shop.publicAddress?.slug ?: details.shop.id, false, !details.shop.isFavorite,
            action = { mutateFavorite() })
    }

    private suspend fun mutateFavorite(): Result<Unit> {
        if (!favoriteMutationMutex.tryLock()) return Result.failure(IllegalStateException("Favorite action is unavailable"))
        var loadingStarted = false
        return try {
            if (!sessionRepository.isLoggedIn()) {
                Navigator.navigate(Navigator.Screen.Auth)
                return Result.failure(IllegalStateException("Favorite action is unavailable"))
            }
            val details = _uiState.value.details ?: return Result.failure(IllegalStateException("Favorite action is unavailable"))
            val isFavorite = details.shop.isFavorite
            _uiState.update { it.copy(isFavoriteLoading = true) }
            loadingStarted = true
            val result = if (isFavorite) {
                favoriteRepository.removeFavorite(details.shop.publicAddress?.slug ?: details.shop.id)
            } else {
                favoriteRepository.addFavorite(
                    shop = details.shop,
                    address = details.location?.address ?: details.shop.address,
                )
            }
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            result
                .onSuccess {
                    val newFavoriteState = !isFavorite
                    FavoriteSync.notifyChanged(details.shop.publicAddress?.slug ?: details.shop.id, newFavoriteState)
                    _uiState.update { state ->
                        val current = state.details ?: return@update state
                        state.copy(
                            details = current.copy(
                                shop = current.shop.copy(isFavorite = newFavoriteState),
                            ),
                            isFavoriteLoading = false,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(actionMessage = e.message, actionFeedback = null, isFavoriteLoading = false) }
                }
        } finally {
            if (loadingStarted) _uiState.update { it.copy(isFavoriteLoading = false) }
            favoriteMutationMutex.unlock()
        }
    }

    fun openCheckInSheet(prepareLegacyDraft: Boolean = true) {
        if (_uiState.value.isCheckInLoading || _uiState.value.showCheckInSheet) return
        val handedOff = com.coffeepeek.admin.feature.checkin.api.CheckInFlowHandoff.take()
        if (handedOff == null) {
            Analytics.shopAction("check_in")
            Analytics.track(AnalyticsEvent.CHECK_IN_START_ATTEMPTED, "source" to "shop_detail")
        }
        workScope.launch {
            if (!sessionRepository.isLoggedIn()) {
                Analytics.gate("check_in")
                Navigator.navigate(Navigator.Screen.Auth)
                return@launch
            }
            checkInAnalytics = (handedOff ?: CheckInTracker(CheckInKind.Visit, CheckInSource.Shop)).also { it.step(CheckInStep.Editor) }
            if (prepareLegacyDraft) loadDrinks()
            val draft = if (prepareLegacyDraft) checkInDraftStore.open(shopId) else null
            _uiState.update {
                it.copy(
                    checkInDraft = draft,
                    submittedCheckInVisibility = null,
                    checkInError = null, checkInFeedback = null, checkInValidationError = null,
                )
            }
            _uiState.update { it.copy(showCheckInSheet = true) }
        }
    }

    /** Temporary app-owned bridge; the new form owns its draft and write result. */
    fun refreshAfterCheckIn() {
        val userId = sessionRepository.peekSession()?.userId ?: return
        val isCurrent = {
            val session = sessionRepository.peekSession()
            session?.userId == userId && sessionRepository.isActiveSession(session)
        }
        if (!isCurrent()) return
        _uiState.update { it.copy(details = it.details?.copy(isVisited = true)) }
        workScope.launch { refreshDetails(showLoading = false, isCurrent = isCurrent) }
    }

    fun loadDrinks() {
        workScope.launch {
            shopRepository.getConsumedDrinks().onSuccess { drinks ->
                _uiState.update { it.copy(drinks = drinks, drinksError = null) }
            }.onFailure { e -> _uiState.update { it.copy(drinksError = e.message ?: "Не удалось загрузить напитки") } }
        }
    }

    fun dismissCheckInSheet() {
        if (_uiState.value.isCheckInLoading) return
        checkInAnalytics?.cancel("close")
        _uiState.update {
            it.copy(showCheckInSheet = false, submittedCheckInVisibility = null, checkInError = null, checkInFeedback = null, checkInValidationError = null,
                checkInDraft = if (it.submittedCheckInVisibility != null) null else it.checkInDraft)
        }
    }

    fun updateCheckInDraft(draft: CheckInDraft) {
        if (draft.shopId != shopId || _uiState.value.isCheckInLoading || _uiState.value.submittedCheckInVisibility != null) return
        val before = _uiState.value.checkInDraft
        if (before != null) listOf(
            "note" to (before.note != draft.note), "date" to (before.visitMillis != draft.visitMillis),
            "drink" to (before.drinkSlug != draft.drinkSlug || before.customDrinkName != draft.customDrinkName),
            "rating_coffee" to (before.coffeeRating != draft.coffeeRating), "rating_service" to (before.serviceRating != draft.serviceRating),
            "rating_place" to (before.placeRating != draft.placeRating), "photos" to (before.photos != draft.photos), "visibility" to (before.isPublic != draft.isPublic),
        ).filter { it.second }.forEach { checkInAnalytics?.field(it.first) }
        checkInDraftStore.save(draft)
        _uiState.update { it.copy(checkInDraft = draft, checkInError = null, checkInFeedback = null, checkInValidationError = null) }
    }

    fun checkIn(submittedDraft: CheckInDraft) {
        if (submittedDraft.shopId != shopId) return
        if (_uiState.value.isCheckInLoading || _uiState.value.submittedCheckInVisibility != null) return
        val tracker = checkInAnalytics
        submittedDraft.validationError()?.let { error ->
            tracker?.submit()
            tracker?.validation(listOf("form"))
            _uiState.update { it.copy(checkInError = null, checkInFeedback = null, checkInValidationError = error) }
            return
        }
        val shopSlug = _uiState.value.details?.shop?.publicAddress?.slug?.takeIf(String::isNotBlank)
        if (shopSlug == null) {
            tracker?.submit()
            _uiState.update { it.copy(checkInError = null, checkInFeedback = ShopDetailFeedback.AddressMissing) }
            return
        }
        val draft = submittedDraft
        tracker?.submit()
        checkInDraftStore.save(draft)
        _uiState.update { it.copy(isCheckInLoading = true, checkInDraft = draft, checkInError = null, checkInFeedback = null, checkInValidationError = null) }
        val visibility = if (draft.isPublic) CheckInVisibility.Public else CheckInVisibility.Private
        workScope.launch {
            val result = try {
                if (!sessionRepository.isLoggedIn()) {
                    _uiState.update { it.copy(isCheckInLoading = false, checkInError = null, checkInFeedback = ShopDetailFeedback.SignInRequired) }
                    Navigator.navigate(Navigator.Screen.Auth)
                    return@launch
                }
                checkInRepository.createCheckIn(
                    CreateCheckInInput(
                        shopSlug = shopSlug,
                        drinkSlug = draft.drinkSlug,
                        customDrinkName = draft.customDrinkName?.trim(),
                        text = draft.note.trim(),
                        visitedAtIso = epochMillisToUtcIsoInstant(draft.visitMillis),
                        visibility = visibility,
                        rating = com.coffeepeek.domain.model.ReviewRating(
                            place = draft.placeRating, service = draft.serviceRating, coffee = draft.coffeeRating,
                        ),
                        photos = draft.photos.map { it.toPendingUpload() },
                    ),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Result.failure(error)
            }
            result.onSuccess {
                tracker?.saved(visibility.name, draft.photos.isNotEmpty())
                checkInDraftStore.clear(shopId)
                _uiState.update { state ->
                    val current = state.details
                    state.copy(
                        details = current?.copy(isVisited = true),
                        isCheckInLoading = false,
                        submittedCheckInVisibility = visibility,
                    )
                }
                onCheckInCreated()
                refreshDetails(showLoading = false)
            }.onFailure { e ->
                val failure = safeAnalyticsError(e)
                if (failure.status == 400 || failure.status == 422) tracker?.validation(listOf("form"), server = true)
                else tracker?.failed(if ((e as? com.coffeepeek.domain.model.CheckInOperationFailure)?.stage == com.coffeepeek.domain.model.CheckInFailureStage.PhotoUpload) CheckInStage.Photo else CheckInStage.Save, failure)
                _uiState.update {
                    it.copy(
                        checkInError = e.message?.takeIf(String::isNotBlank),
                        checkInFeedback = ShopDetailFeedback.CheckInSubmitFailed.takeIf { e.message.isNullOrBlank() },
                        isCheckInLoading = false,
                    )
                }
            }
        }
    }

    fun toggleHelpful(checkInId: String) {
        val current = _uiState.value
        val details = current.details ?: return
        val checkIn = details.checkIns.firstOrNull { it.id == checkInId } ?: return
        if (current.helpfulId != null) return
        _uiState.update { it.copy(helpfulId = checkInId) }
        workScope.launch {
            try {
                if (!sessionRepository.isLoggedIn()) {
                    Navigator.navigate(Navigator.Screen.Auth)
                    return@launch
                }
                checkInRepository.setHelpful(checkInId, helpful = !checkIn.isHelpfulByCurrentUser)
                    .onSuccess { vote ->
                        _uiState.update { state ->
                            val currentDetails = state.details ?: return@update state
                            state.copy(
                                details = currentDetails.copy(
                                    checkIns = currentDetails.checkIns.map { item ->
                                        if (item.id == checkInId) item.copy(
                                            isHelpfulByCurrentUser = vote.isHelpful,
                                            helpfulCount = vote.helpfulCount,
                                        ) else item
                                    },
                                ),
                            )
                        }
                    }
                    .onFailure { e -> _uiState.update { it.copy(actionMessage = e.message, actionFeedback = null) } }
            } finally {
                _uiState.update { it.copy(helpfulId = null) }
            }
        }
    }

    fun openSuggestChange() {
        Analytics.shopAction("edit")
        if (!_uiState.value.isLoggedIn) {
            Analytics.gate("edit_shop")
            Navigator.navigate(Navigator.Screen.Auth)
            return
        }
        Navigator.navigate(Navigator.Screen.SuggestShopChange(shopId))
    }

    fun toggleRoasterFavorite(roaster: CatalogItem) {
        val id = roaster.roasterFavoriteId
        val desired = id !in _uiState.value.favoriteRoasterIds
        com.coffeepeek.admin.feature.favorites.api.FavoriteInteractions.change(workScope, id, true, desired,
            action = { roasterFavorites.setFavorite(roaster, desired) },
            completed = { result -> result.onFailure { _uiState.update { it.copy(actionMessage = null, actionFeedback = ShopDetailFeedback.FavoriteChangeFailed) } } },
        )
    }

    fun openReportIssue() {
        Analytics.shopAction("report_issue")
        val details = _uiState.value.details ?: return
        if (!_uiState.value.isLoggedIn) {
            Analytics.gate("report_issue")
            Navigator.navigate(Navigator.Screen.Auth)
            return
        }
        Navigator.navigate(Navigator.Screen.ReportShop(shopId, details.shop.title))
    }

    fun openOnMap() {
        Analytics.shopAction("directions")
        checkInAnalytics?.cancel("map")
        val details = _uiState.value.details ?: return
        val location = details.location ?: return
        val lat = location.latitude ?: return
        val lon = location.longitude ?: return
        Navigator.openShopOnMap(
            shopId = shopId,
            latitude = lat,
            longitude = lon,
            title = details.shop.title,
        )
    }

    internal fun shareShop(labels: ShopShareLabels) {
        Analytics.shopAction("share", "shop_header")
        val title = _uiState.value.details?.shop?.title?.takeIf { it.isNotBlank() }
        val path = _uiState.value.details?.shop?.publicAddress?.canonicalPath ?: return
        val shareUrl = "https://coffeepeek.by$path"
        ShareHelper.shareText(labels.text(title, shareUrl))
    }

    fun copyPhone(phone: String) {
        Analytics.shopAction("phone_copy", "shop_contacts")
        ClipboardHelper.copyText(phone)
        _uiState.update { it.copy(actionMessage = null, actionFeedback = ShopDetailFeedback.PhoneCopied) }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null, actionFeedback = null) }
    }
}

internal fun CoffeeShopDetails.ownsCheckIn(checkIn: CheckIn, currentUserId: String?): Boolean =
    (currentUserId != null && checkIn.authorAddress?.slug == currentUserId) || userCheckIns.any { it.id == checkIn.id }

private fun PickedImage.toPendingUpload() = PendingPhotoUpload(
    fileName = fileName,
    contentType = contentType,
    bytes = bytes,
)
