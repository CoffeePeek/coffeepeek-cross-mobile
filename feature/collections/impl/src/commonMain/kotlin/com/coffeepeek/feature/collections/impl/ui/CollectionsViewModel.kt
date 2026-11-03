package com.coffeepeek.feature.collections.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.collections.domain.model.CollectionRules
import com.coffeepeek.feature.collections.domain.repository.CollectionsAccount
import com.coffeepeek.feature.collections.domain.repository.CollectionsRepository
import com.coffeepeek.feature.collections.impl.ui.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal class CollectionsViewModel(private val repository: CollectionsRepository, private val account: CollectionsAccount,
    private val publicId: String? = null) : MviViewModel<CollectionsState, CollectionsAction, CollectionsEvent>(
    CollectionsState(publicView = publicId != null)) {
    private var generation = 0
    private var work: Job? = null
    private var requestedCollectionId: String? = null

    init {
        if (publicId != null) read(publicId)
        else {
            observeAccount()
            viewModelScope.launch {
                try { account.loadId() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { updateState { copy(loading = false, failure = CollectionsFailure.Load) } }
            }
        }
    }

    override suspend fun handleActionInternal(action: CollectionsAction) {
        when (action) {
            CollectionsAction.Back -> back()
            CollectionsAction.Retry -> retry()
            CollectionsAction.SignIn -> sendEvent(CollectionsEvent.SignIn)
            CollectionsAction.SelectAll -> selectAll()
            CollectionsAction.OpenLinks -> loadLinks()
            CollectionsAction.ShareLink -> currentState.collection?.let { sendEvent(CollectionsEvent.ShareLink(it)) }
            CollectionsAction.ShareImages -> export()
            is CollectionsAction.Toggle -> toggle(action.key)
            is CollectionsAction.Move -> move(action)
            is CollectionsAction.Title -> if (!currentState.busy) updateState { copy(title = action.value.take(CollectionRules.maxTitleLength), failure = null) }
            is CollectionsAction.Create -> create(action.defaultTitle)
            is CollectionsAction.Revoke -> revoke(action.id)
            is CollectionsAction.OpenCollection -> if (!currentState.busy) read(action.id)
            is CollectionsAction.OpenPlace -> if (!currentState.busy) sendEvent(CollectionsEvent.OpenPlace(action.place))
            is CollectionsAction.Format -> if (!currentState.busy) updateState { copy(format = action.value) }
            is CollectionsAction.ExportFinished -> finishExport(action)
        }
    }

    private fun observeAccount() {
        viewModelScope.launch {
            account.identities.distinctUntilChanged().catch { error ->
                if (error is CancellationException) throw error
                emit(null)
            }.collectLatest { owner ->
                generation++
                work?.cancel()
                requestedCollectionId = null
                updateState { CollectionsState(signedIn = owner != null, loading = owner != null) }
                if (owner != null) loadFavorites()
            }
        }
    }

    private fun loadFavorites() {
        requestedCollectionId = null
        val epoch = generation
        work?.cancel()
        updateState { copy(loading = true, failure = null, page = CollectionsPage.Selection) }
        launchWork(epoch) {
            val result = repository.favorites().checked()
            if (epoch != generation) return@launchWork
            updateState { copy(loading = false, places = result.getOrNull() ?: places,
                selected = selected.intersect(result.getOrNull()?.map { it.key }?.toSet() ?: selected),
                failure = if (result.isFailure) CollectionsFailure.Load else null) }
        }
    }

    private fun create(defaultTitle: String) {
        if (currentState.busy || currentState.loading || !currentState.signedIn) return
        val title = currentState.title.ifBlank { defaultTitle }.trim()
        val places = currentState.places.filter { it.key in currentState.selected }
        if (!CollectionRules.validSelection(places) || !CollectionRules.validTitle(title)) return
        val epoch = generation
        updateState { copy(busy = true, failure = null) }
        launchWork(epoch) {
            val result = repository.create(title, places).checked()
            if (epoch != generation) return@launchWork
            updateState { copy(busy = false, title = title, collection = result.getOrNull(),
                page = if (result.isSuccess) CollectionsPage.Preview else page,
                failure = if (result.isFailure) CollectionsFailure.Create else null) }
        }
    }

    private fun read(id: String) {
        requestedCollectionId = id
        val epoch = generation
        work?.cancel()
        updateState { copy(loading = true, failure = null, collection = null, page = CollectionsPage.Preview) }
        launchWork(epoch) {
            val result = repository.read(id).checked()
            if (epoch != generation) return@launchWork
            updateState { copy(loading = false, collection = result.getOrNull(), failure = if (result.isFailure) CollectionsFailure.Load else null) }
        }
    }

    private fun loadLinks() {
        if (currentState.busy || !currentState.signedIn) return
        requestedCollectionId = null
        val epoch = generation
        work?.cancel()
        updateState { copy(page = CollectionsPage.Links, loading = true, failure = null) }
        launchWork(epoch) {
            val result = repository.mine().checked()
            if (epoch != generation) return@launchWork
            updateState { copy(loading = false, links = result.getOrNull() ?: links, failure = if (result.isFailure) CollectionsFailure.Load else null) }
        }
    }

    private fun revoke(id: String) {
        if (currentState.busy || currentState.links.none { it.id == id }) return
        val epoch = generation
        updateState { copy(busy = true, failure = null) }
        launchWork(epoch) {
            val result = repository.revoke(id).checked()
            if (epoch != generation) return@launchWork
            updateState { copy(busy = false, links = if (result.isSuccess) links.filterNot { it.id == id } else links,
                failure = if (result.isFailure) CollectionsFailure.Revoke else null) }
        }
    }

    private fun toggle(key: String) {
        if (currentState.busy || currentState.places.none { it.key == key }) return
        updateState {
            when {
                key in selected -> copy(selected = selected - key, failure = null)
                else -> copy(selected = selected + key, failure = null)
            }
        }
    }

    private fun selectAll() {
        if (currentState.busy) return
        updateState { copy(selected = if (selected.isNotEmpty()) emptySet()
            else places.map { it.key }.toSet(), failure = null) }
    }

    private fun move(action: CollectionsAction.Move) {
        if (publicId != null || !currentState.signedIn || currentState.page != CollectionsPage.Selection ||
            currentState.busy || currentState.loading) return
        updateState {
            val reordered = moveCollectionPlace(places, action.key, action.targetKey, action.after)
            if (reordered == places) this else copy(places = reordered, failure = null)
        }
    }

    private suspend fun export() {
        val collection = currentState.collection ?: return
        if (currentState.busy || collection.places.isEmpty()) return
        updateState { copy(busy = true, failure = null) }
        sendEvent(CollectionsEvent.ExportImages(collection, currentState.format, generation))
    }

    internal fun isCurrentExport(epoch: Int, id: String): Boolean = epoch == generation && currentState.collection?.id == id && currentState.busy

    private fun finishExport(action: CollectionsAction.ExportFinished) {
        if (isCurrentExport(action.generation, action.collectionId)) updateState {
            copy(busy = false, failure = if (action.failed) CollectionsFailure.Export else null)
        }
    }

    private suspend fun back() {
        if (currentState.busy || currentState.loading) {
            generation++
            work?.cancel()
            sendEvent(CollectionsEvent.CancelExport)
            updateState { copy(busy = false, loading = false) }
        }
        if (publicId != null || currentState.page == CollectionsPage.Selection) sendEvent(CollectionsEvent.Back)
        else {
            requestedCollectionId = null
            updateState { copy(page = CollectionsPage.Selection, collection = null, failure = null) }
        }
    }

    private fun retry() {
        if (currentState.busy || currentState.loading) return
        val requested = requestedCollectionId ?: publicId
        when {
            currentState.page == CollectionsPage.Preview && requested != null -> read(requested)
            currentState.page == CollectionsPage.Links -> loadLinks()
            currentState.signedIn -> loadFavorites()
        }
    }

    private fun launchWork(epoch: Int, block: suspend () -> Unit) {
        work = viewModelScope.launch {
            try { block() }
            catch (cancelled: CancellationException) {
                if (epoch == generation && currentState.loading) updateState { copy(failure = CollectionsFailure.Load) }
                throw cancelled
            }
            finally { if (epoch == generation) updateState { copy(busy = false, loading = false) } }
        }
    }

    private fun <T> Result<T>.checked(): Result<T> = also {
        exceptionOrNull()?.let { if (it is CancellationException) throw it }
    }
}
