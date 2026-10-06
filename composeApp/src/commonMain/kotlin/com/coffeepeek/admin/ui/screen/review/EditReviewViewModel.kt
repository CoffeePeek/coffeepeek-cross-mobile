package com.coffeepeek.admin.ui.screen.review

import com.coffeepeek.domain.repository.ShopRepository

import com.coffeepeek.domain.model.savedDrinkName

import com.coffeepeek.domain.model.validateConsumedDrink

import com.coffeepeek.domain.model.ConsumedDrinkOption

import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.admin.settings.ReviewDraft
import com.coffeepeek.admin.settings.ReviewDraftStore
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.MAX_REVIEW_PHOTOS
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.admin.utils.ReviewSync
import com.coffeepeek.admin.utils.validateReviewComment
import com.coffeepeek.admin.utils.validateReviewHeader
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.coffeepeek.domain.model.UpdateReviewInput
import com.coffeepeek.domain.repository.ReviewRepository
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditReviewUiState(
    val isLoading: Boolean = true,
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
    val drinkName: String? = null,
    val drinkSelectionChanged: Boolean = false,
    val drinks: List<ConsumedDrinkOption> = emptyList(),
    val drinksError: String? = null,
    val header: String = "",
    val comment: String = "",
    val placeRating: Int = 5,
    val serviceRating: Int = 5,
    val coffeeRating: Int = 5,
    val existingPhotoUrls: List<String> = emptyList(),
    val newPhotos: List<PickedImage> = emptyList(),
    val canEdit: Boolean = true,
    /** True while the form shows unsaved edits restored from a draft. */
    val draftRestored: Boolean = false,
    val isSubmitting: Boolean = false,
    val headerError: String? = null,
    val commentError: String? = null,
    val error: String? = null,
)

