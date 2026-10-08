package com.coffeepeek.admin.ui.screen.shop

import com.coffeepeek.domain.model.ConsumedDrinkOption
import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.admin.feature.favorites.api.roasterFavoriteId
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.favorites.withFavoriteMembership
import com.coffeepeek.admin.utils.ClipboardHelper
import com.coffeepeek.admin.utils.FavoriteSync
import com.coffeepeek.admin.utils.OpenInBrowser
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.admin.utils.ReviewSync
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.admin.utils.datePickerMillisToUtcIsoInstant
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
)

class ShopDetailViewModel(
    private val shopId: String,
    private val shopRepository: ShopRepository,
    private val favoriteRepository: FavoriteRepository,
    private val checkInRepository: CheckInRepository,
    private val sessionRepository: SessionRepository,
    private val checkInDraftStore: CheckInDraftStore,
    private val userRepository: UserRepository,
    private val observeFavoriteIds: ObserveFavoriteIdsUseCase? = null,
    private val roasterFavorites: RoasterFavorites,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ShopDetailUiState())
    val uiState = _uiState.asStateFlow()
    private val favoriteIds = MutableStateFlow<Set<String>?>(null)
    private val favoriteMutationMutex = Mutex()

    init {
        observeFavoriteIds?.invoke()
            ?.onEach { result ->
                result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
                val ids = result.getOrNull() ?: return@onEach
                favoriteIds.value = ids
                _uiState.update { state ->
                    state.copy(details = state.details?.withFavoriteMembership(ids))
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
        roasterFavorites.observeFavorites()
            .onEach { favorites -> _uiState.update { it.copy(favoriteRoasterIds = favorites.map { it.roasterFavoriteId }.toSet()) } }
            .catch { error ->
                if (error is CancellationException) throw error
                _uiState.update { it.copy(actionMessage = "Не удалось загрузить избранное") }
            }
            .launchIn(workScope)
        load()
    }

    fun load() {
        workScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            refreshDetails(showLoading = true)
        }
    }

    private suspend fun refreshDetails(showLoading: Boolean) {
        val isLoggedIn = sessionRepository.isLoggedIn()
        val currentUserId = if (isLoggedIn) userRepository.getMe().getOrNull()?.address?.slug else null
        shopRepository.getShopDetails(shopId)
            .mapCatching { enrichWithReviewAccess(it) }
            .onSuccess { details ->
                _uiState.update {
                    it.copy(
                        details = favoriteIds.value?.let(details::withFavoriteMembership) ?: details,
                        isLoggedIn = isLoggedIn,
                        currentUserId = currentUserId,
                        isLoading = false,
                    )
                }
            }
            .onFailure { e ->
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
        workScope.launch {
            if (!favoriteMutationMutex.tryLock()) return@launch
            var loadingStarted = false
            try {
                if (!sessionRepository.isLoggedIn()) {
                    Navigator.navigate(Navigator.Screen.Auth)
                    return@launch
                }
                val details = _uiState.value.details ?: return@launch
                val isFavorite = details.shop.isFavorite
                _uiState.update { it.copy(isFavoriteLoading = true) }
                loadingStarted = true
                val result = if (isFavorite) {
                    favoriteRepository.removeFavorite(shopId)
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
                        FavoriteSync.notifyChanged(shopId, newFavoriteState)
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
                        _uiState.update { it.copy(actionMessage = e.message, isFavoriteLoading = false) }
                    }
            } finally {
                if (loadingStarted) _uiState.update { it.copy(isFavoriteLoading = false) }
                favoriteMutationMutex.unlock()
            }
        }
    }

    fun openCheckInSheet() {
        if (_uiState.value.isCheckInLoading || _uiState.value.showCheckInSheet) return
        workScope.launch {
            if (!sessionRepository.isLoggedIn()) {
                Navigator.navigate(Navigator.Screen.Auth)
                return@launch
            }
            loadDrinks()
            val draft = checkInDraftStore.open(shopId)
            _uiState.update {
                it.copy(
                    showCheckInSheet = true,
                    checkInDraft = draft,
                    submittedCheckInVisibility = null,
                    checkInError = null,
                )
            }
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
        _uiState.update {
            it.copy(showCheckInSheet = false, submittedCheckInVisibility = null, checkInError = null,
                checkInDraft = if (it.submittedCheckInVisibility != null) null else it.checkInDraft)
        }
    }

    fun updateCheckInDraft(draft: CheckInDraft) {
        if (draft.shopId != shopId || _uiState.value.isCheckInLoading || _uiState.value.submittedCheckInVisibility != null) return
        checkInDraftStore.save(draft)
        _uiState.update { it.copy(checkInDraft = draft, checkInError = null) }
    }

    fun checkIn(draft: CheckInDraft) {
        if (draft.shopId != shopId) return
        if (_uiState.value.isCheckInLoading || _uiState.value.submittedCheckInVisibility != null) return
        draft.validationError()?.let { error ->
            _uiState.update { it.copy(checkInError = error) }
            return
        }
        val shopSlug = _uiState.value.details?.shop?.publicAddress?.slug?.takeIf(String::isNotBlank)
        if (shopSlug == null) {
            _uiState.update { it.copy(checkInError = "Не удалось определить адрес кофейни") }
            return
        }
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
                        visitedAtIso = datePickerMillisToUtcIsoInstant(draft.visitMillis),
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
        if (!_uiState.value.isLoggedIn) {
            Navigator.navigate(Navigator.Screen.Auth)
            return
        }
        Navigator.navigate(Navigator.Screen.SuggestShopChange(shopId))
    }

    fun toggleRoasterFavorite(roaster: CatalogItem) {
        val id = roaster.roasterFavoriteId
        val current = _uiState.value
        if (id in current.savingRoasterFavoriteIds) return
        _uiState.update { it.copy(savingRoasterFavoriteIds = it.savingRoasterFavoriteIds + id) }
        workScope.launch {
            try {
                if (!sessionRepository.isLoggedIn()) {
                    Navigator.navigate(Navigator.Screen.Auth)
                    return@launch
                }
                roasterFavorites.setFavorite(roaster, id !in current.favoriteRoasterIds)
                    .onFailure { _uiState.update { it.copy(actionMessage = "Не удалось изменить избранное") } }
            } finally {
                _uiState.update { it.copy(savingRoasterFavoriteIds = it.savingRoasterFavoriteIds - id) }
            }
        }
    }

    fun openReportIssue() {
        val details = _uiState.value.details ?: return
        if (!_uiState.value.isLoggedIn) {
            Navigator.navigate(Navigator.Screen.Auth)
            return
        }
        Navigator.navigate(Navigator.Screen.ReportShop(shopId, details.shop.title))
    }

    fun openOnMap() {
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

    fun openRoute() {
        val details = _uiState.value.details ?: return
        val location = details.location
        val lat = location?.latitude
        val lon = location?.longitude
        if (lat == null || lon == null) {
            _uiState.update { it.copy(actionMessage = "Координаты кофейни недоступны") }
            return
        }
        OpenInBrowser.openInBrowser(
            buildYandexMapsRouteUrl(latitude = lat, longitude = lon)
        )
    }

    fun shareShop() {
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
        ClipboardHelper.copyText(phone)
        _uiState.update { it.copy(actionMessage = "Номер скопирован") }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }
}

internal fun CoffeeShopDetails.ownsCheckIn(checkIn: CheckIn, currentUserId: String?): Boolean =
    (currentUserId != null && checkIn.authorAddress?.slug == currentUserId) || userCheckIns.any { it.id == checkIn.id }

internal fun buildYandexMapsRouteUrl(latitude: Double, longitude: Double): String =
    "https://yandex.ru/maps/?mode=routes&rtext=~$latitude,$longitude&rtt=auto"

private fun PickedImage.toPendingUpload() = PendingPhotoUpload(
    fileName = fileName,
    contentType = contentType,
    bytes = bytes,
)
