package com.coffeepeek.feature.roaster.impl.ui

import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.roaster.api.RoasterFavoriteTarget
import com.coffeepeek.feature.roaster.api.RoasterFavoritesPort
import com.coffeepeek.feature.roaster.api.RoasterListAnalytics
import com.coffeepeek.feature.roaster.domain.repository.RoasterRepository
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListAction
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListEvent
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListItem
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListState
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

internal class RoasterListViewModel(
    private val repository: RoasterRepository,
    private val favorites: RoasterFavoritesPort = RoasterFavoritesPort.None,
    private val analytics: RoasterListAnalytics = RoasterListAnalytics.None,
) : MviViewModel<RoasterListState, RoasterListAction, RoasterListEvent>(RoasterListState()) {
    private var initialized = false
    private var loadJob: Job? = null
    private var loadRevision = 0L
    private val detailLocksMutex = Mutex()
    private val detailLocks = mutableMapOf<String, Mutex>()
    private val detailRequests = Semaphore(MAX_DETAIL_REQUESTS)

    init { onAction(RoasterListAction.Initialize) }

    override suspend fun handleActionInternal(action: RoasterListAction) {
        when (action) {
            RoasterListAction.Initialize -> if (!initialized) {
                initialized = true
                // Independent, lifecycle-owned children: favorites keys, store errors and the catalog read.
                coroutineScope {
                    launch { observeFavoriteKeys() }
                    launch { observeFavoriteErrors() }
                    launch { load() }
                }
            }
            RoasterListAction.Refresh -> load()
            is RoasterListAction.QueryChanged -> { updateState { copy(query = action.query) }; reportResults() }
            is RoasterListAction.ToggleTag -> {
                updateState { copy(selectedTags = if (action.slug in selectedTags) selectedTags - action.slug else selectedTags + action.slug) }
                reportResults()
            }
            RoasterListAction.ClearTags -> { updateState { copy(selectedTags = emptySet()) }; reportResults() }
            is RoasterListAction.FavoritesOnlyChanged -> { updateState { copy(favoritesOnly = action.enabled) }; reportResults() }
            is RoasterListAction.SelectedRoastersChanged -> { updateState { copy(selectedRoasters = action.slugs) }; reportResults() }
            is RoasterListAction.LoadDetails -> loadDetails(action.slug)
            is RoasterListAction.ToggleFavorite -> toggleFavorite(action.slug)
            is RoasterListAction.OpenRoaster -> openRoaster(action.slug)
            RoasterListAction.DismissMessage -> updateState { copy(message = null) }
        }
    }

    private suspend fun observeFavoriteKeys() {
        try {
            favorites.observeKeys().collect { keys -> updateState { copy(favoriteKeys = keys) } }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { updateState { copy(message = RoasterMessage.FavoritesUnavailable) } }
    }

    private suspend fun observeFavoriteErrors() {
        try {
            favorites.observeErrors().collect { error -> if (error != null) updateState { copy(message = RoasterMessage.FromStore(error)) } }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* Error reporting is best effort. */ }
    }

    private suspend fun load() {
        loadJob?.cancel()
        val job = currentCoroutineContext().job
        val revision = ++loadRevision
        loadJob = job
        updateState { copy(isLoading = true, loadFailed = false) }
        try {
            val catalog = repository.getRoasters().getOrThrow()
            currentCoroutineContext().ensureActive()
            if (loadJob !== job || revision != loadRevision) return
            updateState { copy(items = catalog.map { RoasterListItem(it) }, isLoading = false) }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            if (loadJob === job && revision == loadRevision && job.isActive) updateState { copy(isLoading = false, loadFailed = true) }
        } finally {
            if (loadJob === job) {
                loadJob = null
                updateState { copy(isLoading = false) }
                reportResults()
            }
        }
    }

    private suspend fun loadDetails(slug: String) {
        if (slug.isBlank()) return
        val lock = detailLocksMutex.withLock { detailLocks.getOrPut(slug) { Mutex() } }
        lock.withLock {
            if (currentState.items.none { it.slug == slug && it.details == null }) return
            val details = detailRequests.withPermit { repository.getRoaster(slug).getOrNull() }
            currentCoroutineContext().ensureActive()
            if (details != null) updateState {
                copy(items = items.map { item -> if (item.slug == slug) item.copy(details = details) else item })
            }
        }
    }

    private fun toggleFavorite(slug: String) {
        val item = currentState.items.firstOrNull { it.slug == slug } ?: return
        val key = item.favoriteKey
        if (key.isBlank()) return
        val target = RoasterFavoriteTarget(key, item.summary.name, item.summary.photoUrl ?: item.details?.photos?.firstOrNull()?.url)
        favorites.change(target, favorite = key !in currentState.favoriteKeys) {
            updateState { copy(message = RoasterMessage.FavoriteChangeFailed) }
        }
    }

    private suspend fun openRoaster(slug: String) {
        val index = currentState.visibleItems.indexOfFirst { it.slug == slug }
        if (slug.isBlank() || index < 0) return
        analytics.opened(index + 1)
        sendEvent(RoasterListEvent.OpenRoaster(slug))
    }

    /** Reported once the visible set settled; the adapter ignores unchanged criteria. */
    private fun reportResults() {
        val state = currentState
        if (state.isLoading) return
        val keys = buildList {
            if (state.selectedRoasters.isNotEmpty()) add("roasters")
            if (state.selectedTags.isNotEmpty()) add("tags")
            if (state.favoritesOnly) add("favoritesOnly")
        }
        val fingerprint = listOf(state.query.trim().lowercase(), state.selectedRoasters.sorted().joinToString(","),
            state.favoritesOnly, state.selectedTags.sorted().joinToString(",")).joinToString("|")
        analytics.resultsShown(fingerprint, state.query, keys, state.visibleItems.size, state.loadFailed)
    }

    override fun onCleared() {
        loadRevision++
        loadJob?.cancel()
        super.onCleared()
    }

    private companion object { const val MAX_DETAIL_REQUESTS = 4 }
}
