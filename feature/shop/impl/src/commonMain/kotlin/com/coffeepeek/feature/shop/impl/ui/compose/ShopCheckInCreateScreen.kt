package com.coffeepeek.feature.shop.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekDestructiveConfirmationSheet
import com.coffeepeek.core.designsystem.component.CoffeePeekBackHandler
import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.component.CoffeePeekIcon
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.usecase.ShopCheckInValidationError
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_dismiss
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_title
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_today
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_history
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_uncertain
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_history_checked
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_draft_error
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_discard
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_discard_title
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_discard_message
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_discard_cancel
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_error_forbidden
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_error_photos_too_large
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_error_shop_gone
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_error_too_many_requests
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_error_unauthorized
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_submit_error
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_invalid_shop
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_invalid_date
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_invalid_drink
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_invalid_note
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_invalid_rating
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_invalid_photos
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_invalid_photo
import com.coffeepeek.feature.shop.impl.ui.ShopCheckInCreateViewModel
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopCheckInFormFields
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopCheckInSaveBar
import com.coffeepeek.feature.shop.impl.ui.compose.component.isEditable
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopCheckInSubmissionContent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInSubmitFailure
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormEvent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormState
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDateFormatter
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInPhotoPicker
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Runtime shell. The caller owns its lifecycle VM, platform conversions and navigation. */
@Composable
internal fun ShopCheckInCreateScreen(
    viewModel: ShopCheckInCreateViewModel,
    shopName: String,
    shopAddress: String?,
    dates: ShopCheckInDateFormatter,
    photos: ShopCheckInPhotoPicker,
    onChangeShop: () -> Unit,
    onEvent: (ShopCheckInFormEvent) -> Unit,
) {
    val picker = photos.rememberController()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val visitMillis = dates.pickerMillis(state.input.visitedAtIso)
    val todayMillis = dates.nowMillis()
    val visitLabel = if (visitMillis == todayMillis) stringResource(Res.string.shop_checkin_today)
        else dates.label(state.input.visitedAtIso)
    val currentOnEvent by rememberUpdatedState(onEvent)
    LaunchedEffect(viewModel) { viewModel.events.collect { currentOnEvent(it) } }
    CoffeePeekBackHandler(enabled = !state.isSubmitting && !state.isDiscarding) {
        viewModel.onAction(ShopCheckInFormAction.Dismiss)
    }
    CoffeePeekScaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = { ShopCheckInEditorTopBar(state, viewModel::onAction) },
        bottomBar = { if (!state.submitted && !state.isSubmitting) ShopCheckInSaveBar(
            state, picker.isPreparing, viewModel::onAction) },
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets)) {
            ShopCheckInCreateScreenContent(
                state = state, shopName = shopName, shopAddress = shopAddress,
                visitLabel = visitLabel,
                selectedVisitMillis = visitMillis, nowMillis = todayMillis,
                onAction = viewModel::onAction, onChangeShop = onChangeShop,
                onVisitDate = { viewModel.onAction(ShopCheckInFormAction.VisitDateChanged(dates.visitInstant(it, state.input.visitedAtIso))) },
                onGallery = { remaining -> picker.pickFromGallery(remaining) { viewModel.onAction(ShopCheckInFormAction.PhotosAdded(it)) } },
                onCamera = { picker.takePhoto { viewModel.onAction(ShopCheckInFormAction.PhotosAdded(it)) } },
                modifier = Modifier.fillMaxSize(), photosPreparing = picker.isPreparing)
        }
    }
    if (state.confirmDiscard) ShopCheckInDiscardConfirmation(viewModel::onAction)
}

/** Previewable body: no DI, ViewModel, platform picker or network lookup. */
@Composable
internal fun ShopCheckInCreateScreenContent(
    state: ShopCheckInFormState,
    shopName: String,
    shopAddress: String?,
    visitLabel: String,
    selectedVisitMillis: Long?,
    nowMillis: Long,
    onAction: (ShopCheckInFormAction) -> Unit,
    onChangeShop: () -> Unit,
    onVisitDate: (Long) -> Unit,
    onGallery: (Int) -> Unit,
    onCamera: () -> Unit,
    modifier: Modifier = Modifier,
    photosPreparing: Boolean = false,
) {
    if (state.isSubmitting) {
        ShopCheckInSendingPane(modifier)
        return
    }
    val scroll = rememberScrollState()
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(scroll.isScrollInProgress) {
        if (scroll.isScrollInProgress) { keyboard?.hide(); focus.clearFocus() }
    }
    val editable = state.isEditable(photosPreparing)
    Column(
        modifier.fillMaxWidth().testTag("shop-checkin-form").verticalScroll(scroll)
            .padding(horizontal = CoffeePeekDimens.spacing4)
            .padding(top = CoffeePeekDimens.spacing3, bottom = CoffeePeekDimens.spacing6),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing6),
    ) {
        if (state.submitted) {
            ShopCheckInSubmissionContent(
                submittedVisibility = state.input.visibility.takeIf { state.submitted },
                cleanupFailed = state.submitted && state.draftFailed,
                onFeed = { onAction(ShopCheckInFormAction.GoToFeed) },
            )
        } else if (state.isLoading) {
            CoffeePeekLoader(size = 40.dp, color = MaterialTheme.colorScheme.primary, strokeWidth = 4.dp)
        } else {
            ShopCheckInFormFields(state, shopName, shopAddress, visitLabel,
                selectedVisitMillis, nowMillis, editable, photosPreparing,
                onAction, onChangeShop, onVisitDate, onGallery, onCamera)
            ShopCheckInFeedback(state, onAction)
        }
    }
}

