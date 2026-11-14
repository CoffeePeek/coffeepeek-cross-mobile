package com.coffeepeek.feature.coffee.impl.ui

import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.coffee.api.CoffeeContentAction
import com.coffeepeek.feature.coffee.api.CoffeeDetailAnalytics
import com.coffeepeek.feature.coffee.api.CoffeeDetailSection
import com.coffeepeek.feature.coffee.domain.model.CoffeeFilters
import com.coffeepeek.feature.coffee.domain.repository.CoffeeRepository
import com.coffeepeek.feature.coffee.impl.ui.compose.model.CoffeeDetailAction
import com.coffeepeek.feature.coffee.impl.ui.compose.model.CoffeeDetailEvent
import com.coffeepeek.feature.coffee.impl.ui.compose.model.CoffeeDetailState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

internal class CoffeeDetailViewModel(
    private val slug: String,
    private val repository: CoffeeRepository,
    private val analytics: CoffeeDetailAnalytics = CoffeeDetailAnalytics.None,
) :
    MviViewModel<CoffeeDetailState, CoffeeDetailAction, CoffeeDetailEvent>(CoffeeDetailState()) {
    private var initialized = false
    private var loadJob: Job? = null
    private var similarJob: Job? = null
    private var loadRevision = 0L
    private var similarRevision = 0L

    init { onAction(CoffeeDetailAction.Initialize) }

    override suspend fun handleActionInternal(action: CoffeeDetailAction) {
        when (action) {
            CoffeeDetailAction.Initialize -> if (!initialized) {
                initialized = true
                coroutineScope {
                    launch { loadLabels() }
                    launch { loadDetail() }
                }
            }
            CoffeeDetailAction.Retry -> loadDetail()
            CoffeeDetailAction.RetrySimilar -> loadSimilar()
            CoffeeDetailAction.Back -> sendEvent(CoffeeDetailEvent.Back)
            CoffeeDetailAction.OpenPhotos -> if (!currentState.details?.photos.isNullOrEmpty()) {
                analytics.contentInteracted(CoffeeDetailSection.Photos, CoffeeContentAction.Open)
                updateState { copy(arePhotosOpen = true) }
            }
            CoffeeDetailAction.ClosePhotos -> updateState { copy(arePhotosOpen = false) }
            CoffeeDetailAction.ToggleDescription -> if (currentState.details?.description != null) {
                analytics.contentInteracted(CoffeeDetailSection.Description,
                    if (currentState.isDescriptionExpanded) CoffeeContentAction.Close else CoffeeContentAction.Open)
                updateState { copy(isDescriptionExpanded = !isDescriptionExpanded) }
            }
            CoffeeDetailAction.OpenRoaster -> currentState.details?.roasterSlug?.takeIf(String::isNotBlank)?.let {
                analytics.contentInteracted(CoffeeDetailSection.Roaster, CoffeeContentAction.Select)
                analytics.roasterOpened()
                sendEvent(CoffeeDetailEvent.OpenRoaster(it))
            }
            is CoffeeDetailAction.OpenSimilar -> if (action.slug.isNotBlank() && currentState.similar.any { it.slug == action.slug }) {
                analytics.contentInteracted(CoffeeDetailSection.SimilarCoffees, CoffeeContentAction.Select)
                analytics.similarOpened()
                sendEvent(CoffeeDetailEvent.OpenCoffee(action.slug))
            }
            is CoffeeDetailAction.OpenSeller -> currentState.details?.coffee?.offers?.getOrNull(action.offerIndex)?.sourceUrl?.let {
                sendEvent(CoffeeDetailEvent.OpenSeller(it))
            }
            CoffeeDetailAction.Share -> currentState.details?.let {
                analytics.contentInteracted(CoffeeDetailSection.Contacts, CoffeeContentAction.Share)
                sendEvent(CoffeeDetailEvent.Share(it.coffee.name, it.canonicalPath))
            }
        }
    }

    private suspend fun loadDetail() {
        loadJob?.cancel()
        similarJob?.cancel()
        similarRevision++
        val job = currentCoroutineContext().job
        val revision = ++loadRevision
        loadJob = job
        updateState { CoffeeDetailState(filterGroups = filterGroups) }
        try {
            if (slug.isBlank() || slug.length > 200) {
                updateState { copy(loadFailed = true) }
                return
            }
            val details = repository.getDetails(slug).getOrThrow()
            currentCoroutineContext().ensureActive()
            if (loadJob !== job || revision != loadRevision) return
            updateState { copy(details = details) }
            onAction(CoffeeDetailAction.RetrySimilar)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            if (loadJob === job && revision == loadRevision && job.isActive) updateState { copy(loadFailed = true) }
        } finally {
            if (loadJob === job) { loadJob = null; updateState { copy(isLoading = false) } }
        }
    }

    private suspend fun loadSimilar() {
        val coffee = currentState.details?.coffee ?: return
        val tastes = coffee.tasteCodes.toSet()
        if (tastes.isEmpty()) return
        similarJob?.cancel()
        val job = currentCoroutineContext().job
        val revision = ++similarRevision
        val detailRevision = loadRevision
        similarJob = job
        updateState { copy(similarLoading = true, similarFailed = false) }
        try {
            val page = repository.search("", CoffeeFilters(mapOf("taste" to tastes)), 1).getOrThrow()
            currentCoroutineContext().ensureActive()
            if (similarJob === job && revision == similarRevision && detailRevision == loadRevision) {
                updateState { copy(similar = page.items.filter { it.slug != coffee.slug }.take(6)) }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            if (similarJob === job && revision == similarRevision && detailRevision == loadRevision && job.isActive) {
                updateState { copy(similarFailed = true) }
            }
        } finally {
            if (similarJob === job) { similarJob = null; updateState { copy(similarLoading = false) } }
        }
    }

    private suspend fun loadLabels() {
        try {
            val groups = repository.getFilterGroups().getOrThrow()
            currentCoroutineContext().ensureActive()
            updateState { copy(filterGroups = groups) }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            // Existing detail behavior: optional catalogue labels failing must not hide the coffee.
        }
    }

    override fun onCleared() {
        loadRevision++; similarRevision++
        loadJob?.cancel(); similarJob?.cancel()
        super.onCleared()
    }
}
