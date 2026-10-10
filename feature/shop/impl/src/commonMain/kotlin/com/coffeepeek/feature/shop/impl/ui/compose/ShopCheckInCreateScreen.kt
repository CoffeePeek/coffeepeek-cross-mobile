package com.coffeepeek.feature.shop.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.AppButton
import com.coffeepeek.core.designsystem.component.SwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.shop.domain.model.MAX_SHOP_CHECK_IN_PHOTOS
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.usecase.ShopCheckInValidationError
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_dismiss
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_title
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_photos
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_photos_hint
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_submit
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_history
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_uncertain
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_history_checked
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_draft_error
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_submit_error
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_invalid_shop
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_invalid_date
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_invalid_drink
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_invalid_note
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_invalid_rating
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_invalid_photos
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_invalid_photo
import com.coffeepeek.feature.shop.impl.ui.ShopCheckInCreateViewModel
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopCheckInDateField
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopCheckInDrinkField
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopCheckInNoteField
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopCheckInRatings
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopCheckInSubmissionContent
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopCheckInVisibilityField
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopPhotoAttachments
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormEvent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormState
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDateFormatter
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInPhotoPicker
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Runtime shell. The caller owns its lifecycle VM, platform conversions and navigation. */
@Composable
internal fun ShopCheckInCreateScreen(
    viewModel: ShopCheckInCreateViewModel,
    shopName: String,
    dates: ShopCheckInDateFormatter,
    photos: ShopCheckInPhotoPicker,
    onEvent: (ShopCheckInFormEvent) -> Unit,
) {
    val picker = photos.rememberController()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnEvent by rememberUpdatedState(onEvent)
    LaunchedEffect(viewModel) { viewModel.events.collect { currentOnEvent(it) } }
    SwipeDismissModalBottomSheet(
        onDismissRequest = { viewModel.onAction(ShopCheckInFormAction.Dismiss) },
        dismissDescription = stringResource(Res.string.shop_checkin_form_dismiss),
        dismissEnabled = !state.isSubmitting,
    ) {
        ShopCheckInCreateScreenContent(
            state = state, shopName = shopName,
            visitLabel = dates.label(state.input.visitedAtIso),
            selectedVisitMillis = dates.pickerMillis(state.input.visitedAtIso), nowMillis = dates.nowMillis(),
            onAction = viewModel::onAction,
            onVisitDate = { viewModel.onAction(ShopCheckInFormAction.VisitDateChanged(dates.visitInstant(it))) },
            onGallery = { remaining -> picker.pickFromGallery(remaining) { viewModel.onAction(ShopCheckInFormAction.PhotosAdded(it)) } },
            onCamera = { picker.takePhoto { viewModel.onAction(ShopCheckInFormAction.PhotosAdded(it)) } },
            modifier = Modifier.fillMaxHeight(0.76f),
            photosPreparing = picker.isPreparing,
        )
    }
}

