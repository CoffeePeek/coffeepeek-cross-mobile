package com.coffeepeek.admin.ui.screen.shop

import com.coffeepeek.admin.ui.component.ReviewFormStep

import com.coffeepeek.admin.ui.screen.review.ConsumedDrinkField

import com.coffeepeek.domain.model.validateConsumedDrink

import com.coffeepeek.domain.model.ConsumedDrinkOption

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.checkin_action
import coffeepeek.composeapp.generated.resources.checkin_date_label
import coffeepeek.composeapp.generated.resources.checkin_date_picker_confirm
import coffeepeek.composeapp.generated.resources.checkin_date_picker_dismiss
import coffeepeek.composeapp.generated.resources.checkin_note_label
import coffeepeek.composeapp.generated.resources.checkin_note_placeholder
import coffeepeek.composeapp.generated.resources.checkin_photos_label
import coffeepeek.composeapp.generated.resources.checkin_public_switch_hint
import coffeepeek.composeapp.generated.resources.checkin_public_switch_title
import coffeepeek.composeapp.generated.resources.checkin_sheet_title
import org.jetbrains.compose.resources.stringResource
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.AppButton
import com.coffeepeek.admin.ui.component.PhotoAttachmentsSection
import com.coffeepeek.admin.ui.component.ReviewRatingCards
import com.coffeepeek.admin.ui.component.ReviewFormField
import com.coffeepeek.admin.ui.component.ReviewTextInput
import com.coffeepeek.admin.ui.component.SwipeDismissModalBottomSheet
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.MAX_REVIEW_PHOTOS
import com.coffeepeek.admin.utils.currentEpochMillis
import com.coffeepeek.admin.utils.formatVisitDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInBottomSheet(
    draft: CheckInDraft,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onDraftChange: (CheckInDraft) -> Unit,
    onSubmit: (CheckInDraft) -> Unit,
    placeName: String? = null,
    drinks: List<ConsumedDrinkOption> = emptyList(),
    drinksError: String? = null,
    onRetryDrinks: () -> Unit = {},
) {
    var noteError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = draft.visitMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= currentEpochMillis()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onDraftChange(draft.copy(visitMillis = it))
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(Res.string.checkin_date_picker_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.checkin_date_picker_dismiss))
                }
            },
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }

    SwipeDismissModalBottomSheet(
        onDismissRequest = {
            keyboardController?.hide()
            focusManager.clearFocus()
            onDismiss()
        },
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
            // ── Header ────────────────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
            ) {
                Text(
                    text = stringResource(Res.string.checkin_sheet_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
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
            }

            // ── Ratings ───────────────────────────────────────────────────────
            ReviewRatingCards(
                coffeeRating = draft.coffeeRating,
                serviceRating = draft.serviceRating,
                placeRating = draft.placeRating,
                onCoffeeRatingChange = { onDraftChange(draft.copy(coffeeRating = it)) },
                onServiceRatingChange = { onDraftChange(draft.copy(serviceRating = it)) },
                onPlaceRatingChange = { onDraftChange(draft.copy(placeRating = it)) },
            )

            Column {
                // ── Visit date ────────────────────────────────────────────────────
                ReviewFormStep {
                    ReviewFormField(label = stringResource(Res.string.checkin_date_label)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(CpDimens.radiusMd))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(CpDimens.radiusMd),
                                )
                                .clickable(enabled = !isLoading) { showDatePicker = true }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = CpIcons.Calendar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = formatVisitDate(draft.visitMillis),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                imageVector = CpIcons.ChevronDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                ReviewFormStep {
                    ReviewFormField(
                        label = stringResource(Res.string.checkin_note_label),
                        error = noteError,
                    ) {
                        ReviewTextInput(
                            value = draft.note,
                            onValueChange = { onDraftChange(draft.copy(note = it)); noteError = null },
                            placeholder = stringResource(Res.string.checkin_note_placeholder),
                            isError = noteError != null,
                            maxLength = 1000,
                            modifier = Modifier.heightIn(min = 80.dp),
                        )
                    }
                }

                // ── Photos (optional) ─────────────────────────────────────────────
                ReviewFormStep(optional = true) {
                    PhotoAttachmentsSection(
                        photos = draft.photos,
                        maxPhotos = MAX_REVIEW_PHOTOS,
                        onPhotosAdded = { added ->
                            val remaining = MAX_REVIEW_PHOTOS - draft.photos.size
                            if (remaining > 0) {
                                onDraftChange(draft.copy(photos = draft.photos + added.take(remaining)))
                            }
                        },
                        onRemovePhoto = { index ->
                            onDraftChange(
                                draft.copy(photos = draft.photos.filterIndexed { i, _ -> i != index })
                            )
                        },
                        title = stringResource(Res.string.checkin_photos_label),
                        hint = "Добавьте до $MAX_REVIEW_PHOTOS фото вашего визита.",
                    )
                }

                ReviewFormStep(optional = true) {
                    ConsumedDrinkField(
                        drinks, draft.drinkSlug, draft.customDrinkName, draft.drinkName, drinksError, onRetryDrinks,
                        { slug, name -> onDraftChange(draft.copy(drinkSlug = slug, customDrinkName = name)) },
                    )
                }
                // ── Public toggle ─────────────────────────────────────────────────
                ReviewFormStep(optional = true, last = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth().toggleable(
                            value = draft.isPublic, enabled = !isLoading, role = Role.Switch,
                            onValueChange = { onDraftChange(draft.copy(isPublic = it)) },
                        ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.checkin_public_switch_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(Res.string.checkin_public_switch_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(CpDimens.spacing3))
                        Switch(
                            checked = draft.isPublic,
                            onCheckedChange = null,
                            enabled = !isLoading,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                checkedBorderColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                            ),
                        )
                    }
                }
            }

            // ── Submit ────────────────────────────────────────────────────────
            AppButton(
                text = stringResource(Res.string.checkin_action),
                onClick = {
                    noteError = draft.validationError()
                    if (noteError != null) return@AppButton
                    onSubmit(draft)
                },
                enabled = !isLoading && validateConsumedDrink(draft.drinkSlug, draft.customDrinkName) == null,
            )
        }
    }
}
