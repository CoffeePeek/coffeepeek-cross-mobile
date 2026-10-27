package com.coffeepeek.feature.shop.impl.ui

import com.coffeepeek.feature.shop.api.ShopCheckInAnalytics
import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility
import com.coffeepeek.feature.shop.impl.ui.data.changed
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.shop.domain.model.MAX_SHOP_CHECK_IN_PHOTOS
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInCreationUnconfirmed
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository
import com.coffeepeek.feature.shop.domain.usecase.validateShopCheckIn
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormEvent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormState
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDraftSnapshot
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDraftStore
import kotlinx.coroutines.CancellationException
import kotlin.time.Clock
import kotlin.time.Instant

internal class ShopCheckInCreateViewModel(
    private val initial: ShopCheckInCreateInput,
    private val repository: ShopCheckInRepository,
    private val drafts: ShopCheckInDraftStore,
    private val now: () -> Instant = { Clock.System.now() },
    private val analytics: ShopCheckInAnalytics = ShopCheckInAnalytics.None,
) : MviViewModel<ShopCheckInFormState, ShopCheckInFormAction, ShopCheckInFormEvent>(
    ShopCheckInFormState(initial),
) {
    init {
        analytics.opened()
        onAction(ShopCheckInFormAction.Load)
    }

    override fun onCleared() {
        analytics.cancelled()
        super.onCleared()
    }

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
            is ShopCheckInFormAction.MovePhoto -> movePhoto(action.from, action.to)
            ShopCheckInFormAction.Submit -> submit()
            ShopCheckInFormAction.Dismiss -> if (!currentState.isSubmitting) {
                analytics.cancelled()
                sendEvent(ShopCheckInFormEvent.Dismiss)
            }
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
            isLoading = false, draftFailed = result.isFailure || snapshot == null) }
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

    private fun movePhoto(from: Int, to: Int) {
        val photos = currentState.input.photos
        if (from !in photos.indices || to !in photos.indices || from == to) return
        edit {
            val reordered = photos.toMutableList()
            reordered.add(to, reordered.removeAt(from))
            copy(photos = reordered)
        }
    }

    private fun edit(transform: ShopCheckInCreateInput.() -> ShopCheckInCreateInput) {
        if (!canEdit()) return
        val before = currentState.input
        val input = before.transform()
        analytics.changed(before, input)
        updateState { copy(input = input, validationError = null, submitFailed = false) }
        saveDraft(input, currentState.deliveryUnconfirmed)
    }

    private fun canEdit(): Boolean = !currentState.isLoading && !currentState.isSubmitting &&
        !currentState.submitted && !currentState.deliveryUnconfirmed

    private fun saveDraft(input: ShopCheckInCreateInput, unconfirmed: Boolean): Boolean {
        val result = draftResult { drafts.save(ShopCheckInDraftSnapshot(input, unconfirmed)) }
        updateState { copy(draftFailed = result.isFailure) }
        return result.isSuccess
    }

    private suspend fun submit() {
        val input = prepareSubmission() ?: return
        updateState { copy(isSubmitting = true) }
        try {
            repository.create(input).getOrThrow()
            completeSubmission(input)
        } catch (cancelled: CancellationException) {
            updateState { copy(deliveryUnconfirmed = true) }
            throw cancelled
        } catch (error: Exception) {
            failSubmission(input, error)
        } finally { updateState { copy(isSubmitting = false) } }
    }

    private fun prepareSubmission(): ShopCheckInCreateInput? {
        val state = currentState
        if (state.isLoading || state.isSubmitting || state.submitted || state.deliveryUnconfirmed) return null
        analytics.submitted()
        val validation = validateShopCheckIn(state.input, now())
        updateState { copy(validationError = validation, submitFailed = false) }
        if (validation != null) {
            analytics.validationFailed()
            return null
        }
        // Persist the delivery guard before any request; never silently repeat an uncertain write.
        return state.input.takeIf { saveDraft(it, unconfirmed = true) }
    }

    private suspend fun completeSubmission(input: ShopCheckInCreateInput) {
        analytics.saved(input.visibility == ShopCheckInVisibility.Public, input.photos.isNotEmpty())
        updateState { copy(submitted = true, deliveryUnconfirmed = false) }
        val cleanup = draftResult { drafts.clear(initial.shopSlug) }
        updateState { copy(draftFailed = cleanup.isFailure) }
        sendEvent(ShopCheckInFormEvent.Submitted(input.visibility))
    }

    private fun failSubmission(input: ShopCheckInCreateInput, error: Exception) {
        analytics.failed(error)
        val uncertain = error is ShopCheckInCreationUnconfirmed
        val saved = if (uncertain) true else saveDraft(input, unconfirmed = false)
        updateState { copy(deliveryUnconfirmed = uncertain || !saved, submitFailed = !uncertain) }
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
