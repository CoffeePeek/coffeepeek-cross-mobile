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
    MviViewModel<SavedCollectionsState, SavedCollectionsAction, SavedCollectionsEvent>(SavedCollectionsState(mode)) {
    private var generation = 0
    private var work: Job? = null

    init {
        if (mode == SavedCollectionsMode.Public || mode == SavedCollectionsMode.Featured) reload()
        else observeAccount()
    }

    override suspend fun handleActionInternal(action: SavedCollectionsAction) {
        when (action) {
            SavedCollectionsAction.Reload -> if (!currentState.busy && !currentState.loading) reload()
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
                generation++
                work?.cancel()
                clearCover()
                sendEvent(SavedCollectionsEvent.CancelExport)
                updateState { SavedCollectionsState(mode, accountVersion = generation, signedIn = owner != null, loading = owner != null) }
                if (owner != null) reload()
            }
        }
        viewModelScope.launch {
            try { account.loadId() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { updateState { copy(loading = false, failure = SavedCollectionsFailure.Load) } }
        }
    }

    private fun reload() {
        if (mode !in setOf(SavedCollectionsMode.Public, SavedCollectionsMode.Featured) && !currentState.signedIn) return
        val epoch = generation
        work?.cancel()
        updateState { copy(loading = true, failure = null, membershipsLoaded = false,
            collection = if (mode == SavedCollectionsMode.Featured) null else collection) }
        launchWork(epoch) {
            when (mode) {
                SavedCollectionsMode.Library, SavedCollectionsMode.Picker -> loadLists(epoch)
                SavedCollectionsMode.Preference -> loadPreference(epoch)
                SavedCollectionsMode.Editor -> loadEditor(epoch)
                SavedCollectionsMode.Public -> loadPublic(epoch)
                SavedCollectionsMode.Featured -> loadFeatured(epoch)
            }
        }
    }

    private suspend fun loadLists(epoch: Int) {
        val result = saved.mine(target).checked()
        if (epoch != generation) return
        updateState { copy(lists = result.getOrNull() ?: lists, memberships = result.getOrNull()?.filter { it.containsItem }?.map { it.id }?.toSet() ?: memberships,
            membershipsLoaded = result.isSuccess, failure = if (result.isFailure) SavedCollectionsFailure.Load else null) }
    }

    private suspend fun loadPreference(epoch: Int) {
        val lists = saved.mine().checked()
        val featured = saved.featuredId().checked()
        if (epoch != generation) return
        updateState { copy(lists = lists.getOrNull() ?: this.lists, featuredId = featured.getOrNull(),
            failure = if (lists.isFailure || featured.isFailure) SavedCollectionsFailure.Load else null) }
    }

    private suspend fun loadEditor(epoch: Int) {
        val id = currentState.collection?.summary?.id ?: initialId
        val detail = id?.let { saved.owned(it).checked() }
        val favorites = snapshots.favorites().checked()
        if (epoch != generation) return
        if (detail?.isFailure == true) updateState { copy(failure = SavedCollectionsFailure.Load) }
        else {
            val value = detail?.getOrNull()
            clearCover()
            updateState { copy(collection = value, title = value?.summary?.title.orEmpty(), description = value?.summary?.description.orEmpty(),
                visibility = value?.summary?.visibility ?: SavedCollectionVisibility.Private,
                places = CollectionRules.orderedPlaces((value?.places.orEmpty() + favorites.getOrNull().orEmpty()).distinctBy { it.key }),
                selectedPlaces = value?.places.orEmpty().map { it.key }.toSet(), coverInput = null, uploadedCover = null, removeCover = false,
                failure = if (favorites.isFailure) SavedCollectionsFailure.Load else null) }
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
        updateState { copy(busy = true, failure = null) }
        launchWork(epoch) {
            if (!ensureCreated(epoch)) return@launchWork
            val result = saveMetadata(epoch).checked()
            if (epoch != generation) return@launchWork
            val collection = result.getOrNull()
            if (collection == null) updateState { copy(failure = failureFor(result.exceptionOrNull())) }
            else saveItems(epoch, collection)
        }
    }

    private suspend fun ensureCreated(epoch: Int): Boolean {
        if (currentState.collection != null) return true
        val result = saved.create(metadata().copy(coverUpload = null, removeCover = false)).checked()
        if (epoch != generation) throw CancellationException("Collection account changed")
        updateState { copy(collection = result.getOrNull(), failure = result.exceptionOrNull()?.let(::failureFor)) }
        return result.isSuccess
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
        return saved.update(checkNotNull(existing).id, metadata(), existing.revision)
    }

    private suspend fun saveItems(epoch: Int, collection: SavedCollection) {
        val selected = currentState.places.filter { it.key in currentState.selectedPlaces }
        clearCover()
        updateState { copy(collection = collection, title = collection.summary.title, description = collection.summary.description,
            visibility = collection.summary.visibility, coverInput = null, uploadedCover = null, removeCover = false) }
        val result = if (selected.map { it.key } == collection.places.map { it.key }) Result.success(collection)
            else saved.replaceItems(collection.summary.id, selected, collection.summary.revision).checked()
        if (epoch != generation) return
        updateState { copy(collection = result.getOrNull() ?: collection, failure = result.exceptionOrNull()?.let(::failureFor)) }
    }

    private fun metadata() = SavedCollectionMetadata(currentState.title, currentState.description,
        currentState.visibility, currentState.uploadedCover, currentState.removeCover)

    private fun toggleMembership(id: String) {
        if (!writable() || !currentState.membershipsLoaded || currentState.lists.none { it.id == id }) return
        updateState { copy(memberships = if (id in memberships) memberships - id else memberships + id) }
    }

    private fun commitMemberships() {
        if (!writable() || target == null || !currentState.membershipsLoaded) return
        val epoch = generation
        updateState { copy(busy = true, failure = null) }
        launchWork(epoch) {
            val result = saved.memberships(target, currentState.memberships).checked()
            if (epoch != generation) return@launchWork
            if (result.isSuccess) sendEvent(SavedCollectionsEvent.Back)
            else updateState { copy(failure = SavedCollectionsFailure.Membership) }
        }
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
            finally { if (epoch == generation) updateState { copy(busy = false, loading = false) } }
        }
    }

    private fun <T> Result<T>.checked(): Result<T> = also {
        exceptionOrNull()?.let { if (it is CancellationException) throw it }
    }

    override fun onCleared() {
        clearCover()
        super.onCleared()
    }
}
