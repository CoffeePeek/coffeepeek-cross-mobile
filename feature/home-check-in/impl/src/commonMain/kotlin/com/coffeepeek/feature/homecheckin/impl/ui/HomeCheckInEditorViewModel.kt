package com.coffeepeek.feature.homecheckin.impl.ui

import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import com.coffeepeek.feature.homecheckin.domain.usecase.homePreparationFieldErrors
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.*
import com.coffeepeek.feature.homecheckin.impl.ui.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlin.time.Clock
import kotlin.time.Instant

/** One create/edit form and account lease. Composition owns local-date conversion and guarded ports. */
internal class HomeCheckInEditorViewModel(
    private val repository: HomePreparationRepository,
    private val catalog: HomeCheckInEditorCatalog,
    initialPreparedAt: String,
    initialPreparedOn: String,
    private val preparationId: String?,
    private val sessionGeneration: Int,
    private val canAccess: () -> Boolean,
    private val now: () -> Instant = { Clock.System.now() },
    private val analytics: HomeCheckInEditorAnalytics = HomeCheckInEditorAnalytics.None,
    startBean: HomeCheckInStartBean? = null,
) : MviViewModel<HomeCheckInEditorState, HomeCheckInEditorAction, HomeCheckInEditorEventEnvelope>(
    homeCheckInStartState(initialPreparedAt, initialPreparedOn, startBean.takeIf { preparationId == null }),
) {
    private var initialized = false
    private var loadJob: Job? = null
    private var saveJob: Job? = null
    private var methodsJob: Job? = null
    private var beansJob: Job? = null
    private var searchRevision = 0L
    private var revision = 0L

    init { onAction(HomeCheckInEditorAction.Initialize) }

    override suspend fun handleActionInternal(action: HomeCheckInEditorAction) {
        if (action == HomeCheckInEditorAction.RevokeAccess || !canAccess()) {
            revokeAccess()
            return
        }
        if (currentState.isClosed || currentState.isAccessRevoked) return
        dispatchAction(action)
    }

    private suspend fun dispatchAction(action: HomeCheckInEditorAction) {
        when (action) {
            HomeCheckInEditorAction.Initialize, HomeCheckInEditorAction.RetryDetail,
            HomeCheckInEditorAction.RetryMethods -> handleLoadAction(action)
            is HomeCheckInEditorAction.DateChosen, is HomeCheckInEditorAction.MethodChanged,
            is HomeCheckInEditorAction.DoseChanged, is HomeCheckInEditorAction.DurationChanged,
            is HomeCheckInEditorAction.TasteRatingTapped, is HomeCheckInEditorAction.NoteChanged,
            is HomeCheckInEditorAction.DrinkNameChanged, is HomeCheckInEditorAction.VisibilityChanged -> handleFieldAction(action)
            HomeCheckInEditorAction.OpenBeanPicker, HomeCheckInEditorAction.LoadShelfBags,
            HomeCheckInEditorAction.ResolveShelfBagCard, HomeCheckInEditorAction.CloseBeanPicker,
            is HomeCheckInEditorAction.BeanQueryChanged, HomeCheckInEditorAction.SearchBeans -> handleBeanSearchAction(action)
            is HomeCheckInEditorAction.ShelfBagSelected, is HomeCheckInEditorAction.ShowShelfBagChanged,
            is HomeCheckInEditorAction.BeanSelected, is HomeCheckInEditorAction.CustomBeanChosen -> handleBeanSelectionAction(action)
            is HomeCheckInEditorAction.PhotosAdded, is HomeCheckInEditorAction.RemovePhoto,
            is HomeCheckInEditorAction.MovePhoto -> handlePhotoAction(action)
            HomeCheckInEditorAction.Save, HomeCheckInEditorAction.FinishSaved,
            HomeCheckInEditorAction.Close -> handleCompletionAction(action)
            HomeCheckInEditorAction.RevokeAccess -> Unit
        }
    }

    private suspend fun handleLoadAction(action: HomeCheckInEditorAction) {
        when (action) {
            HomeCheckInEditorAction.Initialize -> if (!initialized) {
                initialized = true
                onAction(HomeCheckInEditorAction.RetryMethods)
                if (preparationId == null) {
                    track(HomeCheckInEditorMetric.Opened)
                    updateState { copy(isReady = true) }
                    if (currentState.draft.shelfBagId != null) onAction(HomeCheckInEditorAction.ResolveShelfBagCard)
                } else loadDetail()
            }
            HomeCheckInEditorAction.RetryDetail -> if (initialized) loadDetail()
            HomeCheckInEditorAction.RetryMethods -> loadMethods()
            else -> Unit
        }
    }

    private fun handleFieldAction(action: HomeCheckInEditorAction) {
        when (action) {
            is HomeCheckInEditorAction.DateChosen -> changeDate(action)
            is HomeCheckInEditorAction.MethodChanged -> edit { copy(brewMethodCode = action.code) }
            is HomeCheckInEditorAction.DoseChanged -> edit { copy(doseGrams = action.value.replace(',', '.').ifBlank { null }) }
            is HomeCheckInEditorAction.DurationChanged -> edit { copy(brewDurationSeconds = action.seconds) }
            is HomeCheckInEditorAction.TasteRatingTapped -> if (action.score in 1..5) edit {
                copy(tasteScore = action.score.takeUnless { it == tasteScore })
            }
            is HomeCheckInEditorAction.NoteChanged -> edit { copy(text = action.value.take(1000).ifBlank { null }) }
            is HomeCheckInEditorAction.DrinkNameChanged -> changeDrinkName(action.value)
            is HomeCheckInEditorAction.VisibilityChanged -> edit { copy(visibility = action.value) }
            else -> Unit
        }
    }

    private fun changeDate(action: HomeCheckInEditorAction.DateChosen) = edit {
        // Selecting the same day must keep the original time and offset.
        if (preparedOn == action.civilDate) this else copy(preparedOn = action.civilDate, preparedAt = action.preparedAt)
    }

    private fun changeDrinkName(value: String) = edit {
        val name = value.take(100).ifBlank { null }
        copy(drinkSlug = "other".takeIf { name != null }, customDrinkName = name)
    }

    private suspend fun handleBeanSearchAction(action: HomeCheckInEditorAction) {
        when (action) {
            HomeCheckInEditorAction.OpenBeanPicker -> if (canEdit()) {
                track(HomeCheckInEditorMetric.BeanPickerOpened)
                updateState { copy(isBeanPickerOpen = true) }
                onAction(HomeCheckInEditorAction.LoadShelfBags)
                searchBeans(debounce = false)
            }
            HomeCheckInEditorAction.LoadShelfBags -> if (canEdit() && currentState.isBeanPickerOpen) loadShelfBags()
            HomeCheckInEditorAction.ResolveShelfBagCard -> if (canEdit()) resolveShelfBagCard()
            HomeCheckInEditorAction.CloseBeanPicker -> if (canEdit()) closeBeanPicker()
            is HomeCheckInEditorAction.BeanQueryChanged -> changeBeanQuery(action.value)
            HomeCheckInEditorAction.SearchBeans -> retryBeanSearch()
            else -> Unit
        }
    }

    private suspend fun changeBeanQuery(value: String) {
        if (!canEdit() || !currentState.isBeanPickerOpen) return
        track(HomeCheckInEditorMetric.BeanPickerOpened)
        track(HomeCheckInEditorMetric.FieldChanged(HomePreparationField.Bean))
        updateState { copy(beanQuery = value.take(100)) }
        searchBeans(debounce = true)
    }

    private suspend fun retryBeanSearch() {
        if (!canEdit() || !currentState.isBeanPickerOpen) return
        track(HomeCheckInEditorMetric.Retry(HomeCheckInEditorStage.Bean))
        searchBeans(debounce = false)
    }

    private fun handleBeanSelectionAction(action: HomeCheckInEditorAction) {
        when (action) {
            is HomeCheckInEditorAction.ShelfBagSelected -> selectShelfBag(action.id)
            is HomeCheckInEditorAction.ShowShelfBagChanged -> if (currentState.shelfBagCardUrl != null) edit {
                copy(showShelfBag = action.show)
            }
            is HomeCheckInEditorAction.BeanSelected -> selectBean(action.slug)
            is HomeCheckInEditorAction.CustomBeanChosen -> selectCustomBean(action.name)
            else -> Unit
        }
    }

    private fun selectShelfBag(id: String) {
        if (!canEdit() || !currentState.isBeanPickerOpen) return
        currentState.shelfBags.firstOrNull { it.id == id }?.let { bag ->
            edit { copy(shelfBagId = bag.id, coffeeProductSlug = null, coffeeProductId = null,
                customBeanName = null, showShelfBag = true) }
            updateState { copy(beanSelectionName = bag.name, shelfBagCardUrl = bag.imageUrl) }
            closeBeanPicker()
        }
    }

    private fun selectBean(slug: String) {
        if (!canEdit() || !currentState.isBeanPickerOpen) return
        currentState.beans.firstOrNull { it.slug == slug }?.let { bean ->
            edit { copy(coffeeProductSlug = bean.slug, coffeeProductId = null, customBeanName = null, shelfBagId = null) }
            updateState { copy(beanSelectionName = bean.name) }
            closeBeanPicker()
        }
    }

    private fun selectCustomBean(name: String) {
        if (!canEdit() || !currentState.isBeanPickerOpen || name.isBlank()) return
        edit { copy(coffeeProductSlug = null, coffeeProductId = null, shelfBagId = null,
            customBeanName = name.trim().take(250).ifBlank { null }) }
        closeBeanPicker()
    }

    private fun handlePhotoAction(action: HomeCheckInEditorAction) {
        when (action) {
            is HomeCheckInEditorAction.PhotosAdded -> if (preparationId == null) edit {
                val remaining = (5 - photos.size).coerceAtLeast(0)
                copy(photos = photos + action.photos.take(remaining).map { it.copy(bytes = it.bytes.copyOf()) })
            }
            is HomeCheckInEditorAction.RemovePhoto -> if (preparationId == null && action.index in currentState.draft.photos.indices) edit {
                copy(photos = photos.filterIndexed { index, _ -> index != action.index })
            }
            is HomeCheckInEditorAction.MovePhoto -> movePhoto(action.from, action.to)
            else -> Unit
        }
    }

    private suspend fun handleCompletionAction(action: HomeCheckInEditorAction) {
        when (action) {
            HomeCheckInEditorAction.Save -> save()
            HomeCheckInEditorAction.FinishSaved -> finishSaved()
            HomeCheckInEditorAction.Close -> if (saveJob == null) {
                if (currentState.savedEntry != null) finishSaved()
                else {
                    track(HomeCheckInEditorMetric.Cancelled)
                    invalidateWork()
                    updateState { HomeCheckInEditorState(isClosed = true) }
                    emit(HomeCheckInEditorEvent.Close)
                }
            }
            else -> Unit
        }
    }

    fun canDeliver(envelope: HomeCheckInEditorEventEnvelope): Boolean = canAccess() && !currentState.isAccessRevoked &&
        envelope.sessionGeneration == sessionGeneration && envelope.revision == revision

    private fun canEdit() = currentState.isReady && !currentState.isLoading && !currentState.isSaving &&
        currentState.savedEntry == null && !currentState.isClosed && !currentState.isAccessRevoked

    private suspend fun finishSaved() {
        val snapshot = currentState
        val saved = snapshot.savedEntry ?: return
        revision++
        updateState { HomeCheckInEditorState(isClosed = true) }
        emit(HomeCheckInEditorEvent.Saved(saved.id, snapshot.savedVisibilityUnconfirmed))
    }


    private fun movePhoto(from: Int, to: Int) {
        val photos = currentState.draft.photos
        if (preparationId != null || from !in photos.indices || to !in photos.indices || from == to) return
        edit {
            val reordered = photos.toMutableList()
            reordered.add(to, reordered.removeAt(from))
            copy(photos = reordered)
        }
    }

    private fun edit(transform: HomePreparationInput.() -> HomePreparationInput) {
        if (!canEdit()) return
        val next = currentState.draft.transform()
        currentState.draft.changedFields(next).forEach { track(HomeCheckInEditorMetric.FieldChanged(it)) }
        val instant = now()
        updateState { withDraft(next, instant) }
    }

    private suspend fun loadDetail() {
        if (preparationId == null || currentState.isReady || loadJob != null || saveJob != null) return
        if (preparationId.isBlank()) { updateState { copy(loadIssue = HomePreparationIssue.InvalidInput) }; return }
        val job = currentCoroutineContext().job
        val expectedRevision = ++revision
        loadJob = job
        updateState { copy(isLoading = true, loadIssue = null) }
        try {
            val entry = repository.detail(preparationId).getOrThrow()
            currentCoroutineContext().ensureActive()
            if (!canAccess()) { revokeAccess(); return }
            if (loadJob !== job || expectedRevision != revision) return
            if (entry.id != preparationId) throw HomePreparationFailure(HomePreparationIssue.InvalidResponse)
            updateState { copy(entry = entry, draft = entry.editorInput(), beanSelectionName = entry.beanName, isReady = true,
                shelfBagCardUrl = entry.shelfBagPhoto?.cardUrl()) }
            track(HomeCheckInEditorMetric.Opened)
            // A hidden card is still the bag's to show again, if the bag stands on the shelf with one.
            if (entry.shelfBagId != null && entry.shelfBagPhoto == null) onAction(HomeCheckInEditorAction.ResolveShelfBagCard)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            if (!canAccess()) { revokeAccess(); return }
            if (loadJob === job && expectedRevision == revision && job.isActive) {
                updateState { copy(loadIssue = error.issue()) }
            }
        } finally {
            if (loadJob === job) { loadJob = null; updateState { copy(isLoading = false) } }
        }
    }

    private suspend fun loadMethods() {
        if (methodsJob != null || saveJob != null || currentState.methods.isNotEmpty()) return
        val job = currentCoroutineContext().job
        methodsJob = job
        updateState { copy(methodsLoading = true, methodsIssue = null) }
        try {
            val methods = catalog.brewMethods().getOrThrow()
            currentCoroutineContext().ensureActive()
            if (!canAccess()) { revokeAccess(); return }
            if (methodsJob === job) updateState { copy(methods = methods.distinctBy { it.code }) }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            if (!canAccess()) { revokeAccess(); return }
            if (methodsJob === job && job.isActive) updateState { copy(methodsIssue = error.issue()) }
        } finally {
            if (methodsJob === job) { methodsJob = null; updateState { copy(methodsLoading = false) } }
        }
    }

    private suspend fun searchBeans(debounce: Boolean) {
        cancelSearch()
        val job = currentCoroutineContext().job
        val expectedRevision = searchRevision
        val query = currentState.beanQuery.trim()
        beansJob = job
        updateState { copy(beansLoading = true, beansIssue = null, beans = emptyList()) }
        try {
            if (debounce) delay(300)
            val beans = catalog.searchBeans(query).getOrThrow()
            currentCoroutineContext().ensureActive()
            if (!canAccess()) { revokeAccess(); return }
            if (beansJob === job && searchRevision == expectedRevision) updateState { copy(beans = beans.distinctBy { it.slug }) }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            if (!canAccess()) { revokeAccess(); return }
            if (beansJob === job && searchRevision == expectedRevision && job.isActive) {
                track(HomeCheckInEditorMetric.Failed(HomeCheckInEditorStage.Bean, error.issue(), (error as? HomePreparationFailure)?.statusCode))
                updateState { copy(beansIssue = error.issue()) }
            }
        } finally {
            if (beansJob === job) { beansJob = null; updateState { copy(beansLoading = false) } }
        }
    }

    /** Bags are an optional shortcut: a failure only hides them, the catalogue search still works. */
    private suspend fun loadShelfBags() {
        val bags = catalog.shelfBags().getOrElse { error ->
            if (error is CancellationException) throw error
            emptyList()
        }
        if (!canAccess()) { revokeAccess(); return }
        if (currentState.isBeanPickerOpen) updateState { copy(shelfBags = bags.distinctBy { it.id }) }
    }

    /** The card behind the switch for a bag chosen outside the picker; a bag off the shelf or without a card keeps it hidden. */
    private suspend fun resolveShelfBagCard() {
        val bagId = currentState.draft.shelfBagId ?: return
        val card = catalog.shelfBags().getOrElse { error ->
            if (error is CancellationException) throw error
            emptyList()
        }.firstOrNull { it.id == bagId }?.imageUrl ?: return
        if (!canAccess()) { revokeAccess(); return }
        if (canEdit() && currentState.draft.shelfBagId == bagId && currentState.shelfBagCardUrl == null)
            updateState { copy(shelfBagCardUrl = card) }
    }

    private fun closeBeanPicker() {
        cancelSearch()
        updateState { copy(isBeanPickerOpen = false, beansLoading = false, beansIssue = null, beans = emptyList(), shelfBags = emptyList()) }
    }

    private fun cancelSearch() {
        searchRevision++
        beansJob?.cancel()
        beansJob = null
    }

    private suspend fun save() {
        val snapshot = currentState
        if (!canEdit() || snapshot.isBeanPickerOpen) return
        track(HomeCheckInEditorMetric.Submitted)
        val errors = homePreparationFieldErrors(snapshot.draft, preparationId != null, now())
        recordSaveValidation(errors)
        if (errors.isNotEmpty()) {
            track(HomeCheckInEditorMetric.Validation(errors.keys, server = false))
            return
        }
        val job = currentCoroutineContext().job
        val expectedRevision = ++revision
        saveJob = job
        updateState { copy(isSaving = true) }
        try {
            val entry = (if (preparationId == null) repository.create(snapshot.draft)
                else repository.update(preparationId, snapshot.draft)).getOrThrow()
            currentCoroutineContext().ensureActive()
            if (!canAccess()) { revokeAccess(); return }
            if (saveJob !== job || expectedRevision != revision) return
            if (entry.id.isBlank() || preparationId != null && entry.id != preparationId)
                throw HomePreparationFailure(HomePreparationIssue.InvalidResponse)
            val unconfirmed = snapshot.draft.visibility != null && snapshot.draft.visibility != entry.visibility
            // An accepted write must not become a second create while waiting for navigation.
            revision++
            cancelAuxiliaryWork()
            updateState { copy(savedEntry = entry, savedVisibilityUnconfirmed = unconfirmed, isSaving = false) }
            track(HomeCheckInEditorMetric.Saved(entry.visibility, entry.photos.isNotEmpty()))
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            if (!canAccess()) { revokeAccess(); return }
            if (saveJob === job && expectedRevision == revision && job.isActive) {
                // HTTP auth failure alone is not account replacement: keep this owner's draft.
                val failure = error as? HomePreparationFailure
                if (!failure?.fieldErrors.isNullOrEmpty()) track(HomeCheckInEditorMetric.Validation(failure.fieldErrors.keys, server = true))
                else track(HomeCheckInEditorMetric.Failed(
                    if (failure?.operation == HomePreparationOperation.PhotoUpload) HomeCheckInEditorStage.Photo else HomeCheckInEditorStage.Save,
                    error.issue(), failure?.statusCode))
                updateState { copy(fieldErrors = failure?.fieldErrors.orEmpty(),
                    saveIssue = error.issue().takeIf { failure?.fieldErrors.isNullOrEmpty() },
                    failedOperation = failure?.operation) }
            }
        } finally {
            if (saveJob === job) { saveJob = null; updateState { copy(isSaving = false) } }
        }
    }

    private fun recordSaveValidation(errors: Map<HomePreparationField, HomePreparationIssue>) {
        updateState { copy(fieldErrors = errors, saveIssue = null, failedOperation = null,
            saveAttempt = saveAttempt + if (errors.isNotEmpty()) 1 else 0) }
    }

    private suspend fun emit(event: HomeCheckInEditorEvent) {
        sendEvent(HomeCheckInEditorEventEnvelope(event, sessionGeneration, revision))
    }

    private fun cancelAuxiliaryWork() {
        cancelSearch()
        methodsJob?.cancel()
        methodsJob = null
    }

    private fun invalidateWork() {
        revision++
        loadJob?.cancel()
        saveJob?.cancel()
        loadJob = null
        saveJob = null
        cancelAuxiliaryWork()
    }

    private fun revokeAccess() {
        invalidateWork()
        updateState { HomeCheckInEditorState(isAccessRevoked = true) }
    }

    override fun onCleared() {
        if (!currentState.isClosed && !currentState.isAccessRevoked && currentState.savedEntry == null)
            track(HomeCheckInEditorMetric.Cancelled)
        revokeAccess()
        super.onCleared()
    }

    private fun track(event: HomeCheckInEditorMetric) {
        if (!canAccess() || currentState.isAccessRevoked) return
        try { analytics.record(event) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            // Telemetry is ancillary: a broken SDK must not turn an accepted create into a retry.
        }
    }
}

private fun HomePreparationPhoto.cardUrl() = thumbnailUrl?.takeIf(String::isNotBlank) ?: url

private fun Exception.issue() = (this as? HomePreparationFailure)?.issue ?: HomePreparationIssue.RequestFailed
