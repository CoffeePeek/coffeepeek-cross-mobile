package com.coffeepeek.feature.shop.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.shop.domain.model.ShopViewer
import com.coffeepeek.feature.shop.domain.model.ShopCheckIn
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInVoteRepository
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailEvent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailState
import com.coffeepeek.feature.shop.impl.ui.data.toFavoriteSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

internal class ShopDetailViewModel(
    private val shopId: String,
    private val detailsRepository: ShopDetailsRepository,
    private val checkInVoteRepository: ShopCheckInVoteRepository,
    private val favoritesRepository: FavoritesRepository,
    private val currentViewer: suspend () -> Result<ShopViewer>,
    private val currentDayOfWeek: () -> Int,
) : MviViewModel<ShopDetailState, ShopDetailAction, ShopDetailEvent>(ShopDetailState()) {
    private var loadJob: Job? = null
    private var sessionGeneration = 0
    private var resumedOnce = false
    private var favoriteMutationInProgress = false
    private var favoriteObservation: Job? = null
    private var favoriteGeneration = 0
    private var latestFavorites: List<FavoriteShop>? = null

    init { onAction(ShopDetailAction.Retry) }

    override suspend fun handleActionInternal(action: ShopDetailAction) {
        when (action) {
            ShopDetailAction.Retry -> {
                observeFavorites()
                load()
            }
            ShopDetailAction.Resume -> {
                if (resumedOnce) load() else resumedOnce = true
            }
            ShopDetailAction.SessionChanged -> {
                sessionGeneration++
                updateState { copy(details = null, isLoggedIn = false, currentUserId = null,
                    pendingCheckInVoteId = null, isFavoriteLoading = false) }
                observeFavorites()
                load(replace = true)
            }
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
            ShopDetailAction.Share -> {
                val title = currentState.details?.overview?.title ?: return
                sendEvent(ShopDetailEvent.ShareShop(shopId, title))
            }
            ShopDetailAction.SuggestChange -> {
                if (currentState.isLoggedIn) sendEvent(ShopDetailEvent.SuggestChange(shopId))
                else sendEvent(ShopDetailEvent.SignIn)
            }
            ShopDetailAction.OpenRoute -> {
                val overview = currentState.details?.overview ?: return
                val latitude = overview.latitude ?: return
                val longitude = overview.longitude ?: return
                sendEvent(ShopDetailEvent.OpenRoute(latitude, longitude))
            }
            ShopDetailAction.OpenCheckIn -> {
                if (!currentState.isLoggedIn) sendEvent(ShopDetailEvent.SignIn)
                else if (currentState.details != null) sendEvent(ShopDetailEvent.CreateCheckIn)
            }
            ShopDetailAction.OpenCheckIns -> currentState.details?.let {
                sendEvent(ShopDetailEvent.OpenCheckIns(it.overview.id))
            }
            is ShopDetailAction.VoteCheckInHelpful -> voteCheckIn(action.checkInId)
            is ShopDetailAction.ReportCheckIn -> {
                val checkIn = currentState.details?.checkIns?.firstOrNull { it.id == action.checkInId } ?: return
                if (!currentState.isLoggedIn) sendEvent(ShopDetailEvent.SignIn)
                else if (!ownsCheckIn(checkIn)) sendEvent(ShopDetailEvent.ReportCheckIn(checkIn.id))
            }
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
        }
    }

    private suspend fun load(replace: Boolean = false) {
        if (loadJob?.isActive == true && !replace) return
        if (replace) loadJob?.cancel()
        val job = currentCoroutineContext().job
        loadJob = job
        updateState { copy(isLoading = true, hasError = false) }
        try {
            val viewer = currentViewer().getOrThrow()
            val loadedDetails = detailsRepository.getDetails(shopId).getOrThrow()
            val details = if (viewer.isLoggedIn) loadedDetails
                else loadedDetails.copy(userCheckIns = emptyList())
            if (loadJob !== job) return
            val dayOfWeek = currentDayOfWeek()
            updateState {
                copy(details = details, isLoggedIn = viewer.isLoggedIn,
                    currentUserId = viewer.userId, todayDayOfWeek = dayOfWeek,
                    isFavorite = latestFavorites?.any { it.id == details.overview.id } == true,
                    hasError = false)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (loadJob === job) updateState { copy(details = null, hasError = true) }
        } finally {
            if (loadJob === job) {
                loadJob = null
                updateState { copy(isLoading = false) }
            }
        }
    }

    private fun observeFavorites() {
        val generation = ++favoriteGeneration
        favoriteObservation?.cancel()
        updateState { copy(favoriteAvailable = false) }
        favoriteObservation = viewModelScope.launch {
            try {
                favoritesRepository.observe().collect { result ->
                    result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
                    if (generation != favoriteGeneration) return@collect
                    result.getOrNull()?.let { latestFavorites = it }
                    updateState {
                        copy(
                            isFavorite = latestFavorites?.any { it.id == (details?.overview?.id ?: shopId) }
                                ?: isFavorite,
                            favoriteAvailable = result.isSuccess,
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation == favoriteGeneration) updateState { copy(favoriteAvailable = false) }
            }
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
        val generation = sessionGeneration
        updateState { copy(isFavoriteLoading = true) }
        try {
            val newValue = !currentState.isFavorite
            val result = if (newValue) favoritesRepository.save(details.toFavoriteSnapshot())
                else favoritesRepository.remove(details.overview.id)
            result.getOrThrow()
            // Membership comes from observation, including writes from other screens and logout.
            if (generation == sessionGeneration) sendEvent(ShopDetailEvent.FavoriteChanged(details.overview.id, newValue))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (generation == sessionGeneration) sendEvent(ShopDetailEvent.FavoriteFailed)
        } finally {
            favoriteMutationInProgress = false
            updateState { copy(isFavoriteLoading = false) }
        }
    }

    private fun ownsCheckIn(checkIn: ShopCheckIn): Boolean =
        (currentState.currentUserId?.takeIf(String::isNotBlank)?.let { it == checkIn.userId } == true) ||
            currentState.details?.userCheckIns?.any { it.id == checkIn.id } == true

    private suspend fun voteCheckIn(checkInId: String) {
        val checkIn = currentState.details?.checkIns?.firstOrNull { it.id == checkInId } ?: return
        if (!currentState.isLoggedIn) {
            sendEvent(ShopDetailEvent.SignIn)
            return
        }
        if (currentState.pendingCheckInVoteId != null || ownsCheckIn(checkIn)) return
        updateState { copy(pendingCheckInVoteId = checkInId) }
        val generation = sessionGeneration
        try {
            val vote = checkInVoteRepository.setHelpful(checkInId, !checkIn.isHelpfulByCurrentUser).getOrThrow()
            if (generation != sessionGeneration) return
            updateState {
                val current = details ?: return@updateState this
                copy(details = current.copy(checkIns = current.checkIns.map { item ->
                    if (item.id == checkInId) item.copy(
                        helpfulCount = vote.helpfulCount, isHelpfulByCurrentUser = vote.isHelpful,
                    ) else item
                }))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (generation == sessionGeneration) sendEvent(ShopDetailEvent.VoteFailed)
        } finally {
            if (generation == sessionGeneration) updateState { copy(pendingCheckInVoteId = null) }
        }
    }
}
