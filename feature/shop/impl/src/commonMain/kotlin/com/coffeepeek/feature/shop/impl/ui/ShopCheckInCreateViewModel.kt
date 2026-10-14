package com.coffeepeek.feature.shop.impl.ui

import com.coffeepeek.core.presentation.MviViewModel
import androidx.lifecycle.viewModelScope
import com.coffeepeek.feature.shop.domain.model.MAX_SHOP_CHECK_IN_PHOTOS
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInCreationUnconfirmed
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository
import com.coffeepeek.feature.shop.domain.usecase.validateShopCheckIn
import com.coffeepeek.feature.shop.domain.usecase.isValidShopDrinkPhoto
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormEvent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormState
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDraftSnapshot
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDraftStore
import com.coffeepeek.feature.shop.impl.ui.data.ShopDrinkPhotoProcessor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant

internal class ShopCheckInCreateViewModel(
    private val initial: ShopCheckInCreateInput,
    private val repository: ShopCheckInRepository,
    private val drafts: ShopCheckInDraftStore,
    private val now: () -> Instant = { Clock.System.now() },
    private val drinkPhotos: ShopDrinkPhotoProcessor? = null,
) : MviViewModel<ShopCheckInFormState, ShopCheckInFormAction, ShopCheckInFormEvent>(
    ShopCheckInFormState(initial),
) {
    private var drinkPhotoJob: Job? = null
    private var drinkPhotoGeneration = 0
    init { onAction(ShopCheckInFormAction.Load) }

    override suspend fun handleActionInternal(action: ShopCheckInFormAction) {
        when (action) {
            ShopCheckInFormAction.Load -> load()
            ShopCheckInFormAction.RetryDrinks -> loadDrinks()
            is ShopCheckInFormAction.TextChanged -> edit { copy(text = action.value.take(1000)) }
            is ShopCheckInFormAction.RatingChanged -> edit { copy(rating = action.rating) }
            is ShopCheckInFormAction.VisitDateChanged -> edit { copy(visitedAtIso = action.isoTimestamp) }
            is ShopCheckInFormAction.VisibilityChanged -> edit { copy(visibility = action.visibility) }
            is ShopCheckInFormAction.DrinkChanged -> edit {
                copy(drinkSlug = action.slug, customDrinkName = if (action.slug == "other") customDrinkName else null)
            }
            is ShopCheckInFormAction.CustomDrinkChanged -> edit {
                if (drinkSlug == "other") copy(customDrinkName = action.value.take(100)) else this
            }
            is ShopCheckInFormAction.PhotosAdded -> edit {
                copy(photos = appendPhotos(photos, action.photos))
            }
            is ShopCheckInFormAction.RemovePhoto -> edit {
                copy(photos = photos.filterIndexed { index, _ -> index != action.index })
            }
            ShopCheckInFormAction.RetryDrinkPhoto -> if (canEdit()) startDrinkPhoto()
            ShopCheckInFormAction.SkipDrinkPhoto -> if (canEdit()) {
                cancelDrinkPhoto()
                updateState { copy(input = input.copy(drinkPhoto = null), drinkPhotoSkipped = true,
                    drinkPhotoCandidates = emptyList(), drinkPhotoProcessing = false, drinkPhotoFailed = false) }
                saveDraft(currentState.input, currentState.deliveryUnconfirmed)
            }
            is ShopCheckInFormAction.ChooseDrinkPhoto -> if (canEdit() && !currentState.drinkPhotoProcessing) {
                currentState.drinkPhotoCandidates.getOrNull(action.index)?.let { photo ->
                    updateState { copy(input = input.copy(drinkPhoto = photo), drinkPhotoSkipped = false) }
                    saveDraft(currentState.input, currentState.deliveryUnconfirmed)
                }
            }
            ShopCheckInFormAction.Submit -> submit()
            ShopCheckInFormAction.Dismiss -> if (!currentState.isSubmitting) sendEvent(ShopCheckInFormEvent.Dismiss)
            ShopCheckInFormAction.OpenHistory -> if (!currentState.isSubmitting) sendEvent(ShopCheckInFormEvent.OpenHistory)
            ShopCheckInFormAction.GoToFeed -> if (currentState.submitted && !currentState.isSubmitting) sendEvent(ShopCheckInFormEvent.GoToFeed)
            ShopCheckInFormAction.HistoryChecked -> {
                if (!currentState.deliveryUnconfirmed || currentState.isSubmitting || currentState.submitted) return
                if (saveDraft(currentState.input, unconfirmed = false)) {
                    updateState { copy(deliveryUnconfirmed = false, submitFailed = false) }
                }
            }
        }
    }

    private suspend fun load() {
        // Reload must not replace edits or reopen a completed submission.
        if (!currentState.isLoading) return
        val result = draftResult { drafts.open(initial) }
        val snapshot = result.getOrNull()?.takeIf { it.input.shopSlug == initial.shopSlug }
        updateState { copy(input = snapshot?.input ?: input,
            deliveryUnconfirmed = snapshot?.deliveryUnconfirmed == true,
            drinkPhotoSkipped = snapshot?.drinkPhotoSkipped == true,
            isLoading = false, draftFailed = result.isFailure || snapshot == null) }
        if (canEdit() && currentState.input.drinkPhoto == null && !currentState.drinkPhotoSkipped) startDrinkPhoto()
        loadDrinks()
    }

    private suspend fun loadDrinks() {
        if (currentState.drinksLoading || currentState.submitted) return
        updateState { copy(drinksLoading = true, drinksFailed = false) }
        try {
            val result = repository.getDrinkOptions()
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            updateState { copy(drinks = result.getOrNull() ?: drinks, drinksFailed = result.isFailure) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            updateState { copy(drinksFailed = true) }
        } finally { updateState { copy(drinksLoading = false) } }
    }

    private fun edit(transform: ShopCheckInCreateInput.() -> ShopCheckInCreateInput) {
        if (!canEdit()) return
        var input = currentState.input.transform()
        val firstPhotoChanged = input.photos.firstOrNull() != currentState.input.photos.firstOrNull()
        if (firstPhotoChanged) {
            cancelDrinkPhoto()
            input = input.copy(drinkPhoto = null)
            updateState { copy(drinkPhotoSkipped = false, drinkPhotoCandidates = emptyList(),
                drinkPhotoFailed = false, drinkPhotoProcessing = false) }
        }
        updateState { copy(input = input, validationError = null, submitFailed = false) }
        saveDraft(input, currentState.deliveryUnconfirmed)
        if (firstPhotoChanged) startDrinkPhoto()
    }

    private fun canEdit(): Boolean = !currentState.isLoading && !currentState.isSubmitting &&
        !currentState.submitted && !currentState.deliveryUnconfirmed

    private fun cancelDrinkPhoto() {
        drinkPhotoGeneration++
        drinkPhotoJob?.cancel()
        drinkPhotoJob = null
    }

    private fun startDrinkPhoto() {
        val processor = drinkPhotos ?: return
        val source = currentState.input.photos.firstOrNull() ?: return
        cancelDrinkPhoto()
        val generation = drinkPhotoGeneration
        updateState { copy(input = input.copy(drinkPhoto = null), drinkPhotoSkipped = false,
            drinkPhotoFailed = false, drinkPhotoProcessing = true, drinkPhotoCandidates = emptyList()) }
        saveDraft(currentState.input, currentState.deliveryUnconfirmed)
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                val candidates = processor.process(source).getOrThrow()
                currentCoroutineContext().ensureActive()
                require(candidates.isNotEmpty() && candidates.all(::isValidShopDrinkPhoto)) { "No usable foreground PNG" }
                if (generation != drinkPhotoGeneration || currentState.input.photos.firstOrNull() != source) return@launch
                updateState { copy(drinkPhotoCandidates = candidates,
                    input = input.copy(drinkPhoto = candidates.singleOrNull()), drinkPhotoFailed = false) }
                saveDraft(currentState.input, currentState.deliveryUnconfirmed)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation == drinkPhotoGeneration) updateState { copy(drinkPhotoFailed = true) }
            } finally {
                if (generation == drinkPhotoGeneration) {
                    drinkPhotoJob = null
                    updateState { copy(drinkPhotoProcessing = false) }
                }
            }
        }
        drinkPhotoJob = job
        job.start()
    }

    private fun saveDraft(input: ShopCheckInCreateInput, unconfirmed: Boolean): Boolean {
        val result = draftResult { drafts.save(ShopCheckInDraftSnapshot(input, unconfirmed, currentState.drinkPhotoSkipped)) }
        updateState { copy(draftFailed = result.isFailure) }
        return result.isSuccess
    }

    private suspend fun submit() {
        val state = currentState
        if (state.isLoading || state.isSubmitting || state.submitted || state.deliveryUnconfirmed ||
            state.drinkPhotoProcessing || state.drinkPhotoSelectionRequired) return
        val validation = validateShopCheckIn(state.input, now())
        updateState { copy(validationError = validation, submitFailed = false) }
        if (validation != null) return
        // Store the delivery guard before the write so recreation/cancellation cannot retry it silently.
        if (!saveDraft(state.input, unconfirmed = true)) return
        updateState { copy(isSubmitting = true) }
        try {
            repository.create(state.input).getOrThrow()
            // Cleanup failure cannot turn an accepted write into another submission.
            updateState { copy(submitted = true, deliveryUnconfirmed = false) }
            val cleanup = draftResult { drafts.clear(initial.shopSlug) }
            updateState { copy(draftFailed = cleanup.isFailure) }
            sendEvent(ShopCheckInFormEvent.Submitted(state.input.visibility))
        } catch (cancelled: CancellationException) {
            updateState { copy(deliveryUnconfirmed = true) }
            throw cancelled
        } catch (error: Exception) {
            val uncertain = error is ShopCheckInCreationUnconfirmed
            val saved = if (uncertain) true else saveDraft(state.input, unconfirmed = false)
            updateState { copy(deliveryUnconfirmed = uncertain || !saved, submitFailed = !uncertain) }
        } finally { updateState { copy(isSubmitting = false) } }
    }
}

private fun <T> draftResult(block: () -> Result<T>): Result<T> {
    val result = try { block() } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { Result.failure(error) }
    result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
    return result
}

private fun appendPhotos(current: List<ShopCheckInPhoto>, added: List<ShopCheckInPhoto>): List<ShopCheckInPhoto> {
    val result = current.toMutableList()
    for (photo in added) {
        if (result.size >= MAX_SHOP_CHECK_IN_PHOTOS) break
        if (photo.bytes.isNotEmpty() && photo.fileName.isNotBlank() && photo.contentType.isNotBlank() && photo !in result) result += photo
    }
    return result
}
