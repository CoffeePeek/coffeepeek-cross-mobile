package com.coffeepeek.feature.shop.impl.ui

import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.model.ShopReviewUpdateInput
import com.coffeepeek.feature.shop.domain.repository.ShopReviewWriteRepository
import com.coffeepeek.feature.shop.domain.repository.ShopUserReviewRepository
import com.coffeepeek.feature.shop.domain.usecase.MAX_SHOP_REVIEW_COMMENT_LENGTH
import com.coffeepeek.feature.shop.domain.usecase.MAX_SHOP_REVIEW_HEADER_LENGTH
import com.coffeepeek.feature.shop.domain.usecase.validateShopReviewText
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopReviewFormAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopReviewFormEvent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopReviewFormMode
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopReviewFormState
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopReviewRatingKind
import com.coffeepeek.feature.shop.impl.ui.data.ShopReviewDraftSnapshot
import com.coffeepeek.feature.shop.impl.ui.data.ShopReviewDraftStore
import kotlinx.coroutines.CancellationException

/** Edits only a published review owned by [userId], using its moderation record for PUT. */
internal class ShopReviewEditViewModel(
    private val reviewId: String,
    private val userId: String,
    private val reviews: ShopUserReviewRepository,
    private val writes: ShopReviewWriteRepository,
    private val drafts: ShopReviewDraftStore,
) : MviViewModel<ShopReviewFormState, ShopReviewFormAction, ShopReviewFormEvent>(
    ShopReviewFormState(mode = ShopReviewFormMode.Edit, isLoading = true),
) {
    private var baseline: ShopReviewDraftSnapshot? = null
    private var moderationId: String? = null
    private var submitted = false

    init { onAction(ShopReviewFormAction.Load) }

    override suspend fun handleActionInternal(action: ShopReviewFormAction) {
        when (action) {
            ShopReviewFormAction.Load -> load()
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
            is ShopReviewFormAction.PhotosAdded, is ShopReviewFormAction.RemovePhoto -> Unit
            ShopReviewFormAction.DiscardDraft -> discardDraft()
            ShopReviewFormAction.Submit -> submit()
        }
    }

    private suspend fun load() {
        if (submitted || currentState.isSubmitting) return
        updateState { copy(isLoading = true, loadError = false) }
        try {
            val review = reviews.findForEdit(userId, reviewId).getOrThrow()
            if (review == null) {
                updateState { copy(isLoading = false, loadError = true, canEdit = false) }
                return
            }
            moderationId = review.moderationReviewId?.takeIf(String::isNotBlank)
            baseline = ShopReviewDraftSnapshot(review.header, review.comment,
                review.rating.clamped(), emptyList())
            val draftResult = drafts.load()
            draftResult.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            val draft = draftResult.getOrNull()
            val source = draft ?: baseline!!
            updateState {
                ShopReviewFormState(
                    mode = ShopReviewFormMode.Edit,
                    header = source.header,
                    comment = source.comment,
                    rating = source.rating.clamped(),
                    existingPhotoUrls = review.photoUrls,
                    canEdit = moderationId != null,
                    draftRestored = draft != null,
                    draftError = draftResult.isFailure,
                    ignoredDraftPhotos = draft?.photos?.isNotEmpty() == true,
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            updateState { copy(isLoading = false, loadError = true, canEdit = false) }
        }
    }

    private suspend fun edit(transform: ShopReviewFormState.() -> ShopReviewFormState) {
        if (submitted || currentState.isLoading || currentState.isSubmitting || !currentState.canEdit) return
        updateState(transform)
        val state = currentState
        val snapshot = ShopReviewDraftSnapshot(state.header, state.comment, state.rating, emptyList())
        val result = try {
            if (snapshot == baseline) drafts.clear() else drafts.save(snapshot)
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
        val original = baseline ?: return
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
        updateState {
            copy(header = original.header, comment = original.comment, rating = original.rating,
                draftRestored = false, draftError = false, ignoredDraftPhotos = false,
                headerError = null, commentError = null, submitError = null)
        }
    }

    private suspend fun submit() {
        val state = currentState
        val target = moderationId
        if (submitted || state.isLoading || state.isSubmitting || !state.canEdit || target == null) return
        val validation = validateShopReviewText(state.header, state.comment)
        if (!validation.isValid) {
            updateState { copy(headerError = validation.headerError,
                commentError = validation.commentError, submitError = null) }
            return
        }
        updateState { copy(isSubmitting = true, submitError = null) }
        try {
            writes.update(ShopReviewUpdateInput(target, state.header.trim(), state.comment.trim(),
                state.rating)).getOrThrow()
            submitted = true
            val clear = try {
                drafts.clear()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Result.failure(error)
            }
            clear.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            updateState { copy(isSubmitting = false, draftRestored = false,
                draftError = clear.isFailure, ignoredDraftPhotos = false) }
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

private fun ShopRating.clamped() = ShopRating(
    place = place.coerceIn(1, 5),
    service = service.coerceIn(1, 5),
    coffee = coffee.coerceIn(1, 5),
)
