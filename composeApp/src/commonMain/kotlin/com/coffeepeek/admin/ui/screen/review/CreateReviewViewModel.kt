package com.coffeepeek.admin.ui.screen.review

import com.coffeepeek.domain.repository.ShopRepository

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
import com.coffeepeek.domain.model.CreateReviewInput
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.coffeepeek.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val DEFAULT_RATING = 4

data class CreateReviewUiState(
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
    val drinkName: String? = null,
    val drinks: List<ConsumedDrinkOption> = emptyList(),
    val drinksError: String? = null,
    val header: String = "",
    val comment: String = "",
    val placeRating: Int = DEFAULT_RATING,
    val serviceRating: Int = DEFAULT_RATING,
    val coffeeRating: Int = DEFAULT_RATING,
    /** True while the form shows a restored draft; drives the «Черновик» notice. */
    val draftRestored: Boolean = false,
    val photos: List<PickedImage> = emptyList(),
    val isSubmitting: Boolean = false,
    val headerError: String? = null,
    val commentError: String? = null,
    val error: String? = null,
)

class CreateReviewViewModel(
    private val shopId: String,
    private val reviewRepository: ReviewRepository,
    private val shopRepository: ShopRepository,
    private val drafts: ReviewDraftStore,
) : BaseViewModel() {

    private val _state = MutableStateFlow(CreateReviewUiState())
    val state = _state.asStateFlow()
    private val draftKey = ReviewDraftStore.newReviewKey(shopId)

    init {
        loadDrinks()
        workScope.launch {
            val draft = drafts.load(draftKey)
            val photos = drafts.photos(draftKey)
            if (draft == null && photos.isEmpty()) return@launch
            _state.update {
                it.copy(
                    drinkSlug = draft?.drinkSlug,
                    customDrinkName = draft?.customDrinkName,
                    drinkName = draft?.drinkName,
                    header = draft?.header ?: it.header,
                    comment = draft?.comment ?: it.comment,
                    placeRating = draft?.placeRating ?: it.placeRating,
                    serviceRating = draft?.serviceRating ?: it.serviceRating,
                    coffeeRating = draft?.coffeeRating ?: it.coffeeRating,
                    photos = photos,
                    draftRestored = true,
                )
            }
        }
    }

    private fun edit(transform: (CreateReviewUiState) -> CreateReviewUiState) {
        _state.update(transform)
        val s = _state.value
        drafts.save(
            key = draftKey,
            draft = ReviewDraft(s.header, s.comment, s.placeRating, s.serviceRating, s.coffeeRating, drinkSlug = s.drinkSlug, customDrinkName = s.customDrinkName, drinkName = s.drinkName),
            draftPhotos = s.photos,
            defaultRating = DEFAULT_RATING,
        )
    }

    fun loadDrinks() {
        workScope.launch {
            shopRepository.getConsumedDrinks().onSuccess { drinks ->
                _state.update { it.copy(drinks = drinks, drinksError = null) }
            }.onFailure { e -> _state.update { it.copy(drinksError = e.message ?: "Не удалось загрузить напитки") } }
        }
    }

    fun onDrinkChange(slug: String?, name: String?) {
        edit { it.copy(drinkSlug = slug, customDrinkName = name, drinkName = null) }
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

    /** Explicit «Удалить черновик»: wipe the stored draft and reset the form. */
    fun discardDraft() {
        _state.value = CreateReviewUiState(drinks = _state.value.drinks, drinksError = _state.value.drinksError)
        workScope.launch { drafts.clear(draftKey) }
    }

    fun addPhotos(images: List<PickedImage>) {
        if (images.isEmpty()) return
        edit { state ->
            val remaining = MAX_REVIEW_PHOTOS - state.photos.size
            if (remaining <= 0) return@edit state
            state.copy(photos = state.photos + images.take(remaining))
        }
    }

    fun removePhoto(index: Int) {
        edit { state ->
            state.copy(photos = state.photos.filterIndexed { i, _ -> i != index })
        }
    }

    fun submit(onSuccess: () -> Unit = { Navigator.popBack() }) {
        val s = _state.value
        if (s.isSubmitting) return
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
            reviewRepository.createReview(
                CreateReviewInput(
                    shopId = shopId,
                    drinkSlug = s.drinkSlug,
                    customDrinkName = s.customDrinkName?.trim(),
                    header = s.header.trim(),
                    comment = s.comment.trim(),
                    placeRating = s.placeRating,
                    serviceRating = s.serviceRating,
                    coffeeRating = s.coffeeRating,
                    photos = s.photos.map { it.toPendingUpload() },
                )
            ).onSuccess {
                // Sent: wipe everything (form, photos, stored draft) so the same review can't be sent twice.
                // The ViewModel outlives the sheet (keyed per shop), so reopening must show an empty form.
                drafts.clear(draftKey)
                _state.value = CreateReviewUiState(drinks = _state.value.drinks, drinksError = _state.value.drinksError)
                ReviewSync.notifyChanged(shopId)
                onSuccess()
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
