package com.coffeepeek.feature.shelf.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.shelf.domain.repository.ShelfProfileRepository
import com.coffeepeek.feature.shelf.impl.ui.compose.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal class PublicShelfProfileViewModel(private val repository: ShelfProfileRepository, private val slug: String,
    private val ownerView: Boolean = false) :
    MviViewModel<PublicShelfProfileState, PublicShelfProfileAction, PublicShelfProfileEvent>(initialShelfState(repository, slug, ownerView)) {
    private var generation = 0
    private var work: Job? = null

    init { viewModelScope.launch { repository.owners.distinctUntilChanged().collect { load(reset = true) } } }

    override suspend fun handleActionInternal(action: PublicShelfProfileAction) {
        when (action) {
            PublicShelfProfileAction.Refresh -> if (!currentState.loading && !currentState.refreshing) load()
            is PublicShelfProfileAction.OpenCoffee -> open(action.id)
        }
    }

    private fun load(reset: Boolean = false) {
        val epoch = ++generation
        work?.cancel()
        updateState {
            if (reset || !ownerView) initialShelfState(repository, slug, ownerView).copy(revision = epoch)
            else copy(loading = shelf == null && repository.cachedPublicShelf(slug) == null,
                refreshing = true, failed = false)
        }
        work = viewModelScope.launch {
            try { read(epoch) }
            catch (cancelled: CancellationException) {
                if (epoch == generation) updateState { copy(loading = false, refreshing = false, failed = true) }
                throw cancelled
            }
            catch (_: Exception) { if (epoch == generation) updateState { copy(loading = false, refreshing = false, failed = true) } }
        }
    }

    private suspend fun read(epoch: Int) {
        val result = repository.publicShelf(slug)
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        if (epoch == generation) updateState { copy(shelf = if (result.isSuccess) result.getOrNull() else shelf,
            loading = false, refreshing = false, failed = result.isFailure) }
    }

    private suspend fun open(id: String) {
        if (currentState.loading) return
        val coffee = currentState.shelf?.items?.firstOrNull { it.id == id }?.coffeeSlug?.takeIf(String::isNotBlank) ?: return
        sendEvent(PublicShelfProfileEvent.OpenCoffee(coffee))
    }
}

private fun initialShelfState(repository: ShelfProfileRepository, slug: String, ownerView: Boolean): PublicShelfProfileState {
    val cached = if (ownerView) repository.cachedPublicShelf(slug) else null
    return PublicShelfProfileState(shelf = cached?.shelf, loading = cached == null, refreshing = cached != null,
        revision = if (ownerView) 1 else 0)
}