@Composable
private fun ShopCheckInFeedback(state: ShopCheckInFormState, onAction: (ShopCheckInFormAction) -> Unit) {
    validationMessage(state.validationError)?.let { message ->
        Text(message, color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { error(message); liveRegion = LiveRegionMode.Polite })
    }
    if (state.draftFailed) Text(stringResource(Res.string.shop_checkin_form_draft_error),
        color = MaterialTheme.colorScheme.error)
    state.submitFailure?.let { failure ->
        Text(submitFailureText(failure), color = MaterialTheme.colorScheme.error)
    }
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
}

@Composable
private fun ShopCheckInEditorTopBar(state: ShopCheckInFormState, onAction: (ShopCheckInFormAction) -> Unit) {
    CoffeePeekTopBar(
        title = stringResource(Res.string.shop_checkin_form_title),
        backDescription = stringResource(Res.string.shop_checkin_form_dismiss),
        onBack = { onAction(ShopCheckInFormAction.Dismiss) },
        actions = {
            if (state.hasDraft && !state.isLoading && !state.isSubmitting && !state.submitted) {
                CoffeePeekGlassIconButton(
                    onClick = { onAction(ShopCheckInFormAction.AskDiscard) },
                    contentDescription = stringResource(Res.string.shop_checkin_discard),
                    enabled = !state.isDiscarding,
                ) {
                    CoffeePeekIcon(CoffeePeekIcons.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
        },
    )
}

@Composable
private fun ShopCheckInDiscardConfirmation(onAction: (ShopCheckInFormAction) -> Unit) {
    CoffeePeekDestructiveConfirmationSheet(
        title = stringResource(Res.string.shop_checkin_discard_title),
        message = stringResource(Res.string.shop_checkin_discard_message),
        confirmLabel = stringResource(Res.string.shop_checkin_discard),
        cancelLabel = stringResource(Res.string.shop_checkin_discard_cancel),
        onConfirm = { onAction(ShopCheckInFormAction.Discard) },
        onDismiss = { onAction(ShopCheckInFormAction.CancelDiscard) },
    )
}

@Composable
private fun ShopCheckInSendingPane(modifier: Modifier) {
    Box(
        modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = CoffeePeekDimens.spacing4),
        contentAlignment = Alignment.Center,
    ) {
        ShopCheckInSubmissionContent(null, false, {})
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
private fun ShopCheckInFormPreview(dark: Boolean, uncertain: Boolean = false,
    photosPreparing: Boolean = false, submitted: Boolean = false, loading: Boolean = false,
    submitting: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    val state = ShopCheckInFormState(
        input = ShopCheckInCreateInput("preview-shop", "Понравился фильтр", ShopRating(4, 4, 4), "2026-10-09T09:00:00Z"),
        isLoading = loading, isSubmitting = submitting, deliveryUnconfirmed = uncertain,
        submitted = submitted, hasDraft = !loading && !submitted,
    )
    CoffeePeekScaffold(
        topBar = { ShopCheckInEditorTopBar(state, {}) },
        bottomBar = { if (!state.submitted && !state.isSubmitting) ShopCheckInSaveBar(state, photosPreparing, {}) },
    ) { insets ->
        ShopCheckInCreateScreenContent(state, "The Coffee Room", "ул. Петра Мстиславца, 20",
            "9 октября 2026", 1791504000000, 1791547200000, {}, {}, {}, {}, {},
            modifier = Modifier.padding(insets), photosPreparing = photosPreparing)
    }
}

@Preview @Composable private fun ShopCheckInFormLightPreview() = ShopCheckInFormPreview(false)
@Preview @Composable private fun ShopCheckInFormDarkPreview() = ShopCheckInFormPreview(true)
@Preview @Composable private fun ShopCheckInSuccessLightPreview() = ShopCheckInFormPreview(false, submitted = true)
@Preview @Composable private fun ShopCheckInSuccessDarkPreview() = ShopCheckInFormPreview(true, submitted = true)
@Preview @Composable private fun ShopCheckInUnconfirmedLightPreview() = ShopCheckInFormPreview(false, true)
@Preview @Composable private fun ShopCheckInUnconfirmedDarkPreview() = ShopCheckInFormPreview(true, true)
@Preview @Composable private fun ShopCheckInPhotosPreparingLightPreview() = ShopCheckInFormPreview(false, photosPreparing = true)
@Preview @Composable private fun ShopCheckInPhotosPreparingDarkPreview() = ShopCheckInFormPreview(true, photosPreparing = true)
@Preview @Composable private fun ShopCheckInLoadingLightPreview() = ShopCheckInFormPreview(false, loading = true)
@Preview @Composable private fun ShopCheckInLoadingDarkPreview() = ShopCheckInFormPreview(true, loading = true)
@Preview @Composable private fun ShopCheckInSendingLightPreview() = ShopCheckInFormPreview(false, submitting = true)
@Preview @Composable private fun ShopCheckInSendingDarkPreview() = ShopCheckInFormPreview(true, submitting = true)

/** The server's own words win; otherwise a status the user can act on, else the generic "saved, try again" text. */
@Composable
private fun submitFailureText(failure: ShopCheckInSubmitFailure): String = failure.serverMessage ?: stringResource(when (failure.status) {
    401 -> Res.string.shop_checkin_error_unauthorized
    403 -> Res.string.shop_checkin_error_forbidden
    404 -> Res.string.shop_checkin_error_shop_gone
    413 -> Res.string.shop_checkin_error_photos_too_large
    429 -> Res.string.shop_checkin_error_too_many_requests
    else -> Res.string.shop_checkin_form_submit_error
})
