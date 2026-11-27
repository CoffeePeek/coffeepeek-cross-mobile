@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.feature.homecheckin.impl.ui.compose

import com.coffeepeek.core.designsystem.component.CoffeePeekBackHandler
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.homecheckin.impl.resources.Res
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_validation_summary
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_editor_close
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_editor_title
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_home
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_load_failed
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_loading
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_photo_upload_failed
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_retry
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_save
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_saving
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton as IconButton
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import org.jetbrains.compose.resources.pluralStringResource
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekCoffeeIcon
import com.coffeepeek.core.designsystem.component.CoffeePeekIcon
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.impl.resources.*
import com.coffeepeek.feature.homecheckin.impl.ui.HomeCheckInEditorViewModel
import com.coffeepeek.feature.homecheckin.impl.ui.compose.component.*
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.*
import com.coffeepeek.feature.homecheckin.impl.ui.data.*

/** Internal until the app's owner/lease, picker and analytics adapters are ready. */
@Composable
internal fun HomeCheckInEditorScreen(
    viewModel: HomeCheckInEditorViewModel,
    accessAllowed: Boolean,
    today: String,
    formatDate: (String) -> String,
    dateToInstant: (String) -> String,
    formatMethod: (String) -> String,
    media: HomeCheckInEditorMedia,
    pickerLoading: Boolean,
    onAddPhotos: (Int) -> Unit,
    onDismissAllowedChange: (Boolean) -> Unit,
    onEvent: (HomeCheckInEditorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentAccess by rememberUpdatedState(accessAllowed)
    val currentEvent by rememberUpdatedState(onEvent)
    val currentDismiss by rememberUpdatedState(onDismissAllowedChange)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { if (currentAccess && viewModel.canDeliver(it)) currentEvent(it.event) }
        }
    }
    LaunchedEffect(viewModel, accessAllowed) {
        if (!accessAllowed) viewModel.onAction(HomeCheckInEditorAction.RevokeAccess)
    }
    val visible = if (accessAllowed) state else HomeCheckInEditorState(isAccessRevoked = true)
    LaunchedEffect(visible.isSaving, visible.savedEntry, visible.saveAttempt) {
        currentDismiss(!visible.isSaving && visible.savedEntry == null)
    }
    CoffeePeekBackHandler(enabled = !visible.isClosed) {
        when {
            visible.isAccessRevoked -> currentEvent(HomeCheckInEditorEvent.Close)
            visible.savedEntry != null -> viewModel.onAction(HomeCheckInEditorAction.FinishSaved)
            visible.isBeanPickerOpen -> viewModel.onAction(HomeCheckInEditorAction.CloseBeanPicker)
            else -> viewModel.onAction(HomeCheckInEditorAction.Close)
        }
    }
    HomeCheckInEditorScreenContent(visible, viewModel::onAction, today, formatDate, dateToInstant, formatMethod,
        media, pickerLoading, onAddPhotos, modifier, onBeforeSave = { currentDismiss(false) })
}

/** Body only. The caller owns the full-screen/modal container and its navigation/system-bar insets. */
@Composable
internal fun HomeCheckInEditorScreenContent(
    state: HomeCheckInEditorState,
    onAction: (HomeCheckInEditorAction) -> Unit,
    today: String,
    formatDate: (String) -> String,
    dateToInstant: (String) -> String,
    formatMethod: (String) -> String,
    media: HomeCheckInEditorMedia,
    pickerLoading: Boolean,
    onAddPhotos: (remaining: Int) -> Unit,
    modifier: Modifier = Modifier,
    onBeforeSave: () -> Unit = {},
) {
    if (state.isClosed || state.isAccessRevoked) return
    state.savedEntry?.let { saved ->
        HomeCheckInSavedContent(saved, state.savedVisibilityUnconfirmed,
            { onAction(HomeCheckInEditorAction.FinishSaved) }, modifier)
        return
    }
    HomeCheckInEditorForm(state, onAction, today, formatDate, dateToInstant, formatMethod,
        media, pickerLoading, onAddPhotos, modifier, onBeforeSave)
}

@Composable
internal fun HomeCheckInEditorBeanPickerContent(state: HomeCheckInEditorState, media: HomeCheckInEditorMedia,
    onAction: (HomeCheckInEditorAction) -> Unit, dispatch: (HomeCheckInEditorAction) -> Unit) {
    HomeCheckInBeanPicker(state, { action ->
        when (action) {
            is HomeCheckInEditorAction.BeanQueryChanged, HomeCheckInEditorAction.SearchBeans -> onAction(action)
            else -> dispatch(action)
        }
    }, media.emptyBeanArtwork, media.shelfBagImage)
}

