package com.coffeepeek.admin.ui.screen.shop

import com.coffeepeek.domain.model.validateConsumedDrink

import com.coffeepeek.domain.model.ConsumedDrinkOption

import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.favorites.withFavoriteMembership
import com.coffeepeek.admin.utils.ClipboardHelper
import com.coffeepeek.admin.utils.FavoriteSync
import com.coffeepeek.admin.utils.OpenInBrowser
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.admin.utils.ReviewSync
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.admin.utils.datePickerMillisToUtcIsoInstant
import com.coffeepeek.admin.utils.validatePublicCheckInDescription
import com.coffeepeek.admin.utils.validatePublicCheckInHeader
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.model.CreateCheckInInput
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.coffeepeek.domain.repository.CheckInRepository
import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.domain.repository.ReviewRepository
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.feature.favorites.domain.ObserveFavoriteIdsUseCase
import kotlinx.coroutines.CancellationException
import com.coffeepeek.domain.repository.UserRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShopDetailUiState(
    val drinks: List<ConsumedDrinkOption> = emptyList(),
    val drinksError: String? = null,
    val details: CoffeeShopDetails? = null,
    val isLoggedIn: Boolean = false,
    val currentUserId: String? = null,
    val isLoading: Boolean = false,
    val isFavoriteLoading: Boolean = false,
    val isCheckInLoading: Boolean = false,
    val showCheckInSheet: Boolean = false,
    val checkInDraft: CheckInDraft? = null,
    val showReviewSheet: Boolean = false,
    val editingReviewId: String? = null,
    val actionMessage: String? = null,
    val error: String? = null,
)

