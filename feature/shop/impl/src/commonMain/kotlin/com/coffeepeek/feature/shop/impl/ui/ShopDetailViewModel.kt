package com.coffeepeek.feature.shop.impl.ui

import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.shop.domain.model.ShopViewer
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import com.coffeepeek.feature.shop.domain.repository.ShopReviewVoteRepository
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailEvent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailState
import com.coffeepeek.feature.shop.impl.ui.data.toFavoriteSnapshot
import kotlinx.coroutines.CancellationException

internal class ShopDetailViewModel(
    private val shopId: String,
    private val detailsRepository: ShopDetailsRepository,
    private val voteRepository: ShopReviewVoteRepository,
    private val favoritesRepository: FavoritesRepository,
    private val currentViewer: suspend () -> Result<ShopViewer>,
    private val currentDayOfWeek: () -> Int,
) : MviViewModel<ShopDetailState, ShopDetailAction, ShopDetailEvent>(ShopDetailState()) {
    private var loadInProgress = false
    private val votesInProgress = mutableSetOf<String>()
    private var favoriteMutationInProgress = false

    init { onAction(ShopDetailAction.Retry) }

    override suspend fun handleActionInternal(action: ShopDetailAction) {
        when (action) {
            ShopDetailAction.Retry -> load()
            ShopDetailAction.Back -> sendEvent(ShopDetailEvent.Back)
            ShopDetailAction.OpenMap -> {
                val overview = currentState.details?.overview ?: return
                val latitude = overview.latitude ?: return
                val longitude = overview.longitude ?: return
                sendEvent(ShopDetailEvent.OpenMap(latitude, longitude))
            }
            ShopDetailAction.OpenMenuGallery -> {
                if (currentState.details?.menu?.photos?.isNotEmpty() == true) {
                    sendEvent(ShopDetailEvent.OpenMenuGallery)
                }
            }
            ShopDetailAction.ToggleSchedule -> updateState { copy(scheduleExpanded = !scheduleExpanded) }
            ShopDetailAction.ToggleFeatures -> updateState { copy(featuresExpanded = !featuresExpanded) }
            ShopDetailAction.SignIn -> sendEvent(ShopDetailEvent.SignIn)
            ShopDetailAction.Register -> sendEvent(ShopDetailEvent.Register)
            ShopDetailAction.ToggleFavorite -> toggleFavorite()
            is ShopDetailAction.OpenPhoto -> {
                if (action.index in action.urls.indices) {
                    sendEvent(ShopDetailEvent.OpenPhoto(action.urls, action.index))
                }
            }
            is ShopDetailAction.OpenRoaster -> {
                if (currentState.details?.coffee?.roasters?.any { it.id == action.id } == true) {
                    sendEvent(ShopDetailEvent.OpenRoaster(action.id))
                }
            }
            is ShopDetailAction.OpenLink -> {
                if (action.target.startsWith("https://") || action.target.startsWith("http://") ||
                    action.target.startsWith("mailto:") || action.target.startsWith("tel:")) {
                    sendEvent(ShopDetailEvent.OpenLink(action.target))
                }
            }
            is ShopDetailAction.CopyPhone -> {
                if (action.number == currentState.details?.contact?.phone) {
                    sendEvent(ShopDetailEvent.CopyPhone(action.number))
                }
            }
            is ShopDetailAction.VoteHelpful -> vote(action.reviewId)
        }
    }

    private suspend fun load() {
        if (loadInProgress) return
        loadInProgress = true
        updateState { copy(isLoading = true, hasError = false) }
        try {
            val viewer = currentViewer().getOrThrow()
            val details = detailsRepository.getDetails(shopId).getOrThrow()
            val favoritesResult = favoritesRepository.read()
            favoritesResult.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            val favorites = favoritesResult.getOrNull()
            val dayOfWeek = currentDayOfWeek()
            updateState {
                copy(details = details, isLoggedIn = viewer.isLoggedIn,
                    currentUserId = viewer.userId, todayDayOfWeek = dayOfWeek,
                    isFavorite = favorites?.any { it.id == details.overview.id } == true,
                    favoriteAvailable = favorites != null, hasError = false)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            updateState { copy(details = null, hasError = true) }
        } finally {
            loadInProgress = false
            updateState { copy(isLoading = false) }
        }
    }

    private suspend fun toggleFavorite() {
        if (!currentState.isLoggedIn) {
            sendEvent(ShopDetailEvent.SignIn)
            return
        }
        if (!currentState.favoriteAvailable || favoriteMutationInProgress) return
        val details = currentState.details ?: return
        favoriteMutationInProgress = true
        updateState { copy(isFavoriteLoading = true) }
        try {
            val newValue = !currentState.isFavorite
            val result = if (newValue) favoritesRepository.save(details.toFavoriteSnapshot())
                else favoritesRepository.remove(details.overview.id)
            result.getOrThrow()
            updateState { copy(isFavorite = newValue) }
            sendEvent(ShopDetailEvent.FavoriteChanged(details.overview.id, newValue))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            sendEvent(ShopDetailEvent.FavoriteFailed)
        } finally {
            favoriteMutationInProgress = false
            updateState { copy(isFavoriteLoading = false) }
        }
    }

    private suspend fun vote(reviewId: String) {
        if (!currentState.isLoggedIn) {
            sendEvent(ShopDetailEvent.SignIn)
            return
        }
        val review = currentState.details?.reviews?.firstOrNull { it.id == reviewId } ?: return
        if (review.userId == currentState.currentUserId || !votesInProgress.add(reviewId)) return
        updateState { copy(pendingVoteIds = pendingVoteIds + reviewId) }
        try {
            val vote = voteRepository.setHelpful(reviewId, !review.isHelpfulByCurrentUser).getOrThrow()
            updateState {
                val current = details ?: return@updateState this
                copy(details = current.copy(reviews = current.reviews.map { item ->
                    if (item.id == reviewId) item.copy(
                        helpfulCount = vote.helpfulCount,
                        isHelpfulByCurrentUser = vote.isHelpful,
                    ) else item
                }))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            sendEvent(ShopDetailEvent.VoteFailed)
        } finally {
            votesInProgress.remove(reviewId)
            updateState { copy(pendingVoteIds = pendingVoteIds - reviewId) }
        }
    }
}
