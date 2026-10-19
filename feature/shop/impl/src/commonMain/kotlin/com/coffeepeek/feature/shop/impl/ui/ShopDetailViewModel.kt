package com.coffeepeek.feature.shop.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.feature.shop.api.ShopDetailContext
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.shop.impl.ui.data.blockableAuthor
import com.coffeepeek.feature.shop.domain.model.ShopViewer
import com.coffeepeek.feature.shop.domain.model.ShopCheckIn
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInVoteRepository
import com.coffeepeek.feature.shop.domain.repository.ShopFavorites
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailEvent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailEventEnvelope
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailState
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopPriceLevel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

internal class ShopDetailViewModel(
    private val shopId: String,
    private val detailsRepository: ShopDetailsRepository,
    private val checkInVoteRepository: ShopCheckInVoteRepository,
    private val favoritesRepository: ShopFavorites,
    private val currentViewer: suspend () -> Result<ShopViewer>,
    private val currentDayOfWeek: () -> Int,
    private val currentMinuteOfDay: () -> Int? = { null },
    initialSessionGeneration: Int = 0,
    initialRefreshGeneration: Long = 0,
) : MviViewModel<ShopDetailState, ShopDetailAction, ShopDetailEventEnvelope>(
    ShopDetailState(sessionGeneration = initialSessionGeneration),
) {
    private var loadJob: Job? = null
    private var voteJob: Job? = null
    private var refreshQueued = false
    private var sessionGeneration = initialSessionGeneration
    private var refreshGeneration = initialRefreshGeneration
    private var resumedOnce = false
    private var favoriteMutationJob: Job? = null
    private var favoriteObservation: Job? = null
    private var favoriteGeneration = 0
    private var latestFavorites: Set<String>? = null

    init { onAction(ShopDetailAction.Retry) }

    override suspend fun handleActionInternal(action: ShopDetailAction) {
        when (action) {
            ShopDetailAction.Retry -> {
                observeFavorites()
                load()
            }
            ShopDetailAction.Resume -> {
                updateClock()
                if (resumedOnce) load() else resumedOnce = true
            }
            is ShopDetailAction.Refresh -> {
                if (action.generation <= refreshGeneration) return
                refreshGeneration = action.generation
                if (loadJob != null || voteJob != null) refreshQueued = true else load()
            }
            is ShopDetailAction.SessionChanged -> {
                if (action.generation <= sessionGeneration) return
                val nextGeneration = action.generation
                sessionGeneration = nextGeneration
                loadJob?.cancel()
                voteJob?.cancel()
                favoriteMutationJob?.cancel()
                favoriteObservation?.cancel()
                loadJob = null
                voteJob = null
                favoriteMutationJob = null
                latestFavorites = null
                refreshQueued = false
                updateState { ShopDetailState(sessionGeneration = nextGeneration) }
                observeFavorites()
                load()
            }
            ShopDetailAction.Back -> emitEvent(ShopDetailEvent.Back)
            ShopDetailAction.ChooseAnotherShop -> emitEvent(ShopDetailEvent.ChooseAnotherShop)
            ShopDetailAction.ReportShop -> {
                val context = detailContext() ?: return
                if (currentState.isLoggedIn) {
                    emitEvent(ShopDetailEvent.ReportShop(context))
                } else emitEvent(ShopDetailEvent.SignIn)
            }
            ShopDetailAction.OpenPriceInfo -> {
                if (ShopPriceLevel.from(currentState.details?.overview?.priceRange) != null) {
                    updateState { copy(priceInfoVisible = true) }
                }
            }
            ShopDetailAction.DismissPriceInfo -> updateState { copy(priceInfoVisible = false) }
            ShopDetailAction.OpenMap -> {
                val overview = currentState.details?.overview ?: return
                val latitude = overview.latitude ?: return
                val longitude = overview.longitude ?: return
                emitEvent(ShopDetailEvent.OpenMap(latitude, longitude, overview.title))
            }
            ShopDetailAction.OpenMenuGallery -> {
                if (currentState.details?.menu?.photos?.isNotEmpty() == true) {
                    emitEvent(ShopDetailEvent.OpenMenuGallery(shopId))
                }
            }
            ShopDetailAction.ToggleSchedule -> {
                updateClock()
                updateState { copy(scheduleExpanded = !scheduleExpanded) }
            }
            ShopDetailAction.ToggleFeatures -> updateState { copy(featuresExpanded = !featuresExpanded) }
            ShopDetailAction.SignIn -> emitEvent(ShopDetailEvent.SignIn)
            ShopDetailAction.Register -> emitEvent(ShopDetailEvent.Register)
            ShopDetailAction.ToggleFavorite -> toggleFavorite()
            ShopDetailAction.Share -> {
                val overview = currentState.details?.overview ?: return
                val path = overview.canonicalPath?.takeIf { it.startsWith('/') && !it.startsWith("//") } ?: return
                emitEvent(ShopDetailEvent.ShareShop(path, overview.title))
            }
            ShopDetailAction.SuggestChange -> {
                val context = detailContext() ?: return
                if (currentState.isLoggedIn) emitEvent(ShopDetailEvent.SuggestChange(context))
                else emitEvent(ShopDetailEvent.SignIn)
            }
            ShopDetailAction.OpenRoute -> {
                val overview = currentState.details?.overview ?: return
                val latitude = overview.latitude ?: return
                val longitude = overview.longitude ?: return
                emitEvent(ShopDetailEvent.OpenRoute(latitude, longitude))
            }
            ShopDetailAction.OpenCheckIn -> {
                if (!currentState.isLoggedIn) emitEvent(ShopDetailEvent.SignIn)
                else detailContext()?.let { emitEvent(ShopDetailEvent.CreateCheckIn(it)) }
            }
            ShopDetailAction.OpenCheckIns -> currentState.details?.let {
                emitEvent(ShopDetailEvent.OpenCheckIns(shopId))
            }
            is ShopDetailAction.VoteCheckInHelpful -> voteCheckIn(action.checkInId)
            is ShopDetailAction.OpenAuthor -> {
                val visible = currentState.details?.checkIns.orEmpty().let {
                    if (currentState.isLoggedIn) it.take(3) else it.take(1)
                }
                val row = visible.firstOrNull { it.id == action.checkInId } ?: return
                val slug = row.authorSlug?.takeIf(String::isNotBlank) ?: return
                emitEvent(ShopDetailEvent.OpenAuthor(slug, row.username, row.authorAvatarUrl))
            }
            is ShopDetailAction.ReportCheckIn -> {
                val checkIn = currentState.details?.checkIns?.firstOrNull { it.id == action.checkInId } ?: return
                if (!currentState.isLoggedIn) emitEvent(ShopDetailEvent.SignIn)
                else if (!ownsCheckIn(checkIn)) emitEvent(ShopDetailEvent.ReportCheckIn(checkIn.id))
            }
            is ShopDetailAction.BlockAuthor -> {
                if (currentState.isLoading) return
                val visible = currentState.details?.checkIns.orEmpty().let {
                    if (currentState.isLoggedIn) it.take(3) else it.take(1)
                }
                val row = visible.firstOrNull { it.id == action.checkInId } ?: return
                val author = row.blockableAuthor(ownsCheckIn(row), currentState.isLoggedIn,
                    currentState.currentUserId) ?: return
                emitEvent(ShopDetailEvent.BlockAuthor(author))
            }
            is ShopDetailAction.OpenPhoto -> {
                if (action.index in action.urls.indices) {
                    emitEvent(ShopDetailEvent.OpenPhoto(action.urls, action.index))
                }
            }
            is ShopDetailAction.OpenCheckInPhoto -> {
                val visible = currentState.details?.checkIns.orEmpty().let {
                    if (currentState.isLoggedIn) it.take(3) else it.take(1)
                }
                val row = visible.firstOrNull { it.id == action.checkInId } ?: return
                if (action.index in row.photoUrls.indices) {
                    emitEvent(ShopDetailEvent.OpenPhoto(row.photoDetailUrls, action.index, row.photoUrls))
                }
            }
            is ShopDetailAction.OpenRoaster -> {
                if (currentState.details?.coffee?.roasters?.any { it.id == action.id } == true) {
                    emitEvent(ShopDetailEvent.OpenRoaster(action.id))
                }
            }
            is ShopDetailAction.OpenLink -> {
                if (action.target.startsWith("https://") || action.target.startsWith("http://") ||
                    action.target.startsWith("mailto:") || action.target.startsWith("tel:")) {
                    emitEvent(ShopDetailEvent.OpenLink(action.target))
                }
            }
            is ShopDetailAction.CopyPhone -> {
                if (action.number == currentState.details?.contact?.phone?.trim()) {
                    emitEvent(ShopDetailEvent.CopyPhone(action.number))
                }
            }
        }
    }

    private suspend fun load() {
        // A details response must not roll back a vote that completed after its request began.
        if (voteJob != null) { refreshQueued = true; return }
        if (loadJob != null) return
        val job = currentCoroutineContext().job
        val generation = sessionGeneration
        loadJob = job
        var wasCancelled = false
        updateState { copy(isLoading = true, hasError = false) }
        try {
            val viewer = currentViewer().getOrThrow()
            currentCoroutineContext().ensureActive()
            if (loadJob !== job || generation != sessionGeneration) return
            val loadedDetails = detailsRepository.getDetails(shopId).getOrThrow()
            val details = if (viewer.isLoggedIn) loadedDetails
                else loadedDetails.copy(userCheckIns = emptyList())
            if (loadJob !== job || generation != sessionGeneration) return
            val dayOfWeek = currentDayOfWeek()
            val minuteOfDay = currentMinuteOfDay()
            updateState {
                copy(details = details, isLoggedIn = viewer.isLoggedIn,
                    currentUserId = viewer.userId.takeIf { viewer.isLoggedIn },
                    todayDayOfWeek = dayOfWeek, todayMinuteOfDay = minuteOfDay,
                    featuresExpanded = featuresExpanded && this.details?.features == details.features,
                    isFavorite = latestFavorites?.contains(details.overview.id) == true,
                    hasError = false)
            }
        } catch (cancelled: CancellationException) {
            wasCancelled = true
            throw cancelled
        } catch (_: Exception) {
            if (loadJob === job) updateState { copy(hasError = true) }
        } finally {
            if (loadJob === job) {
                loadJob = null
                updateState { copy(isLoading = false) }
                refreshIfQueued(generation, wasCancelled)
            }
        }
    }

    private fun updateClock() {
        val day = currentDayOfWeek()
        val minute = currentMinuteOfDay()
        updateState { copy(todayDayOfWeek = day, todayMinuteOfDay = minute) }
    }

    private fun observeFavorites() {
        val generation = ++favoriteGeneration
        favoriteObservation?.cancel()
        updateState { copy(favoriteAvailable = false) }
        favoriteObservation = viewModelScope.launch {
            try {
                favoritesRepository.observeIds().collect { result ->
                    result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
                    if (generation != favoriteGeneration) return@collect
                    result.getOrNull()?.let { latestFavorites = it }
                    updateState {
                        copy(
                            isFavorite = latestFavorites?.contains(details?.overview?.id ?: shopId)
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
            emitEvent(ShopDetailEvent.SignIn)
            return
        }
        if (!currentState.favoriteAvailable || favoriteMutationJob != null) return
        val details = currentState.details ?: return
        val job = currentCoroutineContext().job
        favoriteMutationJob = job
        val generation = sessionGeneration
        updateState { copy(isFavoriteLoading = true) }
        try {
            val newValue = !currentState.isFavorite
            val result = if (newValue) favoritesRepository.save(details)
                else favoritesRepository.remove(details.overview.id)
            result.getOrThrow()
            // Membership comes from observation, including writes from other screens and logout.
            if (generation == sessionGeneration && favoriteMutationJob === job) {
                emitEvent(ShopDetailEvent.FavoriteChanged(details.overview.id, newValue))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (generation == sessionGeneration && favoriteMutationJob === job) emitEvent(ShopDetailEvent.FavoriteFailed)
        } finally {
            if (favoriteMutationJob === job) {
                favoriteMutationJob = null
                if (generation == sessionGeneration) updateState { copy(isFavoriteLoading = false) }
            }
        }
    }

    private fun ownsCheckIn(checkIn: ShopCheckIn): Boolean =
        (currentState.currentUserId?.takeIf(String::isNotBlank)?.let { it == checkIn.userId } == true) ||
            currentState.details?.userCheckIns?.any { it.id == checkIn.id } == true

    private suspend fun voteCheckIn(checkInId: String) {
        val checkIn = currentState.details?.checkIns?.firstOrNull { it.id == checkInId } ?: return
        if (!currentState.isLoggedIn) {
            emitEvent(ShopDetailEvent.SignIn)
            return
        }
        if (currentState.pendingCheckInVoteId != null || voteJob != null || loadJob?.isActive == true ||
            ownsCheckIn(checkIn)) return
        val job = currentCoroutineContext().job
        val generation = sessionGeneration
        voteJob = job
        updateState { copy(pendingCheckInVoteId = checkInId) }
        var wasCancelled = false
        try {
            val vote = checkInVoteRepository.setHelpful(checkInId, !checkIn.isHelpfulByCurrentUser).getOrThrow()
            if (generation != sessionGeneration || voteJob !== job) return
            updateState {
                val current = details ?: return@updateState this
                copy(details = current.copy(checkIns = current.checkIns.map { item ->
                    if (item.id == checkInId) item.copy(
                        helpfulCount = vote.helpfulCount, isHelpfulByCurrentUser = vote.isHelpful,
                    ) else item
                }))
            }
        } catch (cancelled: CancellationException) {
            wasCancelled = true
            throw cancelled
        } catch (_: Exception) {
            if (generation == sessionGeneration && voteJob === job) emitEvent(ShopDetailEvent.VoteFailed)
        } finally {
            if (voteJob === job) {
                voteJob = null
                if (generation == sessionGeneration) updateState { copy(pendingCheckInVoteId = null) }
                refreshIfQueued(generation, wasCancelled)
            }
        }
    }

    private suspend fun refreshIfQueued(generation: Int, wasCancelled: Boolean) {
        if (!refreshQueued) return
        refreshQueued = false
        if (!wasCancelled && currentCoroutineContext().job.isActive && generation == sessionGeneration) {
            onAction(ShopDetailAction.Retry)
        }
    }

    private suspend fun emitEvent(event: ShopDetailEvent) {
        sendEvent(ShopDetailEventEnvelope(event, sessionGeneration))
    }

    private fun detailContext(): ShopDetailContext? {
        val overview = currentState.details?.overview ?: return null
        // The current details API maps address.slug to overview.id; it exposes no separate numeric/GUID ID.
        return overview.id.takeIf(String::isNotBlank)?.let { ShopDetailContext(shopId, it, overview.title) }
    }
}
