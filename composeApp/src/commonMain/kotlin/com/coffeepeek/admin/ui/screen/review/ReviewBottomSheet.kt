package com.coffeepeek.admin.ui.screen.review

import com.coffeepeek.admin.ui.component.ReviewFormStep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.AppButton
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.PhotoAttachmentsSection
import com.coffeepeek.admin.ui.component.ReviewRatingCards
import com.coffeepeek.admin.ui.component.SwipeDismissModalBottomSheet
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.MAX_REVIEW_PHOTOS
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.admin.di.platformViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CreateReviewBottomSheet(
    shopId: String,
    placeName: String?,
    onDismiss: () -> Unit,
) {
    val vm: CreateReviewViewModel = platformViewModel(
        key = "create-review-$shopId",
        parameters = { parametersOf(shopId) },
    )
    val state by vm.state.collectAsState()

    ReviewEditorBottomSheet(
        title = "Новый отзыв",
        placeName = placeName,
        drinkField = { ConsumedDrinkField(state.drinks, state.drinkSlug, state.customDrinkName, state.drinkName, state.drinksError, vm::loadDrinks, vm::onDrinkChange) },
        header = state.header,
        comment = state.comment,
        coffeeRating = state.coffeeRating,
        serviceRating = state.serviceRating,
        placeRating = state.placeRating,
        photos = state.photos,
        headerError = state.headerError,
        commentError = state.commentError,
        error = state.error,
        isLoading = false,
        isSubmitting = state.isSubmitting,
        submitLabel = "Отправить на модерацию",
        onHeaderChange = vm::onHeaderChange,
        onCommentChange = vm::onCommentChange,
        onCoffeeRatingChange = vm::onCoffeeRating,
        onServiceRatingChange = vm::onServiceRating,
        onPlaceRatingChange = vm::onPlaceRating,
        onPhotosAdded = vm::addPhotos,
        onRemovePhoto = vm::removePhoto,
        onRetry = {},
        onSubmit = { vm.submit(onSuccess = onDismiss) },
        onDismiss = onDismiss,
        draftRestored = state.draftRestored,
        onDiscardDraft = vm::discardDraft,
    )
}

@Composable
fun EditReviewBottomSheet(
    reviewId: String,
    placeName: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit = onDismiss,
) {
    val vm: EditReviewViewModel = platformViewModel(
        key = "edit-review-$reviewId",
        parameters = { parametersOf(reviewId) },
    )
    val state by vm.state.collectAsState()

    ReviewEditorBottomSheet(
        title = "Редактировать отзыв",
        placeName = placeName,
        drinkField = { ConsumedDrinkField(state.drinks, state.drinkSlug, state.customDrinkName, state.drinkName, state.drinksError, vm::loadDrinks, vm::onDrinkChange) },
        header = state.header,
        comment = state.comment,
        coffeeRating = state.coffeeRating,
        serviceRating = state.serviceRating,
        placeRating = state.placeRating,
        existingPhotoUrls = state.existingPhotoUrls,
        photos = state.newPhotos,
        headerError = state.headerError,
        commentError = state.commentError,
        error = state.error,
        isLoading = state.isLoading,
        isSubmitting = state.isSubmitting,
        submitLabel = "Сохранить изменения",
        onHeaderChange = vm::onHeaderChange,
        onCommentChange = vm::onCommentChange,
        onCoffeeRatingChange = vm::onCoffeeRating,
        onServiceRatingChange = vm::onServiceRating,
        onPlaceRatingChange = vm::onPlaceRating,
        onPhotosAdded = vm::addPhotos,
        onRemovePhoto = vm::removeNewPhoto,
        onRetry = vm::loadReview,
        onSubmit = { vm.submit(onSuccess = onSaved) },
        onDismiss = onDismiss,
        draftRestored = state.draftRestored,
        onDiscardDraft = vm::discardDraft,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewEditorBottomSheet(
    title: String,
    placeName: String?,
    drinkField: @Composable () -> Unit,
    header: String,
    comment: String,
    coffeeRating: Int,
    serviceRating: Int,
    placeRating: Int,
    photos: List<PickedImage>,
    existingPhotoUrls: List<String> = emptyList(),
    headerError: String?,
    commentError: String?,
    error: String?,
    isLoading: Boolean,
    isSubmitting: Boolean,
    submitLabel: String,
    onHeaderChange: (String) -> Unit,
    onCommentChange: (String) -> Unit,
    onCoffeeRatingChange: (Int) -> Unit,
    onServiceRatingChange: (Int) -> Unit,
    onPlaceRatingChange: (Int) -> Unit,
    onPhotosAdded: (List<PickedImage>) -> Unit,
    onRemovePhoto: (Int) -> Unit,
    onRetry: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    draftRestored: Boolean = false,
    onDiscardDraft: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    fun dismiss() {
        keyboardController?.hide()
        focusManager.clearFocus()
        onDismiss()
    }

    SwipeDismissModalBottomSheet(
        onDismissRequest = ::dismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Keep the shop hero/title visible behind the modal.
                .fillMaxHeight(0.76f)
                .verticalScroll(scrollState)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = CpDimens.spacing4)
                .padding(bottom = CpDimens.spacing6),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing6),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (!placeName.isNullOrBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = CpIcons.Location,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = placeName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (draftRestored && !isLoading) {
                    DraftNotice(onDiscard = onDiscardDraft)
                }
            }

            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CoffeePeekLoader()
                }
                error != null && header.isBlank() -> Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
                ) {
                    Text(error, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AppButton(text = "Повторить", onClick = onRetry)
                }
                else -> {
                    Column {
                        ReviewFormStep {
                            ReviewRatingCards(
                                coffeeRating = coffeeRating,
                                serviceRating = serviceRating,
                                placeRating = placeRating,
                                onCoffeeRatingChange = onCoffeeRatingChange,
                                onServiceRatingChange = onServiceRatingChange,
                                onPlaceRatingChange = onPlaceRatingChange,
                            )
                        }
                        ReviewFormStep {
                            ReviewHeaderField(header, onHeaderChange, error = headerError)
                        }
                        ReviewFormStep {
                            ReviewCommentField(comment, onCommentChange, error = commentError)
                        }
                        ReviewFormStep(optional = true) {
                            Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                                ExistingReviewPhotos(existingPhotoUrls, onPhotoClick = {})
                                PhotoAttachmentsSection(
                                    photos = photos,
                                    maxPhotos = MAX_REVIEW_PHOTOS,
                                    onPhotosAdded = onPhotosAdded,
                                    onRemovePhoto = onRemovePhoto,
                                    title = if (existingPhotoUrls.isEmpty()) "Фотографии" else "Новые фото",
                                )
                            }
                        }
                        ReviewFormStep(optional = true, last = true) {
                            drinkField()
                        }
                    }

                    error?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.height(CpDimens.spacing2))
                    if (isSubmitting) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CoffeePeekLoader()
                        }
                    } else {
                        AppButton(text = submitLabel, onClick = onSubmit)
                    }
                }
            }
        }
    }
}

/** Shown when the form was pre-filled from a saved draft (see ReviewDraftStore for the policy). */
@Composable
private fun DraftNotice(onDiscard: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = CpIcons.NoteEdit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.size(CpDimens.spacing1))
        Text(
            text = "Черновик восстановлен",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDiscard) {
            Text("Удалить черновик", style = MaterialTheme.typography.labelMedium)
        }
    }
}
