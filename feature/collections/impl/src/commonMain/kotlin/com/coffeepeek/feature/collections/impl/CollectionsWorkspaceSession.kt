package com.coffeepeek.feature.collections.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.collections.api.CollectionsLibraryObservation
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.domain.repository.*
import com.coffeepeek.feature.collections.impl.ui.SavedCollectionsViewModel
import com.coffeepeek.feature.collections.impl.ui.compose.component.defaultShopPhotoUrl
import com.coffeepeek.feature.collections.impl.ui.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest

/** Platform-independent screen session. All writes and conflict handling remain in the shared VM. */
class CollectionsWorkspaceSession(
    private val saved: SavedCollectionsRepository,
    private val snapshots: CollectionsRepository,
    private val account: CollectionsAccount,
    private val mode: CollectionsWorkspaceMode,
    id: String?, profileSlug: String?, target: SavedCollectionTarget?,
    private val onBack: () -> Unit, private val onSignIn: () -> Unit,
    private val onEditor: (String?) -> Unit, private val onCollection: (String) -> Unit,
    private val onShop: (String) -> Unit, private val onRoaster: (String) -> Unit,
    private val onShare: (String) -> Unit,
    private val onSaved: () -> Unit,
) {
    private val model = SavedCollectionsViewModel(saved, snapshots, account, mode.savedMode(), id, target, profileSlug)
    private val store = ViewModelStore().apply { put("workspace", model) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var closed = false
    private var export: ((CollectionsPoster, Int) -> Unit)? = null
    private val jobs = mutableSetOf<Job>()
    private var photoUrls = mutableMapOf<String, String>()

    init {
        scope.launch { model.events.collectLatest(::handleEvent) }
        scope.launch { observeAccountVersion() }
    }

    fun current(): CollectionsWorkspaceState = visibleState().nativeState(mode)

    fun observe(onState: () -> Unit, onExport: (CollectionsPoster, Int) -> Unit): CollectionsLibraryObservation {
        export = onExport
        val job = scope.launch { model.state.collect { onState() } }
        return CollectionsLibraryObservation { job.cancel(); if (export === onExport) export = null }
    }

    fun reload() = dispatch(SavedCollectionsAction.Reload)
    fun back() = dispatch(SavedCollectionsAction.Back)
    fun signIn() = dispatch(SavedCollectionsAction.SignIn)
    fun title(value: String) = dispatch(SavedCollectionsAction.Title(value))
    fun description(value: String) = dispatch(SavedCollectionsAction.Description(value))
    fun visibility(value: Boolean) = dispatch(SavedCollectionsAction.Visibility(if (value) SavedCollectionVisibility.Public else SavedCollectionVisibility.Private))
    fun save() = dispatch(SavedCollectionsAction.Save)
    fun delete() = dispatch(SavedCollectionsAction.Delete)
    fun removeCover() = dispatch(SavedCollectionsAction.RemoveCover)
    fun feature(id: String?) = dispatch(SavedCollectionsAction.Feature(id))
    fun togglePlace(key: String) = dispatch(SavedCollectionsAction.TogglePlace(key))
    fun movePlace(key: String, target: String, after: Boolean) = dispatch(SavedCollectionsAction.MovePlace(key, target, after))
    fun preview() { if (current().canPreview) dispatch(SavedCollectionsAction.Preview) }
    fun shareLink() { if (current().canShare) dispatch(SavedCollectionsAction.ShareLink) }
    fun shareImages() { if (poster() != null) dispatch(SavedCollectionsAction.ShareImages) }
    fun format(story: Boolean) = dispatch(SavedCollectionsAction.Format(if (story) CollectionPosterFormat.Story else CollectionPosterFormat.Chat))
    fun query(value: String) = dispatch(SavedCollectionsAction.Query(value))
    fun quickTitle(value: String?) = dispatch(SavedCollectionsAction.QuickTitle(value))
    fun createQuick() = dispatch(SavedCollectionsAction.CreateQuick)
    fun toggleMembership(id: String) = dispatch(SavedCollectionsAction.ToggleMembership(id))
    fun commitMemberships() = dispatch(SavedCollectionsAction.CommitMemberships)

    fun openCollection(id: String) {
        if (mode == CollectionsWorkspaceMode.Featured && current().id == id) onCollection(id)
        else if (current().signedIn && current().lists.any { it.id == id }) onEditor(id)
    }

    fun openPlace(key: String) {
        visibleState().collection?.places?.firstOrNull { it.key == key }?.let {
            dispatch(SavedCollectionsAction.OpenPlace(it))
        }
    }

    fun coverPreparing(value: Boolean, version: Int) = dispatch(SavedCollectionsAction.CoverPreparing(value, version))
    fun coverPicked(bytes: ByteArray, version: Int) = dispatch(SavedCollectionsAction.CoverPicked(
        CollectionCoverInput(bytes.copyOf(), "image/jpeg", "cover.jpg"), version))

    fun poster(): CollectionsPoster? = visibleState().let { it.collection?.toSharedPoster()?.nativePoster(it.format) }

    fun isCurrentExport(epoch: Int, id: String): Boolean = !closed && model.isCurrentExport(epoch, id)
    fun finishExport(epoch: Int, failed: Boolean) = dispatch(SavedCollectionsAction.ExportFinished(epoch, failed))

    fun loadCover(id: String, version: Int, receive: (ByteArray?, String?) -> Unit): CollectionsLibraryObservation {
        val owner = account.currentId()
        val summary = visibleState().let { state -> state.collection?.summary?.takeIf { it.id == id } ?: state.lists.firstOrNull { it.id == id } }
        val job = scope.launch {
            val draft = model.state.value.coverInput?.bytes?.copyOf()?.takeIf { id == "draft" }
            if (draft != null) {
                try { if (validMedia(owner, version)) receive(draft, null) } finally { draft.fill(0) }
            } else summary?.let { deliverCover(it, owner, version, receive) }
        }
        jobs.add(job)
        job.invokeOnCompletion { jobs.remove(job) }
        return CollectionsLibraryObservation { job.cancel() }
    }

    fun loadPhoto(url: String, receive: (ByteArray?) -> Unit): CollectionsLibraryObservation {
        val version = current().accountVersion
        val job = scope.launch {
            var bytes: ByteArray? = null
            try {
                bytes = snapshots.photo(url)
                currentCoroutineContext().ensureActive()
                if (!closed && version == current().accountVersion) receive(bytes)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (!closed && version == current().accountVersion) receive(null) }
            finally { bytes?.fill(0) }
        }
        jobs.add(job)
        job.invokeOnCompletion { jobs.remove(job) }
        return CollectionsLibraryObservation { job.cancel() }
    }

    fun close() {
        if (closed) return
        closed = true
        export = null
        photoUrls.clear()
        scope.cancel()
        store.clear()
    }

    private fun visibleState(): SavedCollectionsState {
        val state = model.state.value
        return if (mode in publicModes && state.collection?.summary?.visibility != SavedCollectionVisibility.Public)
            state.copy(collection = null) else state
    }

    private fun dispatch(action: SavedCollectionsAction) { if (!closed) model.onAction(action) }

    private suspend fun observeAccountVersion() {
        var version = model.state.value.accountVersion
        model.state.collect {
            if (version != it.accountVersion) {
                version = it.accountVersion
                jobs.toList().forEach(Job::cancel)
                photoUrls.clear()
                photoUrls = mutableMapOf()
            }
        }
    }

    private suspend fun handleEvent(event: SavedCollectionsEvent) {
        when (event) {
            SavedCollectionsEvent.Back -> onBack()
            SavedCollectionsEvent.SignIn -> onSignIn()
            SavedCollectionsEvent.CancelExport -> Unit
            is SavedCollectionsEvent.Saved -> if (model.isCurrentSave(event.epoch)) onSaved()
            is SavedCollectionsEvent.Editor -> onEditor(event.id)
            is SavedCollectionsEvent.Place -> if (event.value.kind == CollectionPlaceKind.CoffeeShop) onShop(event.value.slug) else onRoaster(event.value.slug)
            is SavedCollectionsEvent.Share -> onShare(event.value)
            is SavedCollectionsEvent.Export -> export?.invoke(event.value.nativePoster(event.format), event.epoch)
                ?: finishExport(event.epoch, true)
        }
    }

    private suspend fun deliverCover(summary: SavedCollectionSummary, owner: String?, version: Int,
        receive: (ByteArray?, String?) -> Unit) {
        var bytes: ByteArray? = null
        try {
            val state = model.state.value
            val input = state.coverInput?.takeIf { summary.id == state.collection?.summary?.id }
            val cover = summary.cover?.takeUnless { state.removeCover && summary.id == state.collection?.summary?.id }
            if (input != null) bytes = input.bytes.copyOf()
            else if (cover != null && (!cover.authenticated || mode !in publicModes && owner != null)) bytes = saved.cover(summary.id, cover).getOrThrow()
            val url = if (input == null && cover == null) defaultPhoto(summary) else null
            currentCoroutineContext().ensureActive()
            if (validMedia(owner, version)) receive(bytes, url)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { if (validMedia(owner, version)) receive(null, null) }
        finally { bytes?.fill(0) }
    }

    private fun validMedia(owner: String?, version: Int): Boolean = !closed &&
        version == current().accountVersion && (mode in publicModes || owner != null && owner == account.currentId())

    private suspend fun defaultPhoto(summary: SavedCollectionSummary): String? {
        val loaded = model.state.value.collection?.takeIf { it.summary.id == summary.id }
        if (loaded != null) return loaded.places.firstOrNull { it.kind == CollectionPlaceKind.CoffeeShop }?.photoUrl
        return defaultShopPhotoUrl(summary, saved, photoUrls)
    }
}

private val publicModes = setOf(CollectionsWorkspaceMode.Public, CollectionsWorkspaceMode.Featured)

private fun CollectionsWorkspaceMode.savedMode(): SavedCollectionsMode = when (this) {
    CollectionsWorkspaceMode.Editor -> SavedCollectionsMode.Editor
    CollectionsWorkspaceMode.Owner -> SavedCollectionsMode.Library
    CollectionsWorkspaceMode.Preference -> SavedCollectionsMode.Preference
    CollectionsWorkspaceMode.Featured -> SavedCollectionsMode.Featured
    CollectionsWorkspaceMode.Public -> SavedCollectionsMode.Public
    CollectionsWorkspaceMode.Picker -> SavedCollectionsMode.Picker
    CollectionsWorkspaceMode.Snapshot -> error("Snapshot uses its public read session")
}

private fun SavedCollectionsState.nativeState(mode: CollectionsWorkspaceMode): CollectionsWorkspaceState {
    val summary = collection?.summary
    val edits = hasEdits()
    return CollectionsWorkspaceState(mode.name, accountVersion, signedIn, loading, refreshing, busy, failure?.name,
        summary?.id, if (mode == CollectionsWorkspaceMode.Editor) title else summary?.title.orEmpty(),
        if (mode == CollectionsWorkspaceMode.Editor) description else summary?.description.orEmpty(),
        visibility == SavedCollectionVisibility.Public, edits,
        signedIn && !loading && !busy && !preparingCover && SavedCollectionRules.validMetadata(SavedCollectionMetadata(title, description)),
        !edits && summary?.visibility == SavedCollectionVisibility.Public && summary.url != null || mode == CollectionsWorkspaceMode.Public && summary?.url != null,
        !busy && collection?.places?.isNotEmpty() == true && (mode == CollectionsWorkspaceMode.Public || !edits),
        preparingCover, coverInput != null || summary?.cover != null && !removeCover, if (summary == null) coverInput?.let { "draft:${it.hashCode()}" } else summary.let { "$accountVersion:${it.id}:${it.revision}:${it.cover}:$removeCover:${coverInput?.hashCode()}" },
        preview, format == CollectionPosterFormat.Story, featuredId,
        lists.map { CollectionsWorkspaceList(it.id, it.title, it.visibility == SavedCollectionVisibility.Public,
            it.itemCount, "$accountVersion:${it.id}:${it.revision}:${it.cover}", it.id in memberships) },
        (if (mode == CollectionsWorkspaceMode.Editor) places else collection?.places.orEmpty()).map { it.nativePlace(it.key in selectedPlaces) },
        query, quickTitle, membershipsLoaded,
        signedIn && !loading && !busy && membershipsLoaded && CollectionRules.validTitle(quickTitle.orEmpty()))
}
