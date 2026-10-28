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
        when (action) {
            HomeCheckInEditorAction.Initialize -> if (!initialized) {
                initialized = true
                onAction(HomeCheckInEditorAction.RetryMethods)
                if (preparationId == null) {
                    track(HomeCheckInEditorMetric.Opened)
                    updateState { copy(isReady = true) }
                } else loadDetail()
            }
            HomeCheckInEditorAction.RetryDetail -> if (initialized) loadDetail()
            HomeCheckInEditorAction.RetryMethods -> loadMethods()
            is HomeCheckInEditorAction.DateChosen -> edit {
                // A picker selecting the same civil day must not replace the recorded time with midnight.
                if (preparedOn == action.civilDate) this else copy(preparedOn = action.civilDate, preparedAt = action.preparedAt)
            }
            is HomeCheckInEditorAction.MethodChanged -> edit { copy(brewMethodCode = action.code) }
            is HomeCheckInEditorAction.DoseChanged -> edit { copy(doseGrams = action.value.replace(',', '.').ifBlank { null }) }
            is HomeCheckInEditorAction.DurationChanged -> edit { copy(brewDurationSeconds = action.seconds) }
            is HomeCheckInEditorAction.TasteRatingTapped -> if (action.score in 1..5) edit {
                copy(tasteScore = action.score.takeUnless { it == tasteScore })
            }
            is HomeCheckInEditorAction.NoteChanged -> edit { copy(text = action.value.take(1000).ifBlank { null }) }
            is HomeCheckInEditorAction.DrinkNameChanged -> edit {
                val name = action.value.take(100).ifBlank { null }
                copy(drinkSlug = "other".takeIf { name != null }, customDrinkName = name)
            }
            is HomeCheckInEditorAction.VisibilityChanged -> edit { copy(visibility = action.value) }
            HomeCheckInEditorAction.OpenBeanPicker -> if (canEdit()) {
                track(HomeCheckInEditorMetric.BeanPickerOpened)
                updateState { copy(isBeanPickerOpen = true) }
                onAction(HomeCheckInEditorAction.LoadShelfBags)
                searchBeans(debounce = false)
            }
            HomeCheckInEditorAction.LoadShelfBags -> if (canEdit() && currentState.isBeanPickerOpen) loadShelfBags()
            is HomeCheckInEditorAction.ShelfBagSelected -> if (canEdit() && currentState.isBeanPickerOpen) {
                currentState.shelfBags.firstOrNull { it.id == action.id }?.let { bag ->
                    edit { copy(shelfBagId = bag.id, coffeeProductSlug = null, coffeeProductId = null, customBeanName = null) }
                    updateState { copy(beanSelectionName = bag.name) }
                    closeBeanPicker()
                }
            }
            HomeCheckInEditorAction.CloseBeanPicker -> if (canEdit()) closeBeanPicker()
            is HomeCheckInEditorAction.BeanQueryChanged -> if (canEdit() && currentState.isBeanPickerOpen) {
                track(HomeCheckInEditorMetric.BeanPickerOpened)
                track(HomeCheckInEditorMetric.FieldChanged(HomePreparationField.Bean))
                updateState { copy(beanQuery = action.value.take(100)) }
                searchBeans(debounce = true)
            }
            HomeCheckInEditorAction.SearchBeans -> if (canEdit() && currentState.isBeanPickerOpen) {
                track(HomeCheckInEditorMetric.Retry(HomeCheckInEditorStage.Bean))
                searchBeans(debounce = false)
            }
            is HomeCheckInEditorAction.BeanSelected -> if (canEdit() && currentState.isBeanPickerOpen) {
                currentState.beans.firstOrNull { it.slug == action.slug }?.let { bean ->
                    edit { copy(coffeeProductSlug = bean.slug, coffeeProductId = null, customBeanName = null, shelfBagId = null) }
                    updateState { copy(beanSelectionName = bean.name) }
                    closeBeanPicker()
                }
            }
            is HomeCheckInEditorAction.CustomBeanChosen -> if (canEdit() && currentState.isBeanPickerOpen) {
                if (action.name.isBlank()) return
                edit { copy(coffeeProductSlug = null, coffeeProductId = null, shelfBagId = null,
                    customBeanName = action.name.trim().take(250).ifBlank { null }) }
                closeBeanPicker()
            }
            is HomeCheckInEditorAction.PhotosAdded -> if (preparationId == null) edit {
                val remaining = (5 - photos.size).coerceAtLeast(0)
                copy(photos = photos + action.photos.take(remaining).map { it.copy(bytes = it.bytes.copyOf()) })
            }
            is HomeCheckInEditorAction.RemovePhoto -> if (preparationId == null && action.index in currentState.draft.photos.indices) edit {
                copy(photos = photos.filterIndexed { index, _ -> index != action.index })
            }
            is HomeCheckInEditorAction.MovePhoto -> movePhoto(action.from, action.to)
            HomeCheckInEditorAction.Save -> save()
            HomeCheckInEditorAction.Close -> if (saveJob == null) {
                track(HomeCheckInEditorMetric.Cancelled)
                invalidateWork()
                updateState { HomeCheckInEditorState(isClosed = true) }
                emit(HomeCheckInEditorEvent.Close)
            }
            HomeCheckInEditorAction.RevokeAccess -> Unit
        }
    }

    fun canDeliver(envelope: HomeCheckInEditorEventEnvelope): Boolean = canAccess() && !currentState.isAccessRevoked &&
        envelope.sessionGeneration == sessionGeneration && envelope.revision == revision

    private fun canEdit() = currentState.isReady && !currentState.isLoading && !currentState.isSaving &&
        !currentState.isClosed && !currentState.isAccessRevoked


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
            updateState { copy(entry = entry, draft = entry.editorInput(), beanSelectionName = entry.beanName, isReady = true) }
            track(HomeCheckInEditorMetric.Opened)
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
        updateState { copy(fieldErrors = errors, saveIssue = null, failedOperation = null) }
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
            updateState { HomeCheckInEditorState(isClosed = true) }
            track(HomeCheckInEditorMetric.Saved(entry.visibility, entry.photos.isNotEmpty()))
            emit(HomeCheckInEditorEvent.Saved(entry.id, unconfirmed))
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
        if (!currentState.isClosed && !currentState.isAccessRevoked) track(HomeCheckInEditorMetric.Cancelled)
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

private fun Exception.issue() = (this as? HomePreparationFailure)?.issue ?: HomePreparationIssue.RequestFailed
