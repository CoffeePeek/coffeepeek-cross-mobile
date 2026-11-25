package com.coffeepeek.feature.collections.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.collections.api.*
import com.coffeepeek.feature.collections.domain.model.SavedCollectionSummary
import com.coffeepeek.feature.collections.domain.model.SavedCollectionVisibility
import com.coffeepeek.feature.collections.domain.repository.CollectionsAccount
import com.coffeepeek.feature.collections.domain.repository.CollectionsRepository
import com.coffeepeek.feature.collections.domain.repository.SavedCollectionsRepository
import com.coffeepeek.feature.collections.impl.ui.SavedCollectionsViewModel
import com.coffeepeek.feature.collections.impl.ui.compose.component.defaultShopPhotoUrl
import com.coffeepeek.feature.collections.impl.ui.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

fun createCollectionsLibrarySession(
    saved: SavedCollectionsRepository,
    snapshots: CollectionsRepository,
    account: CollectionsAccount,
): CollectionsLibrarySession {
    return CollectionsLibrarySessionImpl(saved, snapshots, account)
}

private class CollectionsLibrarySessionImpl(
    private val saved: SavedCollectionsRepository,
    snapshots: CollectionsRepository,
    private val account: CollectionsAccount,
) : CollectionsLibrarySession {
    private val model = SavedCollectionsViewModel(saved, snapshots, account, SavedCollectionsMode.Library)
    private val store = ViewModelStore().apply { put("library", model) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val covers = mutableSetOf<Job>()
    private var photoUrls = mutableMapOf<String, String>()
    private var closed = false

    init {
        scope.launch {
            var version = model.state.value.accountVersion
            model.state.collect {
                if (version != it.accountVersion) {
                    version = it.accountVersion
                    clearPhotoUrls()
                    covers.toList().forEach { job -> job.cancel() }
                    covers.clear()
                }
            }
        }
    }

    override fun current(): CollectionsLibrarySnapshot = model.state.value.toLibrarySnapshot()

    override fun observe(onState: (CollectionsLibrarySnapshot) -> Unit,
        onEvent: (CollectionsLibraryEvent) -> Unit): CollectionsLibraryObservation {
        val job = scope.launch {
            launch { model.state.collect { onState(it.toLibrarySnapshot()) } }
            launch { model.events.collect { event -> event.toLibraryEvent()?.let(onEvent) } }
        }
        return CollectionsLibraryObservation { job.cancel() }
    }

    override fun reload() = dispatch(SavedCollectionsAction.Reload)
    override fun query(value: String) = dispatch(SavedCollectionsAction.Query(value))
    override fun signIn() = dispatch(SavedCollectionsAction.SignIn)
    override fun back() = dispatch(SavedCollectionsAction.Back)

    override fun openEditor(id: String?) {
        if (!current().signedIn || id != null && current().items.none { it.id == id }) return
        dispatch(SavedCollectionsAction.OpenEditor(id))
    }

    private fun dispatch(action: SavedCollectionsAction) {
        if (!closed) model.onAction(action)
    }

    override fun loadCover(id: String, accountVersion: Int,
        onCover: (ByteArray?, String?) -> Unit): CollectionsLibraryObservation {
        val owner = account.currentId()
        val summary = model.state.value.lists.firstOrNull { it.id == id }
        val job = scope.launch {
            if (summary == null || owner == null || accountVersion != current().accountVersion) return@launch
            deliverCover(summary, owner, accountVersion, onCover)
        }
        covers.add(job)
        job.invokeOnCompletion { covers.remove(job) }
        return CollectionsLibraryObservation { job.cancel() }
    }

    private suspend fun deliverCover(summary: SavedCollectionSummary, owner: String, version: Int,
        onCover: (ByteArray?, String?) -> Unit) {
        var bytes: ByteArray? = null
        try {
            val cover = summary.cover
            val url = if (cover == null) defaultShopPhotoUrl(summary, saved, photoUrls) else null
            if (cover != null) bytes = saved.cover(summary.id, cover).getOrThrow()
            currentCoroutineContext().ensureActive()
            if (owner == account.currentId() && version == current().accountVersion) onCover(bytes, url)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (owner == account.currentId() && version == current().accountVersion) onCover(null, null)
        } finally {
            bytes?.fill(0)
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        clearPhotoUrls()
        scope.cancel()
        store.clear()
    }

    private fun clearPhotoUrls() {
        photoUrls.clear()
        // Late work can only populate the retired account's detached map.
        photoUrls = mutableMapOf()
    }
}

private fun SavedCollectionsState.toLibrarySnapshot() = CollectionsLibrarySnapshot(
    accountVersion = accountVersion,
    signedIn = signedIn,
    loading = loading,
    refreshing = refreshing,
    failed = failure != null,
    query = query,
    items = lists.map { it.libraryItem(accountVersion) },
    matchingItems = lists.filter { matchesCollectionQuery(it.title, query) }.map { it.libraryItem(accountVersion) },
)

private fun SavedCollectionSummary.libraryItem(version: Int) = CollectionsLibraryItem(
    id, title, visibility == SavedCollectionVisibility.Public, itemCount,
    "$version:$id:$revision:$itemCount:$cover",
)

private fun SavedCollectionsEvent.toLibraryEvent(): CollectionsLibraryEvent? = when (this) {
    SavedCollectionsEvent.Back -> CollectionsLibraryEvent.Back
    SavedCollectionsEvent.SignIn -> CollectionsLibraryEvent.SignIn
    is SavedCollectionsEvent.Editor -> CollectionsLibraryEvent.Editor(id)
    else -> null
}
