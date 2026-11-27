package com.coffeepeek.feature.shop.impl.ui

import com.coffeepeek.feature.shop.api.ShopCheckInAnalytics
import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPublication
import com.coffeepeek.feature.shop.impl.ui.data.changed
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.shop.domain.model.MAX_SHOP_CHECK_IN_PHOTOS
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInCreationUnconfirmed
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRejection
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInSubmitFailure
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInPublicationReader
import com.coffeepeek.feature.shop.domain.usecase.validateShopCheckIn
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormEvent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormState
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDraftSnapshot
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDraftStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Instant

internal class ShopCheckInCreateViewModel(
    private val initial: ShopCheckInCreateInput,
    private val repository: ShopCheckInRepository,
    private val drafts: ShopCheckInDraftStore,
    private val now: () -> Instant = { Clock.System.now() },
    private val analytics: ShopCheckInAnalytics = ShopCheckInAnalytics.None,
    private val publication: ShopCheckInPublicationReader = ShopCheckInPublicationReader.None,
) : MviViewModel<ShopCheckInFormState, ShopCheckInFormAction, ShopCheckInFormEvent>(
    ShopCheckInFormState(initial),
) {
    private val draftWrites = Mutex()

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
            is ShopCheckInFormAction.TextChanged, is ShopCheckInFormAction.RatingChanged,
            is ShopCheckInFormAction.VisitDateChanged, is ShopCheckInFormAction.VisibilityChanged,
            is ShopCheckInFormAction.DrinkChanged,
            is ShopCheckInFormAction.PhotosAdded, is ShopCheckInFormAction.RemovePhoto -> edit { changedInput(action) }
            is ShopCheckInFormAction.MovePhoto -> movePhoto(action.from, action.to)
            ShopCheckInFormAction.Submit -> submit()
            ShopCheckInFormAction.ChangeShop -> changeShop()
            ShopCheckInFormAction.Discard -> discard()
            ShopCheckInFormAction.HistoryChecked -> historyChecked()
            else -> navigation(action)
        }
    }

    private fun ShopCheckInCreateInput.changedInput(action: ShopCheckInFormAction): ShopCheckInCreateInput = when (action) {
        is ShopCheckInFormAction.TextChanged -> copy(text = action.value.take(1000))
        is ShopCheckInFormAction.RatingChanged -> copy(rating = action.rating)
        is ShopCheckInFormAction.VisitDateChanged -> copy(visitedAtIso = action.isoTimestamp)
        is ShopCheckInFormAction.VisibilityChanged -> copy(visibility = action.visibility)
        is ShopCheckInFormAction.DrinkChanged -> if (action.slug == null || currentState.drinks.any { it.slug == action.slug })
            copy(drinkSlug = action.slug, customDrinkName = null) else this
        is ShopCheckInFormAction.PhotosAdded -> copy(photos = appendPhotos(photos, action.photos))
        is ShopCheckInFormAction.RemovePhoto -> copy(photos = photos.filterIndexed { index, _ -> index != action.index })
        else -> this
    }

    private suspend fun navigation(action: ShopCheckInFormAction) {
        val state = currentState
        if (state.isSubmitting || state.isDiscarding || state.isChangingShop) return
        when (action) {
            ShopCheckInFormAction.Dismiss -> {
                analytics.cancelled()
                sendEvent(ShopCheckInFormEvent.Dismiss)
            }
            ShopCheckInFormAction.AskDiscard -> if (state.hasDraft && !state.submitted) updateState { copy(confirmDiscard = true) }
            ShopCheckInFormAction.CancelDiscard -> updateState { copy(confirmDiscard = false) }
            ShopCheckInFormAction.OpenHistory -> sendEvent(ShopCheckInFormEvent.OpenHistory)
            ShopCheckInFormAction.GoToFeed -> if (state.submitted) sendEvent(ShopCheckInFormEvent.GoToFeed)
            else -> Unit
        }
    }

    private suspend fun historyChecked() {
        val state = currentState
        if (!state.deliveryUnconfirmed || state.isSubmitting || state.submitted || state.isDiscarding ||
            state.confirmDiscard || state.isChangingShop) return
        val saved = saveDraft(state.input, unconfirmed = false)
        updateState { copy(deliveryUnconfirmed = false, submitFailure = null, draftFailed = !saved) }
    }

    private suspend fun changeShop() {
        val state = currentState
        if (state.isLoading || state.isSubmitting || state.isDiscarding || state.isChangingShop || state.submitted || state.confirmDiscard) return
        updateState { copy(isChangingShop = true) }
        var saved = false
        try {
            val needsSave = state.input != initial || state.hasDraft || state.deliveryUnconfirmed || draftWrites.isLocked
            saved = !needsSave || saveDraft(currentState.input, currentState.deliveryUnconfirmed)
            if (saved) sendEvent(ShopCheckInFormEvent.ChangeShop)
        } finally {
            if (!saved) updateState { copy(isChangingShop = false) }
        }
    }

    private suspend fun load() {
        // Reload must not replace edits or reopen a completed submission.
        if (!currentState.isLoading) return
        val result = draftResult { drafts.open(initial) }
        val snapshot = result.getOrNull()?.takeIf { it.input.shopSlug == initial.shopSlug }
        updateState { copy(input = snapshot?.input ?: input,
            deliveryUnconfirmed = snapshot?.deliveryUnconfirmed == true,
            hasDraft = snapshot?.exists == true,
            isLoading = false, draftFailed = result.isFailure || snapshot == null) }
        loadDrinks()
    }

    private suspend fun loadDrinks() {
        if (currentState.drinksLoading || currentState.submitted) return
        updateState { copy(drinksLoading = true, drinksFailed = false) }
        try {
            val result = repository.getDrinkOptions()
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            val options = result.getOrNull()?.filter { it.slug.isNotBlank() && it.slug != "other" }?.distinctBy { it.slug }
            updateState { copy(drinks = options ?: drinks, drinksFailed = result.isFailure) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            updateState { copy(drinksFailed = true) }
        } finally { updateState { copy(drinksLoading = false) } }
    }

    private suspend fun movePhoto(from: Int, to: Int) {
        val photos = currentState.input.photos
        if (from !in photos.indices || to !in photos.indices || from == to) return
        edit {
            val reordered = photos.toMutableList()
            reordered.add(to, reordered.removeAt(from))
            copy(photos = reordered)
        }
    }

    private suspend fun edit(transform: ShopCheckInCreateInput.() -> ShopCheckInCreateInput) {
        if (!canEdit()) return
        val before = currentState.input
        val input = before.transform()
        if (before == input) return
        analytics.changed(before, input)
        updateState { copy(input = input, validationError = null, submitFailure = null) }
        saveDraft(input, currentState.deliveryUnconfirmed)
    }

    private fun canEdit(): Boolean = !currentState.isLoading && !currentState.isSubmitting && !currentState.isDiscarding &&
        !currentState.submitted && !currentState.deliveryUnconfirmed && !currentState.confirmDiscard && !currentState.isChangingShop

    private suspend fun saveDraft(input: ShopCheckInCreateInput, unconfirmed: Boolean): Boolean {
        val result = draftWrites.withLock { draftResult { drafts.save(ShopCheckInDraftSnapshot(input, unconfirmed)) } }
        updateState { copy(hasDraft = hasDraft || result.isSuccess,
            draftFailed = if (this.input == input) result.isFailure else draftFailed) }
        return result.isSuccess
    }

    private suspend fun discard() {
        if (!currentState.confirmDiscard || currentState.isSubmitting || currentState.isDiscarding || currentState.isChangingShop) return
        updateState { copy(confirmDiscard = false, isDiscarding = true) }
        val result = draftWrites.withLock { draftResult { drafts.clear(initial.shopSlug) } }
        updateState { copy(isDiscarding = false, hasDraft = result.isFailure, draftFailed = result.isFailure) }
        if (result.isSuccess) {
            analytics.cancelled()
            sendEvent(ShopCheckInFormEvent.Dismiss)
        }
    }

    private suspend fun submit() {
        val input = prepareSubmission() ?: return
        updateState { copy(isSubmitting = true) }
        var createdId: String? = null
        try {
            // Persist the delivery guard before any request. A broken storage must not block
            // the submission itself; the uncertain outcome stays visible only in this session.
            saveDraft(input, unconfirmed = true)
            createdId = repository.create(input).getOrThrow()
            completeSubmission(input)
        } catch (cancelled: CancellationException) {
            updateState { copy(deliveryUnconfirmed = true) }
            throw cancelled
        } catch (error: Exception) {
            failSubmission(input, error)
        } finally { updateState { copy(isSubmitting = false) } }
        if (input.visibility == ShopCheckInVisibility.Public && currentState.submitted) {
            createdId?.let { refreshPublication(it) }
        }
    }

    private suspend fun refreshPublication(checkInId: String) {
        val status = try { publication.status(checkInId) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { Result.failure(error) }
        status.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        updateState { copy(publication = status.getOrNull() ?: ShopCheckInPublication.Unknown) }
    }

    private fun prepareSubmission(): ShopCheckInCreateInput? {
        val state = currentState
        if (!canEdit()) return null
        analytics.submitted()
        val validation = validateShopCheckIn(state.input, now())
        updateState { copy(validationError = validation, submitFailure = null) }
        if (validation != null) {
            analytics.validationFailed()
            return null
        }
        return state.input
    }

    private suspend fun completeSubmission(input: ShopCheckInCreateInput) {
        analytics.saved(input.visibility == ShopCheckInVisibility.Public, input.photos.isNotEmpty())
        updateState { copy(submitted = true, deliveryUnconfirmed = false) }
        val cleanup = draftWrites.withLock { draftResult { drafts.clear(initial.shopSlug) } }
        updateState { copy(draftFailed = cleanup.isFailure) }
        sendEvent(ShopCheckInFormEvent.Submitted(input.visibility))
    }

    private suspend fun failSubmission(input: ShopCheckInCreateInput, error: Exception) {
        analytics.failed(error)
        val uncertain = error is ShopCheckInCreationUnconfirmed
        val saved = if (uncertain) true else saveDraft(input, unconfirmed = false)
        val rejection = error as? ShopCheckInRejection
        updateState { copy(deliveryUnconfirmed = uncertain, draftFailed = !saved,
            submitFailure = if (uncertain) null else ShopCheckInSubmitFailure(rejection?.status, rejection?.serverMessage)) }
    }
}

private suspend fun <T> draftResult(block: suspend () -> Result<T>): Result<T> {
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