/** Previewable body: no DI, ViewModel, platform picker or network lookup. */
@Composable
internal fun ShopCheckInCreateScreenContent(
    state: ShopCheckInFormState,
    shopName: String,
    visitLabel: String,
    selectedVisitMillis: Long?,
    nowMillis: Long,
    onAction: (ShopCheckInFormAction) -> Unit,
    onVisitDate: (Long) -> Unit,
    onGallery: (Int) -> Unit,
    onCamera: () -> Unit,
    modifier: Modifier = Modifier,
    photosPreparing: Boolean = false,
) {
    val scroll = rememberScrollState()
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(scroll.isScrollInProgress) {
        if (scroll.isScrollInProgress) { keyboard?.hide(); focus.clearFocus() }
    }
    val editable = !state.isLoading && !state.isSubmitting && !state.submitted && !state.deliveryUnconfirmed && !photosPreparing
    Column(
        modifier.fillMaxWidth().testTag("shop-checkin-form").verticalScroll(scroll)
            .imePadding().navigationBarsPadding().padding(CpDimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4),
    ) {
        if (state.isSubmitting || state.submitted) {
            ShopCheckInSubmissionContent(
                submittedVisibility = state.input.visibility.takeIf { state.submitted },
                cleanupFailed = state.submitted && state.draftFailed,
                onFeed = { onAction(ShopCheckInFormAction.GoToFeed) },
                onHistory = { onAction(ShopCheckInFormAction.OpenHistory) },
            )
        } else if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            Text(stringResource(Res.string.shop_checkin_form_title), style = MaterialTheme.typography.headlineSmall)
            Text(shopName, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ShopCheckInRatings(state.input.rating, editable) { onAction(ShopCheckInFormAction.RatingChanged(it)) }
            ShopCheckInDateField(visitLabel, selectedVisitMillis, nowMillis, editable, onVisitDate)
            ShopCheckInNoteField(state.input.text, editable,
                state.validationError == ShopCheckInValidationError.InvalidNote) {
                onAction(ShopCheckInFormAction.TextChanged(it))
            }
            ShopPhotoAttachments(
                photos = state.input.photos.map { it.bytes }, isLoading = photosPreparing, enabled = editable,
                title = stringResource(Res.string.shop_checkin_form_photos),
                hint = stringResource(Res.string.shop_checkin_form_photos_hint, MAX_SHOP_CHECK_IN_PHOTOS),
                maxPhotos = MAX_SHOP_CHECK_IN_PHOTOS,
                onRemove = { onAction(ShopCheckInFormAction.RemovePhoto(it)) },
                onGallery = onGallery, onCamera = onCamera,
            )
            ShopCheckInDrinkField(
                state.drinks, state.input.drinkSlug, state.input.customDrinkName,
                state.drinksLoading, state.drinksFailed, editable,
                state.validationError == ShopCheckInValidationError.InvalidDrink,
                { onAction(ShopCheckInFormAction.RetryDrinks) },
                { onAction(ShopCheckInFormAction.DrinkChanged(it)) },
                { onAction(ShopCheckInFormAction.CustomDrinkChanged(it)) },
            )
            ShopCheckInVisibilityField(state.input.visibility, editable) {
                onAction(ShopCheckInFormAction.VisibilityChanged(it))
            }
            validationMessage(state.validationError)?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { error(message); liveRegion = LiveRegionMode.Polite })
            }
            if (state.draftFailed) Text(stringResource(Res.string.shop_checkin_form_draft_error),
                color = MaterialTheme.colorScheme.error)
            if (state.submitFailed) Text(stringResource(Res.string.shop_checkin_form_submit_error),
                color = MaterialTheme.colorScheme.error)
            if (state.deliveryUnconfirmed) {
                Text(stringResource(Res.string.shop_checkin_form_uncertain),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                TextButton(onClick = { onAction(ShopCheckInFormAction.OpenHistory) }) {
                    Text(stringResource(Res.string.shop_checkin_form_history))
                }
                TextButton(onClick = { onAction(ShopCheckInFormAction.HistoryChecked) }) {
                    Text(stringResource(Res.string.shop_checkin_form_history_checked))
                }
            }
            AppButton(
                text = stringResource(Res.string.shop_checkin_form_submit),
                enabled = editable,
                onClick = {
                    keyboard?.hide()
                    focus.clearFocus(force = true)
                    onAction(ShopCheckInFormAction.Submit)
                },
            )
        }
    }
}

@Composable
private fun validationMessage(error: ShopCheckInValidationError?): String? = error?.let {
    stringResource(when (it) {
        ShopCheckInValidationError.InvalidShop -> Res.string.shop_checkin_form_invalid_shop
        ShopCheckInValidationError.InvalidVisitDate -> Res.string.shop_checkin_form_invalid_date
        ShopCheckInValidationError.InvalidDrink -> Res.string.shop_checkin_form_invalid_drink
        ShopCheckInValidationError.InvalidNote -> Res.string.shop_checkin_form_invalid_note
        ShopCheckInValidationError.InvalidRating -> Res.string.shop_checkin_form_invalid_rating
        ShopCheckInValidationError.TooManyPhotos -> Res.string.shop_checkin_form_invalid_photos
        ShopCheckInValidationError.InvalidPhoto -> Res.string.shop_checkin_form_invalid_photo
    })
}

@Composable
private fun ShopCheckInFormPreview(dark: Boolean, uncertain: Boolean = false, photosPreparing: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    val state = ShopCheckInFormState(
        input = ShopCheckInCreateInput("preview-shop", "Понравился фильтр", ShopRating(4, 4, 4), "2026-10-09T09:00:00Z"),
        isLoading = false, deliveryUnconfirmed = uncertain,
    )
    ShopCheckInCreateScreenContent(state, "Кофейня", "9 октября 2026", 1791504000000, 1791547200000, {}, {}, {}, {}, photosPreparing = photosPreparing)
}

@Preview @Composable private fun ShopCheckInFormLightPreview() = ShopCheckInFormPreview(false)
@Preview @Composable private fun ShopCheckInFormDarkPreview() = ShopCheckInFormPreview(true)
@Preview @Composable private fun ShopCheckInUnconfirmedLightPreview() = ShopCheckInFormPreview(false, true)
@Preview @Composable private fun ShopCheckInUnconfirmedDarkPreview() = ShopCheckInFormPreview(true, true)
@Preview @Composable private fun ShopCheckInPhotosPreparingLightPreview() = ShopCheckInFormPreview(false, photosPreparing = true)
@Preview @Composable private fun ShopCheckInPhotosPreparingDarkPreview() = ShopCheckInFormPreview(true, photosPreparing = true)