@Composable
internal fun HomeCheckInEditorHeader(state: HomeCheckInEditorState, step: HomeCheckInEditorStep,
    onClose: () -> Unit, onBack: (() -> Unit)?) {
    if (state.entry == null && state.isReady) {
        CoffeePeekTopBar(
            title = stringResource(if (step == HomeCheckInEditorStep.Experience)
                Res.string.home_check_in_step_impressions else Res.string.home_check_in_step_details),
            backDescription = stringResource(Res.string.home_check_in_step_back),
            onBack = (onBack ?: onClose).takeUnless { state.isSaving },
            titleMaxLines = 2,
        )
        return
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing4)
        .padding(bottom = CoffeePeekDimens.spacing4), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(if (state.entry == null) Res.string.home_check_in_home else Res.string.home_check_in_editor_title),
            Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        IconButton(onClick = onClose, enabled = !state.isSaving, modifier = Modifier.size(CoffeePeekDimens.minimumTouchTarget)) {
            CoffeePeekIcon(CoffeePeekIcons.Close, stringResource(Res.string.home_check_in_editor_close), Modifier.size(20.dp))
        }
    }
}

@Composable
internal fun HomeCheckInEditorLoadStatus(state: HomeCheckInEditorState, onRetry: () -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing4), horizontalAlignment = Alignment.CenterHorizontally) {
        if (state.isLoading) CoffeePeekLoader(stringResource(Res.string.home_check_in_loading))
        state.loadIssue?.let {
            Text(if (it == HomePreparationIssue.RequestFailed || it == HomePreparationIssue.InvalidResponse)
                stringResource(Res.string.home_check_in_load_failed) else it.editorText(),
                color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            TextButton(onClick = onRetry, enabled = !state.isLoading) {
                Text(stringResource(Res.string.home_check_in_retry))
            }
        }
    }
}

@Composable
internal fun HomeCheckInEditorSaveError(state: HomeCheckInEditorState) {
    val summary = if (state.fieldErrors.isNotEmpty()) pluralStringResource(Res.plurals.home_check_in_validation_summary,
        state.fieldErrors.size, state.fieldErrors.size)
    else if (state.saveIssue != null && state.failedOperation == HomePreparationOperation.PhotoUpload)
        stringResource(Res.string.home_check_in_photo_upload_failed)
    else state.saveIssue?.editorText()
    summary?.let { HomeCheckInEditorErrorSummary(it, Modifier.padding(horizontal = CoffeePeekDimens.spacing4)
        .padding(bottom = CoffeePeekDimens.spacing4)) }
}

@Composable
internal fun HomeCheckInEditorFooter(state: HomeCheckInEditorState, enabled: Boolean,
    onAction: (HomeCheckInEditorAction) -> Unit, onSave: () -> Unit) {
    // Leave the available height to the form while typing, including in iOS full-screen dialogs.
    if (WindowInsets.ime.getBottom(LocalDensity.current) > 0) return
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(CoffeePeekDimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        if (state.entry != null) HomeCheckInEditorVisibilityField(state, enabled, onAction)
        CoffeePeekButton(stringResource(if (state.isSaving) Res.string.home_check_in_saving else Res.string.home_check_in_save),
            onSave, enabled = enabled)
    }
}

/** Fixture ports only; real artwork/decoding will come from application composition. */
internal fun previewHomeEditorMedia() = HomeCheckInEditorMedia(
    { CoffeePeekCoffeeIcon(null, it) }, { CoffeePeekIcon(CoffeePeekIcons.Search, null, it) },
    { _, modifier -> CoffeePeekCoffeeIcon(null, modifier, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
    { _, modifier, _ -> Box(modifier) },
)

internal fun previewHomeEditorState() = HomeCheckInEditorState(isReady = true,
    draft = HomePreparationInput(preparedAt = "2026-10-17T07:30:00Z", preparedOn = "2026-10-17", tasteScore = 5))

@Preview @Composable private fun HomeCheckInEditorPreviewLight() = HomeCheckInEditorPreview(false)
@Preview @Composable private fun HomeCheckInEditorPreviewDark() = HomeCheckInEditorPreview(true)

@Composable private fun HomeCheckInEditorPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { HomeCheckInEditorScreenContent(previewHomeEditorState(), {}, "2026-10-17", { "17 октября 2026" },
        { it + "T00:00:00Z" }, { it }, previewHomeEditorMedia(), false, {}) }
}
@Preview @Composable private fun HomeCheckInEditorLightPreview() = HomeCheckInEditorPreview(false)
@Preview @Composable private fun HomeCheckInEditorDarkPreview() = HomeCheckInEditorPreview(true)
