package com.coffeepeek.feature.shelf.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.shelf.domain.repository.ShelfProfileRepository
import com.coffeepeek.feature.shelf.impl.ui.compose.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal class PublicShelfProfileViewModel(private val repository: ShelfProfileRepository, private val slug: String) :
    MviViewModel<PublicShelfProfileState, PublicShelfProfileAction, PublicShelfProfileEvent>(PublicShelfProfileState()) {
    private var generation = 0
    private var work: Job? = null

    init { viewModelScope.launch { repository.owners.distinctUntilChanged().collect { load() } } }

    override suspend fun handleActionInternal(action: PublicShelfProfileAction) {
        when (action) {
            PublicShelfProfileAction.Refresh -> if (!currentState.loading) load()
            is PublicShelfProfileAction.OpenCoffee -> open(action.id)
        }
    }

    private fun load() {
        val epoch = ++generation
        work?.cancel()
        updateState { PublicShelfProfileState(revision = epoch) }
        work = viewModelScope.launch {
            try { read(epoch) }
            catch (cancelled: CancellationException) {
                if (epoch == generation) updateState { copy(loading = false, failed = true) }
                throw cancelled
            }
            catch (_: Exception) { if (epoch == generation) updateState { copy(loading = false, failed = true) } }
        }
    }

    private suspend fun read(epoch: Int) {
        val result = repository.publicShelf(slug)
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        if (epoch == generation) updateState { copy(shelf = result.getOrNull(), loading = false, failed = result.isFailure) }
    }

    private suspend fun open(id: String) {
        if (currentState.loading) return
        val coffee = currentState.shelf?.items?.firstOrNull { it.id == id }?.coffeeSlug?.takeIf(String::isNotBlank) ?: return
        sendEvent(PublicShelfProfileEvent.OpenCoffee(coffee))
    }
}
