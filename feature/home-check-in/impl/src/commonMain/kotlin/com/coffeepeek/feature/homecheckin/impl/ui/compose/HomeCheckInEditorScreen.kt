package com.coffeepeek.feature.homecheckin.impl.ui.compose

import com.coffeepeek.core.designsystem.component.CoffeePeekBackHandler
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.homecheckin.impl.resources.Res
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_validation_summary
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_bean
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_brew_method
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_choose_bean
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_choose_date
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_dose_description
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_dose_hint
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_dose_label
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_drink
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_drink_hint
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_editor_close
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_editor_note
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_editor_title
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_home
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_keep_photos
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_load_failed
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_loading
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_methods_loading
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_methods_retry
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_note_hint
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_optional
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_photo_upload_failed
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_prepared_date
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_retry
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_save
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_saving
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekFormStep
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.domain.model.*
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
    LaunchedEffect(visible.isSaving) { currentDismiss(!visible.isSaving) }
    CoffeePeekBackHandler(enabled = !visible.isClosed) {
        when {
            visible.isAccessRevoked -> currentEvent(HomeCheckInEditorEvent.Close)
            visible.isBeanPickerOpen -> viewModel.onAction(HomeCheckInEditorAction.CloseBeanPicker)
            else -> viewModel.onAction(HomeCheckInEditorAction.Close)
        }
    }
    HomeCheckInEditorScreenContent(visible, viewModel::onAction, today, formatDate, dateToInstant, formatMethod,
        media, pickerLoading, onAddPhotos, modifier)
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
) {
    if (state.isClosed || state.isAccessRevoked) return
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val enabled = state.isReady && !state.isLoading && !state.isSaving
    val draft = state.draft
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    fun dispatch(action: HomeCheckInEditorAction) {
        focus.clearFocus(force = true); keyboard?.hide(); onAction(action)
    }
    if (showDatePicker && enabled && !state.isBeanPickerOpen) HomeCheckInEditorDatePicker(
        draft.preparedOn.orEmpty(), today, { showDatePicker = false },
    ) { date ->
        // Do not even convert the current day: keep the exact original instant and offset.
        if (date != draft.preparedOn) onAction(HomeCheckInEditorAction.DateChosen(date, dateToInstant(date)))
        showDatePicker = false
    }
    // Android IME space is consumed once, around header, scroll body and save footer together.
    Box(Modifier.imePadding().then(modifier)) {
        if (state.isBeanPickerOpen && enabled) {
            HomeCheckInBeanPicker(state, { action ->
                when (action) {
                    is HomeCheckInEditorAction.BeanQueryChanged, HomeCheckInEditorAction.SearchBeans -> onAction(action)
                    else -> dispatch(action)
                }
            }, media.emptyBeanArtwork)
            return@Box
        }
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (state.entry == null) Res.string.home_check_in_home else Res.string.home_check_in_editor_title),
                    Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                IconButton(onClick = { dispatch(HomeCheckInEditorAction.Close) }, enabled = !state.isSaving,
                    modifier = Modifier.size(44.dp)) {
                    Icon(CoffeePeekIcons.Close, stringResource(Res.string.home_check_in_editor_close), Modifier.size(20.dp))
                }
            }
            if (!state.isReady) {
                Column(Modifier.weight(1f).fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (state.isLoading) CoffeePeekLoader(stringResource(Res.string.home_check_in_loading))
                    state.loadIssue?.let {
                        Text(if (it == HomePreparationIssue.RequestFailed || it == HomePreparationIssue.InvalidResponse)
                            stringResource(Res.string.home_check_in_load_failed) else it.editorText(),
                            color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        TextButton(onClick = { onAction(HomeCheckInEditorAction.RetryDetail) }, enabled = !state.isLoading) {
                            Text(stringResource(Res.string.home_check_in_retry))
                        }
                    }
                }
                return@Column
            }
            val saveText = state.saveIssue?.editorText()
            val photoUploadText = stringResource(Res.string.home_check_in_photo_upload_failed)
            val summary = if (state.fieldErrors.isNotEmpty()) pluralStringResource(Res.plurals.home_check_in_validation_summary,
                    state.fieldErrors.size, state.fieldErrors.size)
                // Production reported any failed photo upload with one clear message, whatever the transport reason.
                else if (state.saveIssue != null && state.failedOperation == HomePreparationOperation.PhotoUpload) photoUploadText
                else saveText
            summary?.let { HomeCheckInEditorErrorSummary(it, Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                HomeCheckInEditorVisibility(draft.visibility == HomePreparationVisibility.Public, enabled) {
                    onAction(HomeCheckInEditorAction.VisibilityChanged(if (it) HomePreparationVisibility.Public else HomePreparationVisibility.Private))
                }
                state.fieldErrors[HomePreparationField.Visibility]?.let { HomeCheckInEditorFieldError(it.editorText()) }
                HomeCheckInEditorField("", state.fieldErrors[HomePreparationField.Rating]?.editorText()) {
                    HomeCheckInEditorRating(draft.tasteScore, enabled, media.ratingArtwork) {
                        onAction(HomeCheckInEditorAction.TasteRatingTapped(it))
                    }
                }
                Column {
                    @Composable fun Step(field: HomePreparationField, label: String, last: Boolean = false,
                        content: @Composable (String?) -> Unit) {
                        val error = state.fieldErrors[field]?.editorText()
                        CoffeePeekFormStep(optional = field != HomePreparationField.Date, last = last,
                            isError = error != null, optionalDescription = stringResource(Res.string.home_check_in_optional)) {
                            HomeCheckInEditorField(label, error) { content(error) }
                        }
                    }
                    Step(HomePreparationField.Date, stringResource(Res.string.home_check_in_prepared_date)) { error ->
                        HomeCheckInEditorSelect(formatDate(draft.preparedOn.orEmpty()), CoffeePeekIcons.Calendar,
                            stringResource(Res.string.home_check_in_choose_date), enabled, error) {
                            focus.clearFocus(); keyboard?.hide(); showDatePicker = true
                        }
                    }
                    Step(HomePreparationField.Method, stringResource(Res.string.home_check_in_brew_method)) { error ->
                        HomeCheckInEditorMethod(draft.brewMethodCode, state.methods, enabled && !state.methodsLoading,
                            error, formatMethod, media.methodArtwork, { onAction(HomeCheckInEditorAction.MethodChanged(it)) })
                        if (state.methodsLoading) Text(stringResource(Res.string.home_check_in_methods_loading), style = MaterialTheme.typography.bodySmall)
                        if (state.methodsIssue != null) TextButton(onClick = { onAction(HomeCheckInEditorAction.RetryMethods) }, enabled = enabled) {
                            Text(stringResource(Res.string.home_check_in_methods_retry))
                        }
                    }
                    Step(HomePreparationField.Bean, stringResource(Res.string.home_check_in_bean)) { error ->
                        HomeCheckInEditorSelect(draft.customBeanName ?: state.beanSelectionName ?: stringResource(Res.string.home_check_in_choose_bean),
                            CoffeePeekIcons.CoffeeBean, stringResource(Res.string.home_check_in_choose_bean), enabled, error) {
                            dispatch(HomeCheckInEditorAction.OpenBeanPicker)
                        }
                    }
                    Step(HomePreparationField.Dose, stringResource(Res.string.home_check_in_dose_label)) { error ->
                        HomeCheckInEditorInput(draft.doseGrams.orEmpty(), { onAction(HomeCheckInEditorAction.DoseChanged(it)) },
                            stringResource(Res.string.home_check_in_dose_description), stringResource(Res.string.home_check_in_dose_hint),
                            enabled, error, keyboardType = KeyboardType.Decimal, maxLength = 8)
                    }
                    val durationError = state.fieldErrors[HomePreparationField.Duration]?.editorText()
                    CoffeePeekFormStep(optional = true, isError = durationError != null,
                        optionalDescription = stringResource(Res.string.home_check_in_optional)) {
                        HomeCheckInEditorDuration(draft.brewDurationSeconds, enabled, durationError) { onAction(HomeCheckInEditorAction.DurationChanged(it)) }
                    }
                    Step(HomePreparationField.Note, stringResource(Res.string.home_check_in_editor_note)) { error ->
                        HomeCheckInEditorInput(draft.text.orEmpty(), { onAction(HomeCheckInEditorAction.NoteChanged(it)) },
                            stringResource(Res.string.home_check_in_editor_note), stringResource(Res.string.home_check_in_note_hint),
                            enabled, error, multiline = true, maxLength = 1000)
                    }
                    Step(HomePreparationField.Drink, stringResource(Res.string.home_check_in_drink)) { error ->
                        HomeCheckInEditorInput(draft.customDrinkName ?: state.entry?.drinkName.takeIf { draft.drinkSlug != null }.orEmpty(),
                            { onAction(HomeCheckInEditorAction.DrinkNameChanged(it)) }, stringResource(Res.string.home_check_in_drink),
                            stringResource(Res.string.home_check_in_drink_hint), enabled, error, maxLength = 100)
                    }
                    Step(HomePreparationField.Photos, "", last = true) {
                        if (state.entry != null) Text(stringResource(Res.string.home_check_in_keep_photos),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else {
                            HomeCheckInEditorPhotos(draft.photos, enabled, pickerLoading, media.photo, {
                                focus.clearFocus(); keyboard?.hide(); onAddPhotos(it)
                            }, { onAction(HomeCheckInEditorAction.RemovePhoto(it)) })
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                CoffeePeekButton(stringResource(if (state.isSaving) Res.string.home_check_in_saving else Res.string.home_check_in_save),
                    { dispatch(HomeCheckInEditorAction.Save) }, enabled = enabled && !pickerLoading)
            }
        }
    }
}

/** Fixture ports only; real artwork/decoding will come from application composition. */
internal fun previewHomeEditorMedia() = HomeCheckInEditorMedia(
    { Icon(CoffeePeekIcons.Coffee, null, it) }, { Icon(CoffeePeekIcons.Search, null, it) },
    { _, modifier -> Icon(CoffeePeekIcons.Coffee, null, modifier, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
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