class ShopDetailViewModel(
    private val shopId: String,
    private val shopRepository: ShopRepository,
    private val favoriteRepository: FavoriteRepository,
    private val checkInRepository: CheckInRepository,
    private val reviewRepository: ReviewRepository,
    private val sessionRepository: SessionRepository,
    private val checkInDraftStore: CheckInDraftStore,
    private val userRepository: UserRepository,
    private val observeFavoriteIds: ObserveFavoriteIdsUseCase? = null,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ShopDetailUiState())
    val uiState = _uiState.asStateFlow()
    private val favoriteIds = MutableStateFlow<Set<String>?>(null)

    init {
        load()
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
            if (!sessionRepository.isLoggedIn()) {
                Navigator.navigate(Navigator.Screen.Auth)
                return@launch
            }
            val details = _uiState.value.details ?: return@launch
            val isFavorite = details.shop.isFavorite
            _uiState.update { it.copy(isFavoriteLoading = true) }
            val result = if (isFavorite) {
                favoriteRepository.removeFavorite(shopId)
            } else {
                favoriteRepository.addFavorite(
                    shop = details.shop,
                    address = details.location?.address ?: details.shop.address,
                )
            }
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
        }
    }

    fun openCheckInSheet() {
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
                    showReviewSheet = false,
                    editingReviewId = null,
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
        _uiState.update { it.copy(showCheckInSheet = false) }
    }

    fun updateCheckInDraft(draft: CheckInDraft) {
        if (draft.shopId != shopId) return
        checkInDraftStore.save(draft)
        _uiState.update { it.copy(checkInDraft = draft) }
    }

    fun checkIn(draft: CheckInDraft) {
        if (draft.shopId != shopId) return
        val drinkError = validateConsumedDrink(draft.drinkSlug, draft.customDrinkName)
        if (drinkError != null) { _uiState.update { it.copy(actionMessage = drinkError) }; return }
        if (_uiState.value.isCheckInLoading) return
        if (draft.isPublic && (
                validatePublicCheckInHeader(draft.header) != null ||
                    validatePublicCheckInDescription(draft.note) != null
                )
        ) {
            _uiState.update {
                it.copy(actionMessage = "Для публичного чек-ина нужны заголовок и описание")
            }
            return
        }
        workScope.launch {
            _uiState.update { it.copy(isCheckInLoading = true) }
            checkInRepository.createCheckIn(
                CreateCheckInInput(
                    shopId = shopId,
                    drinkSlug = draft.drinkSlug,
                    customDrinkName = draft.customDrinkName?.trim(),
                    header = draft.header.trim().takeIf { draft.isPublic },
                    note = draft.note.trim().takeIf { it.isNotEmpty() },
                    visitedAtIso = datePickerMillisToUtcIsoInstant(draft.visitMillis),
                    isPublic = draft.isPublic,
                    placeRating = draft.placeRating,
                    serviceRating = draft.serviceRating,
                    coffeeRating = draft.coffeeRating,
                    photos = draft.photos.map { it.toPendingUpload() },
                ),
            ).onSuccess {
                checkInDraftStore.clear(shopId)
                _uiState.update { state ->
                    val current = state.details
                    state.copy(
                        details = current?.copy(isVisited = true),
                        isCheckInLoading = false,
                        showCheckInSheet = false,
                        checkInDraft = null,
                    )
                }
                refreshDetails(showLoading = false)
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        actionMessage = e.message,
                        isCheckInLoading = false,
                    )
                }
            }
        }
    }

    fun openCreateReview() {
        workScope.launch {
            if (!sessionRepository.isLoggedIn()) {
                Navigator.navigate(Navigator.Screen.Auth)
                return@launch
            }
            if (!_uiState.value.details?.existingReviewId.isNullOrBlank()) {
                _uiState.update { it.copy(actionMessage = "Вы уже оставляли отзыв об этом месте") }
                return@launch
            }
            _uiState.update {
                it.copy(
                    showReviewSheet = true,
                    editingReviewId = null,
                    showCheckInSheet = false,
                )
            }
        }
    }

    fun openReviewAction() {
        workScope.launch {
            if (!sessionRepository.isLoggedIn()) {
                Navigator.navigate(Navigator.Screen.Auth)
                return@launch
            }
            val existingId = _uiState.value.details?.existingReviewId
            _uiState.update {
                it.copy(
                    showReviewSheet = true,
                    editingReviewId = existingId?.takeIf(String::isNotBlank),
                    showCheckInSheet = false,
                )
            }
        }
    }

    fun openEditReview(reviewId: String) {
        _uiState.update {
            it.copy(
                showReviewSheet = true,
                editingReviewId = reviewId,
                showCheckInSheet = false,
            )
        }
    }

    fun dismissReviewSheet() {
        _uiState.update {
            it.copy(showReviewSheet = false, editingReviewId = null)
        }
    }

    fun toggleHelpful(reviewId: String) {
        workScope.launch {
            if (!sessionRepository.isLoggedIn()) {
                Navigator.navigate(Navigator.Screen.Auth)
                return@launch
            }
            val review = _uiState.value.details?.reviews?.firstOrNull { it.id == reviewId } ?: return@launch
            reviewRepository.setReviewHelpful(reviewId, helpful = !review.isHelpfulByCurrentUser)
                .onSuccess { vote ->
                    _uiState.update { state ->
                        val current = state.details ?: return@update state
                        state.copy(
                            details = current.copy(
                                reviews = current.reviews.map { r ->
                                    if (r.id == reviewId) {
                                        r.copy(
                                            isHelpfulByCurrentUser = vote.isHelpful,
                                            helpfulCount = vote.helpfulCount,
                                        )
                                    } else {
                                        r
                                    }
                                },
                            ),
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(actionMessage = e.message) } }
        }
    }

    fun openSuggestChange() {
        if (!_uiState.value.isLoggedIn) {
            Navigator.navigate(Navigator.Screen.Auth)
            return
        }
        Navigator.navigate(Navigator.Screen.SuggestShopChange(shopId))
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

internal fun buildYandexMapsRouteUrl(latitude: Double, longitude: Double): String =
    "https://yandex.ru/maps/?mode=routes&rtext=~$latitude,$longitude&rtt=auto"

private fun PickedImage.toPendingUpload() = PendingPhotoUpload(
    fileName = fileName,
    contentType = contentType,
    bytes = bytes,
)
