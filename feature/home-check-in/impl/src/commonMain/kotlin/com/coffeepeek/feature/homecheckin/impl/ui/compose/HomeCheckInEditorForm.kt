package com.coffeepeek.feature.homecheckin.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.coffeepeek.core.designsystem.component.CoffeePeekBackHandler
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekCheckInProgress
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationField
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationVisibility
import com.coffeepeek.feature.homecheckin.impl.resources.*
import com.coffeepeek.feature.homecheckin.impl.ui.compose.component.*
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.*
import com.coffeepeek.feature.homecheckin.impl.ui.data.HomeCheckInEditorMedia
import com.coffeepeek.feature.homecheckin.impl.ui.data.editorText

@Composable
internal fun HomeCheckInEditorForm(
    state: HomeCheckInEditorState, onAction: (HomeCheckInEditorAction) -> Unit, today: String,
    formatDate: (String) -> String, dateToInstant: (String) -> String, formatMethod: (String) -> String,
    media: HomeCheckInEditorMedia, pickerLoading: Boolean, onAddPhotos: (Int) -> Unit,
    modifier: Modifier, onBeforeSave: () -> Unit,
    initialStep: HomeCheckInEditorStep = HomeCheckInEditorStep.Experience,
) {
    var step by rememberSaveable(state.entry?.id) { mutableStateOf(initialStep) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    fun changeStep(value: HomeCheckInEditorStep) {
        focus.clearFocus(force = true)
        keyboard?.hide()
        step = value
    }
    val previous = step.previousHomeEditorStep(state.entry != null)
    LaunchedEffect(state.saveAttempt, state.fieldErrors, state.failedOperation) {
        if (state.entry == null && !state.isSaving) state.homeEditorErrorStep()?.let(::changeStep)
    }
    CoffeePeekBackHandler(enabled = previous != null && !state.isBeanPickerOpen) {
        if (!state.isSaving && !pickerLoading) previous?.let(::changeStep)
    }
    val onBack = previous?.let { { if (!state.isSaving && !pickerLoading) changeStep(it) } }
    HomeCheckInEditorFormFields(state, onAction, today, formatDate, dateToInstant, formatMethod,
        media, pickerLoading, onAddPhotos, step, onBack,
        { changeStep(HomeCheckInEditorStep.Details) }, {
            if (state.entry == null) state.homeEditorErrorStep()?.let(::changeStep)
            onBeforeSave()
            focus.clearFocus(force = true)
            keyboard?.hide()
            onAction(HomeCheckInEditorAction.Save)
        }, modifier)
}

@Composable
private fun HomeCheckInEditorFormFields(
    state: HomeCheckInEditorState, onAction: (HomeCheckInEditorAction) -> Unit, today: String,
    formatDate: (String) -> String, dateToInstant: (String) -> String, formatMethod: (String) -> String,
    media: HomeCheckInEditorMedia, pickerLoading: Boolean, onAddPhotos: (Int) -> Unit,
    step: HomeCheckInEditorStep, onBack: (() -> Unit)?, onNext: () -> Unit, onSave: () -> Unit,
    modifier: Modifier,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var recipeExpanded by rememberSaveable(state.entry?.id) { mutableStateOf<Boolean?>(null) }
    val enabled = state.isReady && !state.isLoading && !state.isSaving
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    fun clearInputFocus() {
        focus.clearFocus(force = true)
        keyboard?.hide()
    }
    fun dispatch(action: HomeCheckInEditorAction) {
        clearInputFocus()
        onAction(action)
    }
    if (showDatePicker && enabled && !state.isBeanPickerOpen) HomeEditorDateDialog(
        state, today, dateToInstant, onAction, { showDatePicker = false },
    )
    Box(Modifier.imePadding().then(modifier)) {
        if (state.isBeanPickerOpen && enabled) {
            HomeCheckInEditorBeanPickerContent(state, media, onAction, ::dispatch)
            return@Box
        }
        HomeCheckInEditorFormLayout(state, enabled && !pickerLoading, onAction, step, onBack, onNext, onSave,
            { dispatch(HomeCheckInEditorAction.Close) }) {
            HomeCheckInEditorStepFields(state, enabled, step, formatDate, media, pickerLoading, onAction,
                {
                    clearInputFocus()
                    showDatePicker = true
                },
                {
                    clearInputFocus()
                    onAddPhotos(it)
                })
            if (state.entry != null || step == HomeCheckInEditorStep.Details) {
                HomeCheckInEditorRecipeFields(state, enabled, formatMethod, media, onAction, recipeExpanded,
                    { recipeExpanded = it }, { dispatch(HomeCheckInEditorAction.OpenBeanPicker) })
                if (state.entry == null) HomeCheckInEditorVisibilityField(state, enabled, onAction)
            }
        }
    }
}

@Composable
private fun HomeEditorDateDialog(state: HomeCheckInEditorState, today: String, dateToInstant: (String) -> String,
    onAction: (HomeCheckInEditorAction) -> Unit, onDismiss: () -> Unit) {
    HomeCheckInEditorDatePicker(state.draft.preparedOn.orEmpty(), today, onDismiss) { date ->
        // Keep the existing exact instant when the selected civil date did not change.
        if (date != state.draft.preparedOn) onAction(HomeCheckInEditorAction.DateChosen(date, dateToInstant(date)))
        onDismiss()
    }
}

@Composable
private fun HomeCheckInEditorFormLayout(
    state: HomeCheckInEditorState, enabled: Boolean, onAction: (HomeCheckInEditorAction) -> Unit,
    step: HomeCheckInEditorStep, onBack: (() -> Unit)?, onNext: () -> Unit, onSave: () -> Unit,
    onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        HomeCheckInEditorHeader(state, step, onClose, onBack)
        if (!state.isReady) {
            HomeCheckInEditorLoadStatus(state, { onAction(HomeCheckInEditorAction.RetryDetail) }, Modifier.weight(1f))
            return@Column
        }
        if (state.entry == null) CoffeePeekCheckInProgress(
            step.ordinal + 1, 2, stringResource(Res.string.home_check_in_step_progress, step.ordinal + 1, 2),
            Modifier.padding(horizontal = CoffeePeekDimens.spacing4).padding(bottom = CoffeePeekDimens.spacing4),
        )
        HomeCheckInEditorSaveError(state)
        key(step) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing2),
                verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing6), content = content)
        }
        if (state.entry == null && step == HomeCheckInEditorStep.Experience) {
            if (WindowInsets.ime.getBottom(LocalDensity.current) == 0) {
                CoffeePeekButton(stringResource(Res.string.home_check_in_step_next), onNext,
                    Modifier.padding(CoffeePeekDimens.spacing4), enabled = enabled)
            }
        } else HomeCheckInEditorFooter(state, enabled, onAction, onSave)
    }
}

