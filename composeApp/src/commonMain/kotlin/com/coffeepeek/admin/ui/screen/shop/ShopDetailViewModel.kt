package com.coffeepeek.admin.ui.screen.shop

import com.coffeepeek.admin.analytics.*
import com.coffeepeek.admin.feature.checkin.api.DrinkPhotoPreparation
import com.coffeepeek.admin.feature.checkin.api.DrinkPhotoState

import com.coffeepeek.domain.model.ConsumedDrinkOption
import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.admin.feature.favorites.api.roasterFavoriteId
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.ClipboardHelper
import com.coffeepeek.admin.utils.FavoriteSync
import com.coffeepeek.admin.utils.PickedImage
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
import com.coffeepeek.domain.repository.UserRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
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
    val actionMessage: String? = null,
    val error: String? = null,
    val favoriteRoasterIds: Set<String> = emptySet(),
    val savingRoasterFavoriteIds: Set<String> = emptySet(),
    val drinkPhotoState: DrinkPhotoState = DrinkPhotoState(),
)

class ShopDetailViewModel internal constructor(
    private val shopId: String,
    private val shopRepository: ShopRepository,
    private val favoriteRepository: FavoriteRepository,
    private val checkInRepository: CheckInRepository,
    private val sessionRepository: SessionRepository,
    private val checkInDraftStore: CheckInDraftStore,
    private val userRepository: UserRepository,
    private val roasterFavorites: RoasterFavorites,
    private val drinkPhotoFactory: DrinkPhotoPreparation.Factory = DrinkPhotoPreparation.Factory.Unavailable,
    private val visibility: AuthorVisibility = AuthorVisibility.None,
) : BaseViewModel() {

    internal val authorVisibility = visibility.visibility
    private var loadJob: Job? = null
    private val requestSequence = MutableStateFlow(0L)
    private fun accountKey() = sessionRepository.peekSession()?.takeIf(sessionRepository::isActiveSession)?.let { it.userId ?: it.accessToken }
    private val _uiState = MutableStateFlow(ShopDetailUiState())
    val uiState = _uiState.asStateFlow()
    private val drinkPhotos = drinkPhotoFactory.create(workScope, ::onDrinkPhotoChanged)

    private fun onDrinkPhotoChanged(photoState: DrinkPhotoState) {
        _uiState.update { state ->
            val draft = state.checkInDraft
            if (photoState !== drinkPhotos.state || draft == null || draft.photos.firstOrNull()?.toPendingUpload() != photoState.source) state
            else {
                val updated = draft.copy(drinkPhoto = photoState.selected?.let { PickedImage(it.bytes, it.fileName, it.contentType) },
                    drinkPhotoSkipped = photoState.skipped)
                state.copy(checkInDraft = updated, drinkPhotoState = photoState)
            }
        }
        _uiState.value.checkInDraft?.let(checkInDraftStore::save)
    }
    private var checkInAnalytics: CheckInTracker? = null

    init {
        roasterFavorites.observeErrors()
            .onEach { error -> if (error != null) _uiState.update { it.copy(actionMessage = error) } }
            .launchIn(workScope)
        roasterFavorites.observeFavorites()
            .onEach { favorites -> _uiState.update { it.copy(favoriteRoasterIds = favorites.map { it.roasterFavoriteId }.toSet()) } }
            .catch { _uiState.update { it.copy(actionMessage = "Не удалось загрузить избранное") } }
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
            _uiState.update { it.copy(isLoading = true, error = null) }
            refreshDetails(showLoading = true)
        }
    }

    private suspend fun refreshDetails(showLoading: Boolean) {
        val request = requestSequence.updateAndGet { it + 1 }
        val account = accountKey()
        val revision = visibility.visibility.value.revision
        fun current() = requestSequence.value == request && accountKey() == account && visibility.visibility.value.revision == revision
        if (!visibility.visibility.value.ready && visibility.refresh().isFailure) {
            if (current()) _uiState.update { it.copy(isLoading = false, error = "Не удалось загрузить блокировки. Повторите попытку.") }
            return
        }
        val isLoggedIn = sessionRepository.isLoggedIn()
        val currentUserId = if (isLoggedIn) userRepository.getMe().getOrNull()?.address?.slug else null
        shopRepository.getShopDetails(shopId)
            .mapCatching { enrichWithReviewAccess(it) }
            .onSuccess { details ->
                currentCoroutineContext().ensureActive()
                if (!current()) return@onSuccess
                _uiState.update {
                    it.copy(
                        details = details.copy(checkIns = details.checkIns.filterNot { visibility.visibility.value.hides(it.authorId, it.authorAddress?.slug) }),
                        isLoggedIn = isLoggedIn,
                        currentUserId = currentUserId,
                        isLoading = false,
                    )
                }
            }
            .onFailure { e ->
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
        val favoriteId = details.shop.publicAddress?.slug ?: details.shop.id
        val desired = !details.shop.isFavorite
        com.coffeepeek.admin.feature.favorites.api.FavoriteInteractions.change(workScope, favoriteId, false, desired,
            action = { if (desired) favoriteRepository.addFavorite(details.shop, details.location?.address ?: details.shop.address) else favoriteRepository.removeFavorite(favoriteId) },
            completed = { result -> result.onSuccess {
                FavoriteSync.notifyChanged(favoriteId, desired)
                _uiState.update { state -> state.copy(details = state.details?.let { it.copy(shop = it.shop.copy(isFavorite = desired)) }) }
            }.onFailure { error -> _uiState.update { it.copy(actionMessage = error.message) } } },
        )
    }

    fun openCheckInSheet() {
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
            loadDrinks()
            val draft = checkInDraftStore.open(shopId)
            _uiState.update {
                it.copy(
                    checkInDraft = draft,
                    submittedCheckInVisibility = null,
                    checkInError = null,
                )
            }
            drinkPhotos.setSource(draft.photos.firstOrNull()?.toPendingUpload(), draft.drinkPhoto?.toPendingUpload(), draft.drinkPhotoSkipped)
            // Editing may start as soon as the sheet is visible; initialize the photo source first.
            _uiState.update { it.copy(showCheckInSheet = true) }
        }
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
            it.copy(showCheckInSheet = false, submittedCheckInVisibility = null, checkInError = null,
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
        val sourceChanged = before?.photos?.firstOrNull() != draft.photos.firstOrNull()
        val next = if (sourceChanged) draft.copy(drinkPhoto = null, drinkPhotoSkipped = false)
        else draft.copy(drinkPhoto = drinkPhotos.state.selected?.let { PickedImage(it.bytes, it.fileName, it.contentType) },
            drinkPhotoSkipped = drinkPhotos.state.skipped)
        checkInDraftStore.save(next)
        _uiState.update { it.copy(checkInDraft = next, checkInError = null) }
        drinkPhotos.setSource(next.photos.firstOrNull()?.toPendingUpload(), next.drinkPhoto?.toPendingUpload(), next.drinkPhotoSkipped)
    }

    fun selectDrinkPhoto(photo: PendingPhotoUpload) { if (!_uiState.value.isCheckInLoading) drinkPhotos.select(photo) }
    fun retryDrinkPhoto() { if (!_uiState.value.isCheckInLoading) drinkPhotos.retry() }
    fun skipDrinkPhoto() { if (!_uiState.value.isCheckInLoading) drinkPhotos.skip() }

    fun checkIn(submittedDraft: CheckInDraft) {
        if (submittedDraft.shopId != shopId) return
        if (_uiState.value.isCheckInLoading || _uiState.value.submittedCheckInVisibility != null) return
        val tracker = checkInAnalytics
        // Reject invalid input before starting or waiting for optional image processing.
        submittedDraft.validationError()?.let { error ->
            tracker?.submit()
            tracker?.validation(listOf("form"))
            _uiState.update { it.copy(checkInError = error) }
            return
        }
        val shopSlug = _uiState.value.details?.shop?.publicAddress?.slug?.takeIf(String::isNotBlank)
        if (shopSlug == null) {
            tracker?.submit()
            _uiState.update { it.copy(checkInError = "Не удалось определить адрес кофейни") }
            return
        }
        val photoState = drinkPhotos.state
        if (!photoState.canSubmit) return
        if (submittedDraft.photos.firstOrNull()?.toPendingUpload() != photoState.source) {
            updateCheckInDraft(submittedDraft)
            return
        }
        // A button may still hold the preceding frame's draft when native processing completes.
        val draft = submittedDraft.copy(drinkPhoto = photoState.selected?.let { PickedImage(it.bytes, it.fileName, it.contentType) },
            drinkPhotoSkipped = photoState.skipped)
        tracker?.submit()
        checkInDraftStore.save(draft)
        _uiState.update { it.copy(isCheckInLoading = true, checkInDraft = draft, checkInError = null) }
        val visibility = if (draft.isPublic) CheckInVisibility.Public else CheckInVisibility.Private
        workScope.launch {
            val result = try {
                if (!sessionRepository.isLoggedIn()) {
                    _uiState.update { it.copy(isCheckInLoading = false, checkInError = "Войдите в аккаунт, чтобы создать чекин") }
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
                        drinkPhoto = draft.drinkPhoto?.toPendingUpload(),
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
                refreshDetails(showLoading = false)
            }.onFailure { e ->
                val failure = safeAnalyticsError(e)
                if (failure.status == 400 || failure.status == 422) tracker?.validation(listOf("form"), server = true)
                else tracker?.failed(if ((e as? com.coffeepeek.domain.model.CheckInOperationFailure)?.stage == com.coffeepeek.domain.model.CheckInFailureStage.PhotoUpload) CheckInStage.Photo else CheckInStage.Save, failure)
                _uiState.update {
                    it.copy(
                        checkInError = e.message?.takeIf(String::isNotBlank) ?: "Не удалось отправить чекин. Попробуйте ещё раз",
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
        if (current.helpfulId != null || details.ownsCheckIn(checkIn, current.currentUserId)) return
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
                    .onFailure { e -> _uiState.update { it.copy(actionMessage = e.message) } }
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
            completed = { result -> result.onFailure { _uiState.update { it.copy(actionMessage = "Не удалось изменить избранное") } } },
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

    fun shareShop() {
        Analytics.shopAction("share", "shop_header")
        val title = _uiState.value.details?.shop?.title?.takeIf { it.isNotBlank() }
        val path = _uiState.value.details?.shop?.publicAddress?.canonicalPath ?: return
        val shareUrl = "https://coffeepeek.by$path"
        val text = if (title != null) {
            "Нашёл кофейню «$title» в CoffeePeek — загляни: $shareUrl"
        } else {
            "Нашёл кофейню в CoffeePeek — загляни: $shareUrl"
        }
        ShareHelper.shareText(text)
    }

    fun copyPhone(phone: String) {
        Analytics.shopAction("phone_copy", "shop_contacts")
        ClipboardHelper.copyText(phone)
        _uiState.update { it.copy(actionMessage = "Номер скопирован") }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }
}

internal fun CoffeeShopDetails.ownsCheckIn(checkIn: CheckIn, currentUserId: String?): Boolean =
    (currentUserId != null && checkIn.authorAddress?.slug == currentUserId) || userCheckIns.any { it.id == checkIn.id }

private fun PickedImage.toPendingUpload() = PendingPhotoUpload(
    fileName = fileName,
    contentType = contentType,
    bytes = bytes,
)
