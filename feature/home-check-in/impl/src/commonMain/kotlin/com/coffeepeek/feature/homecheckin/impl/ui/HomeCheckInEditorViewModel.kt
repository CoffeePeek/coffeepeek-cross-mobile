package com.coffeepeek.feature.homecheckin.impl.ui

import androidx.lifecycle.viewModelScope
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
    photoProcessor: HomeCheckInDrinkPhotoProcessor,
    initialPreparedAt: String,
    initialPreparedOn: String,
    private val preparationId: String?,
    private val sessionGeneration: Int,
    private val canAccess: () -> Boolean,
    private val now: () -> Instant = { Clock.System.now() },
) : MviViewModel<HomeCheckInEditorState, HomeCheckInEditorAction, HomeCheckInEditorEventEnvelope>(
    HomeCheckInEditorState(draft = HomePreparationInput(preparedAt = initialPreparedAt,
        preparedOn = initialPreparedOn, tasteScore = 5)),
) {
    private var initialized = false
    private var loadJob: Job? = null
    private var saveJob: Job? = null
    private var methodsJob: Job? = null
    private var beansJob: Job? = null
    private var searchRevision = 0L
    private var revision = 0L
    private val photos = HomeCheckInPhotoPreparation(viewModelScope, photoProcessor, ::onPhotoChanged)

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
                if (preparationId == null) updateState { copy(isReady = true) } else loadDetail()
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
                updateState { copy(isBeanPickerOpen = true) }
                searchBeans(debounce = false)
            }
            HomeCheckInEditorAction.CloseBeanPicker -> if (canEdit()) closeBeanPicker()
            is HomeCheckInEditorAction.BeanQueryChanged -> if (canEdit() && currentState.isBeanPickerOpen) {
                updateState { copy(beanQuery = action.value.take(100)) }
                searchBeans(debounce = true)
            }
            HomeCheckInEditorAction.SearchBeans -> if (canEdit() && currentState.isBeanPickerOpen) searchBeans(debounce = false)
            is HomeCheckInEditorAction.BeanSelected -> if (canEdit() && currentState.isBeanPickerOpen) {
                currentState.beans.firstOrNull { it.slug == action.slug }?.let { bean ->
                    edit { copy(coffeeProductSlug = bean.slug, coffeeProductId = null, customBeanName = null) }
                    updateState { copy(beanSelectionName = bean.name) }
                    closeBeanPicker()
                }
            }
            is HomeCheckInEditorAction.CustomBeanChosen -> if (canEdit() && currentState.isBeanPickerOpen) {
                if (action.name.isBlank()) return
                edit { copy(coffeeProductSlug = null, coffeeProductId = null,
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
            HomeCheckInEditorAction.RetryDrinkPhoto -> if (canPreparePhotos()) photos.retry()
            HomeCheckInEditorAction.SkipDrinkPhoto -> if (canPreparePhotos()) photos.skip()
            is HomeCheckInEditorAction.ChooseDrinkPhoto -> if (canPreparePhotos()) photos.select(action.index)
            HomeCheckInEditorAction.Save -> save()
            HomeCheckInEditorAction.Close -> if (saveJob == null) {
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

    private fun canPreparePhotos() = canEdit() && preparationId == null && currentState.draft.photos.isNotEmpty()

    private fun edit(transform: HomePreparationInput.() -> HomePreparationInput) {
        if (!canEdit()) return
        var next = currentState.draft.transform()
        val sourceChanged = next.photos.firstOrNull() != currentState.draft.photos.firstOrNull()
        if (sourceChanged) next = next.copy(drinkPhoto = null)
        val instant = now()
        updateState {
            val updated = withDraft(next, instant)
            if (sourceChanged) updated.copy(photoPreparation = HomeCheckInPhotoState(
                source = next.photos.firstOrNull(), isProcessing = next.photos.isNotEmpty(),
            )) else updated
        }
        if (sourceChanged) photos.setSource(next.photos.firstOrNull())
    }

    private fun onPhotoChanged(photoState: HomeCheckInPhotoState) {
        if (!canAccess()) { revokeAccess(); return }
        if (!canEdit() || photoState !== photos.state || photoState.source != currentState.draft.photos.firstOrNull()) return
        val instant = now()
        updateState { withDraft(draft.copy(drinkPhoto = photoState.selected), instant).copy(photoPreparation = photoState) }
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
            if (beansJob === job && searchRevision == expectedRevision && job.isActive) updateState { copy(beansIssue = error.issue()) }
        } finally {
            if (beansJob === job) { beansJob = null; updateState { copy(beansLoading = false) } }
        }
    }

    private fun closeBeanPicker() {
        cancelSearch()
        updateState { copy(isBeanPickerOpen = false, beansLoading = false, beansIssue = null, beans = emptyList()) }
    }

    private fun cancelSearch() {
        searchRevision++
        beansJob?.cancel()
        beansJob = null
    }

    private suspend fun save() {
        val snapshot = currentState
        if (!canEdit() || snapshot.isBeanPickerOpen || !snapshot.photoPreparation.canSubmit) return
        val errors = homePreparationFieldErrors(snapshot.draft, preparationId != null, now())
        updateState { copy(fieldErrors = errors, saveIssue = null, failedOperation = null) }
        if (errors.isNotEmpty()) return
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
            emit(HomeCheckInEditorEvent.Saved(entry.id, unconfirmed))
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            if (!canAccess()) { revokeAccess(); return }
            if (saveJob === job && expectedRevision == revision && job.isActive) {
                // HTTP auth failure alone is not account replacement: keep this owner's draft.
                val failure = error as? HomePreparationFailure
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
        photos.clear()
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
        revokeAccess()
        super.onCleared()
    }
}

private fun Exception.issue() = (this as? HomePreparationFailure)?.issue ?: HomePreparationIssue.RequestFailed