@Composable
private fun HomeCheckInEditorStepFields(
    state: HomeCheckInEditorState, enabled: Boolean, step: HomeCheckInEditorStep,
    formatDate: (String) -> String, media: HomeCheckInEditorMedia, pickerLoading: Boolean,
    onAction: (HomeCheckInEditorAction) -> Unit, onDate: () -> Unit, onAddPhotos: (Int) -> Unit,
) {
    when {
        state.entry != null -> HomeCheckInEditorPrimaryFields(state, enabled, formatDate, media,
            pickerLoading, onAction, onDate, onAddPhotos)
        step == HomeCheckInEditorStep.Experience ->
            HomeCheckInEditorExperienceFields(state, enabled, media, pickerLoading, onAction, onAddPhotos)
        else -> HomeCheckInEditorDetailsFields(state, enabled, formatDate, onAction, onDate)
    }
}

@Composable
internal fun HomeCheckInEditorVisibilityField(state: HomeCheckInEditorState, enabled: Boolean,
    onAction: (HomeCheckInEditorAction) -> Unit) {
    Column {
        HomeCheckInEditorVisibility(state.draft.visibility == HomePreparationVisibility.Public, enabled) {
            onAction(HomeCheckInEditorAction.VisibilityChanged(if (it) HomePreparationVisibility.Public else HomePreparationVisibility.Private))
        }
        state.fieldErrors[HomePreparationField.Visibility]?.let { HomeCheckInEditorFieldError(it.editorText()) }
    }
}

@Composable
private fun HomeEditorStepPreview(dark: Boolean, step: HomeCheckInEditorStep) =
    com.coffeepeek.core.designsystem.theme.CoffeePeekTheme(darkTheme = dark) {
        androidx.compose.material3.Surface {
            HomeCheckInEditorForm(previewHomeEditorState(), {}, "2026-10-17", { "17 октября 2026" },
                { it + "T00:00:00Z" }, { it }, previewHomeEditorMedia(), false, {}, Modifier, {}, step)
        }
    }

@org.jetbrains.compose.ui.tooling.preview.Preview @Composable
private fun HomeEditorExperienceLightPreview() = HomeEditorStepPreview(false, HomeCheckInEditorStep.Experience)
@org.jetbrains.compose.ui.tooling.preview.Preview @Composable
private fun HomeEditorExperienceDarkPreview() = HomeEditorStepPreview(true, HomeCheckInEditorStep.Experience)
@org.jetbrains.compose.ui.tooling.preview.Preview @Composable
private fun HomeEditorDetailsLightPreview() = HomeEditorStepPreview(false, HomeCheckInEditorStep.Details)
@org.jetbrains.compose.ui.tooling.preview.Preview @Composable
private fun HomeEditorDetailsDarkPreview() = HomeEditorStepPreview(true, HomeCheckInEditorStep.Details)