class EditReviewViewModel(
    private val reviewId: String,
    private val reviewRepository: ReviewRepository,
    private val shopRepository: ShopRepository,
    private val sessionRepository: SessionRepository,
    private val drafts: ReviewDraftStore,
    private val userRepository: UserRepository,
) : BaseViewModel() {
    private val draftKey = ReviewDraftStore.editReviewKey(reviewId)
    // Published values; a draft is stored only while the form differs from them.
    private var baseline: ReviewDraft? = null

    private val _state = MutableStateFlow(EditReviewUiState())
    val state = _state.asStateFlow()
    private var shopIdForSync: String? = null
    // The PUT targets the moderation record, not the published review. Null → nothing to edit.
    private var moderationReviewId: String? = null

    init {
        loadDrinks()
        loadReview()
    }

    fun loadReview() {
        workScope.launch {
            requireAuthSession(sessionRepository) ?: return@launch
            _state.update { it.copy(isLoading = true, error = null) }
            val profile = userRepository.getMe().getOrElse { error ->
                _state.update { it.copy(isLoading = false, error = error.message) }
                return@launch
            }
            val userId = profile.address?.slug ?: run {
                _state.update { it.copy(isLoading = false, error = "Публичный адрес профиля пока недоступен") }
                return@launch
            }
            reviewRepository.getUserReviews(userId, page = 1, pageSize = 100)
                .onSuccess { page ->
                    val review = page.items.find { it.id == reviewId }
                    if (review == null) {
                        _state.update { it.copy(isLoading = false, error = "Отзыв не найден") }
                        return@onSuccess
                    }
                    shopIdForSync = review.shopId
                    moderationReviewId = review.moderationReviewId
                    baseline = ReviewDraft(
                        drinkSlug = review.drinkSlug,
                        customDrinkName = review.customDrinkName,
                        drinkName = savedDrinkName(review.drinkNameRu, review.drinkNameEn, review.customDrinkName),
                        header = review.header,
                        comment = review.comment,
                        placeRating = review.rating.place.coerceIn(1, 5),
                        serviceRating = review.rating.service.coerceIn(1, 5),
                        coffeeRating = review.rating.coffee.coerceIn(1, 5),
                    )
                    val draft = drafts.load(draftKey)
                    val draftPhotos = drafts.photos(draftKey)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            draftRestored = false,
                            drinkSlug = review.drinkSlug,
                            customDrinkName = review.customDrinkName,
                            drinkName = savedDrinkName(review.drinkNameRu, review.drinkNameEn, review.customDrinkName),
                            drinkSelectionChanged = false,
                            header = review.header,
                            comment = review.comment,
                            placeRating = review.rating.place.coerceIn(1, 5),
                            serviceRating = review.rating.service.coerceIn(1, 5),
                            coffeeRating = review.rating.coffee.coerceIn(1, 5),
                            existingPhotoUrls = review.photoUrls,
                            canEdit = review.moderationReviewId != null,
                        ).let { loaded ->
                            if (draft == null && draftPhotos.isEmpty()) return@let loaded
                            loaded.copy(
                                drinkSlug = if (draft?.drinkSelectionChanged == true) draft.drinkSlug else loaded.drinkSlug,
                                customDrinkName = if (draft?.drinkSelectionChanged == true) draft.customDrinkName else loaded.customDrinkName,
                                drinkName = if (draft?.drinkSelectionChanged == true) draft.drinkName else loaded.drinkName,
                                drinkSelectionChanged = draft?.drinkSelectionChanged ?: false,
                                header = draft?.header ?: loaded.header,
                                comment = draft?.comment ?: loaded.comment,
                                placeRating = draft?.placeRating ?: loaded.placeRating,
                                serviceRating = draft?.serviceRating ?: loaded.serviceRating,
                                coffeeRating = draft?.coffeeRating ?: loaded.coffeeRating,
                                newPhotos = draftPhotos,
                                draftRestored = true,
                            )
                        }.copy(
                            error = if (review.moderationReviewId == null) {
                                "Этот отзыв нельзя редактировать."
                            } else null,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    private fun edit(transform: (EditReviewUiState) -> EditReviewUiState) {
        _state.update(transform)
        val s = _state.value
        val current = ReviewDraft(s.header, s.comment, s.placeRating, s.serviceRating, s.coffeeRating, drinkSlug = s.drinkSlug, customDrinkName = s.customDrinkName, drinkName = s.drinkName, drinkSelectionChanged = s.drinkSelectionChanged)
        if (current == baseline && s.newPhotos.isEmpty()) {
            workScope.launch { drafts.clear(draftKey) }
        } else {
            // defaultRating = -1: for edits, "blank" is decided by the baseline comparison above.
            drafts.save(draftKey, current, s.newPhotos, defaultRating = -1)
        }
    }

    fun loadDrinks() {
        workScope.launch {
            shopRepository.getConsumedDrinks().onSuccess { drinks ->
                _state.update { it.copy(drinks = drinks, drinksError = null) }
            }.onFailure { e -> _state.update { it.copy(drinksError = e.message ?: "Не удалось загрузить напитки") } }
        }
    }

    fun onDrinkChange(slug: String?, name: String?) {
        val changed = slug != baseline?.drinkSlug || name?.trim() != baseline?.customDrinkName?.trim()
        edit { it.copy(drinkSlug = slug, customDrinkName = name,
            drinkName = if (changed) null else baseline?.drinkName, drinkSelectionChanged = changed) }
    }

    fun onHeaderChange(v: String) {
        edit { it.copy(header = v.take(120), headerError = null) }
    }

    fun onCommentChange(v: String) {
        edit { it.copy(comment = v.take(2000), commentError = null) }
    }
    fun onPlaceRating(v: Int) { edit { it.copy(placeRating = v.coerceIn(1, 5)) } }
    fun onServiceRating(v: Int) { edit { it.copy(serviceRating = v.coerceIn(1, 5)) } }
    fun onCoffeeRating(v: Int) { edit { it.copy(coffeeRating = v.coerceIn(1, 5)) } }

    /** Explicit «Удалить черновик»: drop unsaved edits and show the published review again. */
    fun discardDraft() {
        workScope.launch {
            drafts.clear(draftKey)
            loadReview()
        }
    }

    fun addPhotos(images: List<PickedImage>) {
        if (images.isEmpty()) return
        edit { state ->
            val remaining = MAX_REVIEW_PHOTOS - state.newPhotos.size
            if (remaining <= 0) return@edit state
            state.copy(newPhotos = state.newPhotos + images.take(remaining))
        }
    }

    fun removeNewPhoto(index: Int) {
        edit { state ->
            state.copy(newPhotos = state.newPhotos.filterIndexed { i, _ -> i != index })
        }
    }

    fun submit(onSuccess: () -> Unit = { Navigator.popBack() }) {
        val s = _state.value
        if (s.isSubmitting || s.isLoading) return
        val moderationId = moderationReviewId
        if (moderationId == null) {
            _state.update { it.copy(error = "Этот отзыв нельзя редактировать.") }
            return
        }
        val drinkError = validateConsumedDrink(s.drinkSlug, s.customDrinkName)
        if (drinkError != null) { _state.update { it.copy(error = drinkError) }; return }
        val headerError = validateReviewHeader(s.header)
        val commentError = validateReviewComment(s.comment)
        if (headerError != null || commentError != null) {
            _state.update {
                it.copy(
                    headerError = headerError,
                    commentError = commentError,
                    error = null,
                )
            }
            return
        }
        // Flag before launching: a double tap must not start a second request.
        _state.update { it.copy(isSubmitting = true, error = null) }
        workScope.launch {
            reviewRepository.updateReview(
                reviewId = moderationId,
                input = UpdateReviewInput(
                    drinkSlug = s.drinkSlug.takeIf { s.drinkSelectionChanged },
                    customDrinkName = s.customDrinkName?.trim().takeIf { s.drinkSelectionChanged },
                    clearDrink = s.drinkSelectionChanged && s.drinkSlug == null && baseline?.drinkSlug != null,
                    header = s.header.trim(),
                    comment = s.comment.trim(),
                    placeRating = s.placeRating,
                    serviceRating = s.serviceRating,
                    coffeeRating = s.coffeeRating,
                    photos = s.newPhotos.map { it.toPendingUpload() },
                )
            ).onSuccess {
                // Saved: drop the draft and the uploaded new photos (re-sending them would duplicate
                // them), then reload the published version so the form reflects the server state.
                drafts.clear(draftKey)
                _state.update { it.copy(isSubmitting = false, newPhotos = emptyList(), draftRestored = false) }
                shopIdForSync?.let { ReviewSync.notifyChanged(it) }
                onSuccess()
                loadReview()
            }.onFailure { e ->
                _state.update { it.copy(isSubmitting = false, error = e.message) }
            }
        }
    }
}

private fun PickedImage.toPendingUpload() = PendingPhotoUpload(
    fileName = fileName,
    contentType = contentType,
    bytes = bytes,
)
