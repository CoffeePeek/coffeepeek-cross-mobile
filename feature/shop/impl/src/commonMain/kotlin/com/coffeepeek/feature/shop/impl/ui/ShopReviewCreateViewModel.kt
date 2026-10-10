package com.coffeepeek.feature.shop.impl.ui

import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.model.ShopReviewCreateInput
import com.coffeepeek.feature.shop.domain.model.appendShopReviewPhotos
import com.coffeepeek.feature.shop.domain.repository.ShopReviewWriteRepository
import com.coffeepeek.feature.shop.domain.usecase.MAX_SHOP_REVIEW_COMMENT_LENGTH
import com.coffeepeek.feature.shop.domain.usecase.MAX_SHOP_REVIEW_HEADER_LENGTH
import com.coffeepeek.feature.shop.domain.usecase.validateShopReviewText
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopReviewFormAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopReviewFormEvent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopReviewFormState
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopReviewRatingKind
import com.coffeepeek.feature.shop.impl.ui.data.ShopReviewDraftSnapshot
import com.coffeepeek.feature.shop.impl.ui.data.ShopReviewDraftStore
import kotlinx.coroutines.CancellationException

/** New-review form only; edit needs its own server-loaded baseline before it can submit. */
internal class ShopReviewCreateViewModel(
    private val shopId: String,
    private val shopName: String?,
    private val writes: ShopReviewWriteRepository,
    private val drafts: ShopReviewDraftStore,
) : MviViewModel<ShopReviewFormState, ShopReviewFormAction, ShopReviewFormEvent>(
    ShopReviewFormState(shopName = shopName, isLoading = true),
) {
    private var submitted = false

    init { onAction(ShopReviewFormAction.Load) }

    override suspend fun handleActionInternal(action: ShopReviewFormAction) {
        when (action) {
            ShopReviewFormAction.Load -> loadDraft()
            is ShopReviewFormAction.HeaderChanged -> edit {
                copy(header = action.value.take(MAX_SHOP_REVIEW_HEADER_LENGTH), headerError = null)
            }
            is ShopReviewFormAction.CommentChanged -> edit {
                copy(comment = action.value.take(MAX_SHOP_REVIEW_COMMENT_LENGTH), commentError = null)
            }
            is ShopReviewFormAction.RatingChanged -> edit {
                copy(rating = when (action.kind) {
                    ShopReviewRatingKind.Place -> rating.copy(place = action.value.coerceIn(1, 5))
                    ShopReviewRatingKind.Service -> rating.copy(service = action.value.coerceIn(1, 5))
                    ShopReviewRatingKind.Coffee -> rating.copy(coffee = action.value.coerceIn(1, 5))
                })
            }
            is ShopReviewFormAction.PhotosAdded -> edit {
                copy(newPhotos = appendShopReviewPhotos(newPhotos, action.photos))
            }
            is ShopReviewFormAction.RemovePhoto -> edit {
                if (action.index !in newPhotos.indices) this
                else copy(newPhotos = newPhotos.filterIndexed { index, _ -> index != action.index })
            }
            ShopReviewFormAction.DiscardDraft -> discardDraft()
            ShopReviewFormAction.Submit -> submit()
        }
    }

    private suspend fun loadDraft() {
        try {
            val result = drafts.load()
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            val draft = result.getOrNull()
            updateState {
                copy(
                    header = draft?.header ?: header,
                    comment = draft?.comment ?: comment,
                    rating = draft?.rating ?: rating,
                    newPhotos = draft?.photos ?: newPhotos,
                    draftRestored = draft != null,
                    draftError = result.isFailure,
                    isLoading = false,
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            updateState { copy(isLoading = false, draftError = true) }
        }
    }

    private fun edit(transform: ShopReviewFormState.() -> ShopReviewFormState) {
        if (submitted || currentState.isLoading || currentState.isSubmitting) return
        updateState(transform)
        val state = currentState
        val result = try {
            drafts.save(ShopReviewDraftSnapshot(state.header, state.comment, state.rating,
                state.newPhotos))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        updateState { copy(draftError = result.isFailure) }
    }

    private suspend fun discardDraft() {
        if (submitted || currentState.isSubmitting) return
        val result = try {
            drafts.clear()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        if (result.isFailure) {
            updateState { copy(draftError = true) }
            return
        }
        updateState { ShopReviewFormState(shopName = shopName) }
    }

    private suspend fun submit() {
        val state = currentState
        if (submitted || state.isLoading || state.isSubmitting) return
        val validation = validateShopReviewText(state.header, state.comment)
        if (!validation.isValid) {
            updateState { copy(headerError = validation.headerError,
                commentError = validation.commentError, submitError = null) }
            return
        }
        updateState { copy(isSubmitting = true, submitError = null) }
        try {
            writes.create(ShopReviewCreateInput(shopId, state.header.trim(), state.comment.trim(),
                ShopRating(state.rating.place, state.rating.service, state.rating.coffee),
                state.newPhotos)).getOrThrow()
            // A failed draft cleanup must never turn a successful server write into a retry.
            submitted = true
            val clear = try {
                drafts.clear()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Result.failure(error)
            }
            clear.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            updateState { ShopReviewFormState(shopName = shopName, draftError = clear.isFailure) }
            if (clear.isFailure) sendEvent(ShopReviewFormEvent.DraftClearFailed)
            sendEvent(ShopReviewFormEvent.Submitted)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            updateState { copy(submitError = error.message.orEmpty(), isSubmitting = false) }
        } finally {
            updateState { if (isSubmitting) copy(isSubmitting = false) else this }
        }
    }
}
