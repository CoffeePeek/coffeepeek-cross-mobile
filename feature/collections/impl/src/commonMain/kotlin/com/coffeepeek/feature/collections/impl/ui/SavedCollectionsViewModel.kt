package com.coffeepeek.feature.collections.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.domain.repository.*
import com.coffeepeek.feature.collections.impl.ui.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

internal class SavedCollectionsViewModel(private val saved: SavedCollectionsRepository,
    private val snapshots: CollectionsRepository, private val account: CollectionsAccount,
    private val mode: SavedCollectionsMode, private val initialId: String? = null,
    private val target: SavedCollectionTarget? = null, private val profileSlug: String? = null) :
    MviViewModel<SavedCollectionsState, SavedCollectionsAction, SavedCollectionsEvent>(initialSavedState(saved, account, mode, target)) {
    private var generation = 0
    private var work: Job? = null
    private var reloadPending = false

    init {
        observeChanges()
        if (mode == SavedCollectionsMode.Public || mode == SavedCollectionsMode.Featured) reload()
        else observeAccount()
    }

    private fun observeChanges() {
        if (mode !in setOf(SavedCollectionsMode.Library, SavedCollectionsMode.Preference)) return
        viewModelScope.launch {
            saved.observeChanges().collect {
                if (currentState.busy || currentState.loading || currentState.refreshing) reloadPending = true else reload(force = true)
            }
        }
    }

    override suspend fun handleActionInternal(action: SavedCollectionsAction) {
        when (action) {
            SavedCollectionsAction.Reload -> if (!currentState.busy && !currentState.loading) reload(force = true)
            SavedCollectionsAction.Back -> back()
            SavedCollectionsAction.SignIn -> sendEvent(SavedCollectionsEvent.SignIn)
            SavedCollectionsAction.Save -> save()
            SavedCollectionsAction.Delete -> delete()
            SavedCollectionsAction.CommitMemberships -> commitMemberships()
            is SavedCollectionsAction.ToggleMembership -> toggleMembership(action.id)
            is SavedCollectionsAction.Feature -> feature(action.id)
            SavedCollectionsAction.CreateQuick -> createQuick()
            is SavedCollectionsAction.MovePlace -> movePlace(action)
            else -> navigationOrEdit(action)
        }
    }

    private suspend fun navigationOrEdit(action: SavedCollectionsAction) {
        when (action) {
            SavedCollectionsAction.ShareLink -> shareLink()
            SavedCollectionsAction.Preview -> if (!currentState.busy && (mode == SavedCollectionsMode.Public || !currentState.hasEdits())) updateState { copy(preview = true) }
            SavedCollectionsAction.ShareImages -> export()
            is SavedCollectionsAction.ExportFinished -> if (action.epoch == generation) updateState { copy(busy = false, failure = if (action.failed) SavedCollectionsFailure.Save else null) }
            is SavedCollectionsAction.OpenEditor -> sendEvent(SavedCollectionsEvent.Editor(action.id))
            is SavedCollectionsAction.OpenPlace -> sendEvent(SavedCollectionsEvent.Place(action.place))
            else -> edit(action)
        }
    }

    private fun observeAccount() {
        viewModelScope.launch {
            account.identities.distinctUntilChanged().collectLatest { owner ->
                resetAccount(owner)
                if (owner != null && !(mode == SavedCollectionsMode.Picker && currentState.membershipsLoaded)) reload()
            }
        }
        if (mode == SavedCollectionsMode.Picker && currentState.membershipsLoaded) return
        viewModelScope.launch {
            try { account.loadId() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { updateState { copy(loading = false, failure = SavedCollectionsFailure.Load) } }
        }
    }

    private suspend fun resetAccount(owner: String?) {
        generation++
        reloadPending = false
        work?.cancel()
        clearCover()
        sendEvent(SavedCollectionsEvent.CancelExport)
        val fresh = if (mode == SavedCollectionsMode.Picker && owner != null && owner == account.currentId()) saved.cachedFreshMine(target) else null
        val cached = fresh ?: if (mode in cachedListModes) saved.cachedMine(target)
            .takeIf { owner != null && owner == account.currentId() } else null
        val featured = if (mode == SavedCollectionsMode.Preference && owner != null && owner == account.currentId())
            saved.cachedFeatured() else null
        val ready = cached != null && (mode != SavedCollectionsMode.Preference || featured != null)
        updateState { SavedCollectionsState(mode, accountVersion = generation, signedIn = owner != null,
            loading = owner != null && !ready, refreshing = ready && fresh == null,
            listsLoaded = ready, lists = cached.orEmpty(), featuredId = featured?.collectionId,
            memberships = cached?.filter { it.containsItem }?.map { it.id }?.toSet().orEmpty(), membershipsLoaded = fresh != null) }
    }

    private fun reload(force: Boolean = false) {
        if (mode !in setOf(SavedCollectionsMode.Public, SavedCollectionsMode.Featured) && !currentState.signedIn) return
        val epoch = generation
        reloadPending = false
        work?.cancel()
        val cached = mode in cachedListModes && currentState.listsLoaded
        updateState { copy(loading = !cached, refreshing = cached, failure = null, membershipsLoaded = false,
            collection = if (mode == SavedCollectionsMode.Featured) null else collection) }
        launchWork(epoch) {
            when (mode) {
                SavedCollectionsMode.Library, SavedCollectionsMode.Picker -> loadLists(epoch, force)
                SavedCollectionsMode.Preference -> loadPreference(epoch, force)
                SavedCollectionsMode.Editor -> loadEditor(epoch)
                SavedCollectionsMode.Public -> loadPublic(epoch)
                SavedCollectionsMode.Featured -> loadFeatured(epoch)
            }
        }
    }

    private suspend fun loadLists(epoch: Int, force: Boolean) {
        val result = (if (force) saved.refreshMine(target) else saved.mine(target)).checked()
        if (epoch != generation) return
        updateState { copy(lists = result.getOrNull() ?: lists, memberships = result.getOrNull()?.filter { it.containsItem }?.map { it.id }?.toSet() ?: memberships,
            listsLoaded = result.isSuccess || listsLoaded, membershipsLoaded = result.isSuccess,
            failure = if (result.isFailure) SavedCollectionsFailure.Load else null) }
    }

    private suspend fun loadPreference(epoch: Int, force: Boolean) {
        val lists = (if (force) saved.refreshMine() else saved.mine()).checked()
        val featured = saved.featuredId().checked()
        if (epoch != generation) return
        val complete = lists.isSuccess && featured.isSuccess
        updateState { copy(lists = if (complete) lists.getOrThrow() else this.lists,
            listsLoaded = complete || listsLoaded,
            featuredId = if (complete) featured.getOrNull() else featuredId,
            failure = if (complete) null else SavedCollectionsFailure.Load) }
    }

    private suspend fun loadEditor(epoch: Int) {
        val id = currentState.collection?.summary?.id ?: initialId
        val detail = id?.let { saved.owned(it).checked() }
        if (epoch != generation) return
        if (detail?.isFailure == true) updateState { copy(failure = SavedCollectionsFailure.Load) }
        else {
            val value = detail?.getOrNull()
            clearCover()
            updateState { copy(collection = value, title = value?.summary?.title.orEmpty(), description = value?.summary?.description.orEmpty(),
                visibility = value?.summary?.visibility ?: SavedCollectionVisibility.Private,
                places = value?.places.orEmpty(),
                selectedPlaces = value?.places.orEmpty().map { it.key }.toSet(), coverInput = null, uploadedCover = null, removeCover = false,
                failure = null) }
        }
    }

    private suspend fun loadPublic(epoch: Int) {
        val result = saved.public(checkNotNull(initialId)).checked()
        if (epoch == generation) updateState { copy(collection = result.getOrNull(), failure = if (result.isFailure) SavedCollectionsFailure.Load else null) }
    }

    private suspend fun loadFeatured(epoch: Int) {
        val result = saved.featured(checkNotNull(profileSlug)).checked()
        if (epoch == generation) updateState { copy(collection = result.getOrNull(), failure = if (result.isFailure) SavedCollectionsFailure.Load else null) }
    }

    private fun edit(action: SavedCollectionsAction) {
        if (currentState.busy) return
        when (action) {
            is SavedCollectionsAction.Title -> updateState { copy(title = action.value.take(CollectionRules.maxTitleLength), failure = null) }
            is SavedCollectionsAction.Description -> updateState { copy(description = action.value.take(SavedCollectionRules.maxDescriptionLength), failure = null) }
            is SavedCollectionsAction.Visibility -> updateState { copy(visibility = action.value, failure = null) }
            is SavedCollectionsAction.Query -> updateState { copy(query = action.value) }
            is SavedCollectionsAction.CoverPicked -> if (action.accountVersion == currentState.accountVersion && currentState.signedIn) pickCover(action.value)
            is SavedCollectionsAction.CoverPreparing -> if (action.accountVersion == currentState.accountVersion && currentState.signedIn)
                updateState { copy(preparingCover = action.value) }
            SavedCollectionsAction.RemoveCover -> removeCover()
            is SavedCollectionsAction.TogglePlace -> if (currentState.places.any { it.key == action.key }) updateState {
                copy(selectedPlaces = if (action.key in selectedPlaces) selectedPlaces - action.key else selectedPlaces + action.key)
            }
            is SavedCollectionsAction.QuickTitle -> quickTitle(action.value)
            is SavedCollectionsAction.Format -> updateState { copy(format = action.value) }
            else -> Unit
        }
    }

    private fun pickCover(input: CollectionCoverInput) {
        if (!SavedCollectionRules.validCover(input)) {
            updateState { copy(failure = SavedCollectionsFailure.Cover) }
            return
        }
        clearCover()
        updateState { copy(coverInput = input.copy(bytes = input.bytes.copyOf()), uploadedCover = null, removeCover = false, failure = null) }
    }

    private fun removeCover() {
        clearCover()
        updateState { copy(coverInput = null, uploadedCover = null, removeCover = true) }
    }

    private fun movePlace(action: SavedCollectionsAction.MovePlace) {
        if (currentState.busy || mode != SavedCollectionsMode.Editor) return
        updateState { copy(places = moveCollectionPlace(places, action.key, action.target, action.after)) }
    }

    private fun save() {
        if (!writable() || mode != SavedCollectionsMode.Editor || !SavedCollectionRules.validMetadata(metadata())) return
        val epoch = generation
        val editedPlaces = editedPlaces()
        updateState { copy(busy = true, failure = null) }
        launchWork(epoch) {
            val result = saveMetadata(epoch).checked()
            if (epoch != generation) return@launchWork
            val collection = result.getOrNull()
            if (collection == null) updateState { copy(failure = failureFor(result.exceptionOrNull())) }
            else saveItems(epoch, collection, editedPlaces)
        }
    }

    private fun editedPlaces(): List<CollectionPlace>? {
        val original = currentState.collection ?: return null
        val selected = currentState.places.filter { it.key in currentState.selectedPlaces }
        return selected.takeUnless { it.map { place -> place.key } == original.places.map { place -> place.key } }
    }

    private suspend fun saveMetadata(epoch: Int): Result<SavedCollection> {
        val input = currentState.coverInput
        if (input != null && currentState.uploadedCover == null) {
            val upload = saved.uploadCover(input).checked()
            if (epoch != generation) throw CancellationException("Collection account changed")
            if (upload.isFailure) return Result.failure(checkNotNull(upload.exceptionOrNull()))
            updateState { copy(uploadedCover = upload.getOrThrow()) }
        }
        val existing = currentState.collection?.summary
        return if (existing == null) saved.create(metadata())
            else saved.update(existing.id, metadata(), existing.revision)
    }

    private suspend fun saveItems(epoch: Int, collection: SavedCollection, editedPlaces: List<CollectionPlace>?) {
        clearCover()
        updateState { copy(collection = collection, title = collection.summary.title, description = collection.summary.description,
            visibility = collection.summary.visibility, coverInput = null, uploadedCover = null, removeCover = false) }
        val result = if (editedPlaces == null) Result.success(collection)
            else saved.replaceItems(collection.summary.id, editedPlaces, collection.summary.revision).checked()
        if (epoch != generation) return
        updateState {
            val value = result.getOrNull()
            copy(collection = value ?: collection, places = value?.places ?: places,
                selectedPlaces = value?.places?.map { it.key }?.toSet() ?: selectedPlaces,
                failure = result.exceptionOrNull()?.let(::failureFor))
        }
    }

    private fun metadata() = SavedCollectionMetadata(currentState.title, currentState.description,
        currentState.visibility, currentState.uploadedCover, currentState.removeCover)

    private fun toggleMembership(id: String) {
        if (!writable() || !currentState.membershipsLoaded || currentState.lists.none { it.id == id }) return
        updateState { copy(memberships = if (id in memberships) memberships - id else memberships + id) }
    }

    private suspend fun commitMemberships() {
        if (!writable() || target == null || !currentState.membershipsLoaded) return
        val initial = currentState.lists.filter { it.containsItem }.map { it.id }.toSet()
        val added = currentState.memberships - initial
        val removed = initial - currentState.memberships
        if (added.isEmpty() && removed.isEmpty()) return sendEvent(SavedCollectionsEvent.Back)
        val epoch = generation
        updateState { copy(busy = true, failure = null) }
        launchWork(epoch) {
            val result = saved.changeMemberships(target, added, removed).checked()
            if (epoch != generation) return@launchWork
            if (result.isSuccess) {
                updateMemberships(result.getOrThrow().lists)
                sendEvent(SavedCollectionsEvent.Back)
            }
            else updateState { copy(failure = SavedCollectionsFailure.Membership) }
        }
    }

    private fun updateMemberships(lists: List<SavedCollectionSummary>) = updateState {
        copy(lists = lists, memberships = lists.filter { it.containsItem }.map { it.id }.toSet(), membershipsLoaded = true)
    }

    private fun createQuick() {
        val title = currentState.quickTitle ?: return
        if (!writable() || mode != SavedCollectionsMode.Picker || !currentState.membershipsLoaded || !CollectionRules.validTitle(title)) return
        val epoch = generation
        updateState { copy(busy = true, failure = null) }
        launchWork(epoch) {
            val result = saved.create(SavedCollectionMetadata(title)).checked()
            if (epoch != generation) return@launchWork
            updateState { result.getOrNull()?.let { value -> copy(lists = lists + value.summary, memberships = memberships + value.summary.id, quickTitle = null) }
                ?: copy(failure = SavedCollectionsFailure.Save) }
        }
    }

    private fun quickTitle(value: String?) {
        if (value != null && (!writable() || mode != SavedCollectionsMode.Picker || !currentState.membershipsLoaded)) return
        updateState { copy(quickTitle = value?.take(CollectionRules.maxTitleLength)) }
    }

    private fun feature(id: String?) {
        if (!writable() || mode != SavedCollectionsMode.Preference || currentState.failure == SavedCollectionsFailure.Load ||
            id != null && currentState.lists.none { it.id == id && it.visibility == SavedCollectionVisibility.Public }) return
        val epoch = generation
        updateState { copy(busy = true, failure = null) }
        launchWork(epoch) {
            val result = saved.setFeatured(id).checked()
            if (epoch == generation) updateState { copy(featuredId = if (result.isSuccess) id else featuredId,
                failure = if (result.isFailure) SavedCollectionsFailure.Save else null) }
        }
    }

    private fun delete() {
        val id = currentState.collection?.summary?.id ?: return
        if (!writable()) return
        val epoch = generation
        updateState { copy(busy = true, failure = null) }
        launchWork(epoch) {
            val result = saved.delete(id).checked()
            if (epoch != generation) return@launchWork
            if (result.isSuccess) sendEvent(SavedCollectionsEvent.Back)
            else updateState { copy(failure = SavedCollectionsFailure.Delete) }
        }
    }

    private suspend fun shareLink() {
        currentState.collection?.summary?.takeIf { it.visibility == SavedCollectionVisibility.Public }?.let { summary ->
            summary.url?.let { sendEvent(SavedCollectionsEvent.Share("${summary.title}\n$it")) }
        }
    }

    private suspend fun export() {
        val collection = currentState.collection ?: return
        if (currentState.busy || collection.places.isEmpty()) return
        updateState { copy(busy = true) }
        sendEvent(SavedCollectionsEvent.Export(collection.toSharedPoster(), currentState.format, generation))
    }

    internal fun isCurrentExport(epoch: Int, id: String): Boolean = epoch == generation &&
        currentState.collection?.summary?.id == id && currentState.busy

    private suspend fun back() {
        generation++
        work?.cancel()
        sendEvent(SavedCollectionsEvent.CancelExport)
        if (currentState.preview) {
            updateState { copy(preview = false, busy = false, accountVersion = generation) }
            return
        }
        clearCover()
        updateState { copy(accountVersion = generation, coverInput = null, uploadedCover = null, preparingCover = false) }
        sendEvent(SavedCollectionsEvent.Back)
    }

    private fun writable(): Boolean = currentState.signedIn && !currentState.busy && !currentState.loading && !currentState.preparingCover &&
        mode !in setOf(SavedCollectionsMode.Public, SavedCollectionsMode.Featured)

    private fun clearCover() { currentState.coverInput?.bytes?.fill(0) }

    private fun failureFor(error: Throwable?): SavedCollectionsFailure =
        if (error is SavedCollectionConflict) SavedCollectionsFailure.Conflict else SavedCollectionsFailure.Save

    private fun launchWork(epoch: Int, block: suspend () -> Unit) {
        work = viewModelScope.launch {
            try { block() }
            catch (cancelled: CancellationException) {
                if (epoch == generation) updateState { copy(failure = when {
                    loading -> SavedCollectionsFailure.Load
                    busy -> SavedCollectionsFailure.Save
                    else -> failure
                }) }
                throw cancelled
            }
            catch (error: Exception) { if (epoch == generation) updateState { copy(failure = failureFor(error)) } }
            finally { finishWork(epoch) }
        }
    }

    private fun finishWork(epoch: Int) {
        if (epoch != generation) return
        updateState { copy(busy = false, loading = false, refreshing = false) }
        if (reloadPending) reload(force = true)
    }

    private fun <T> Result<T>.checked(): Result<T> = also {
        exceptionOrNull()?.let { if (it is CancellationException) throw it }
    }

    override fun onCleared() {
        clearCover()
        super.onCleared()
    }
}

private val cachedListModes = setOf(SavedCollectionsMode.Library, SavedCollectionsMode.Picker, SavedCollectionsMode.Preference)

private fun initialSavedState(saved: SavedCollectionsRepository, account: CollectionsAccount,
    mode: SavedCollectionsMode, target: SavedCollectionTarget?): SavedCollectionsState {
    val owner = account.currentId()
    val fresh = if (owner != null && mode == SavedCollectionsMode.Picker) saved.cachedFreshMine(target) else null
    val cached = fresh ?: if (owner != null && mode in cachedListModes) saved.cachedMine(target) else null
    val featured = if (owner != null && mode == SavedCollectionsMode.Preference) saved.cachedFeatured() else null
    val ready = cached != null && (mode != SavedCollectionsMode.Preference || featured != null)
    return SavedCollectionsState(mode, signedIn = owner != null,
        loading = mode in setOf(SavedCollectionsMode.Public, SavedCollectionsMode.Featured) || owner != null && !ready,
        refreshing = ready && fresh == null, listsLoaded = ready, lists = cached.orEmpty(), featuredId = featured?.collectionId,
        memberships = cached?.filter { it.containsItem }?.map { it.id }?.toSet().orEmpty(), membershipsLoaded = fresh != null)
}
