package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationField
import com.coffeepeek.feature.homecheckin.impl.resources.*
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.HomeCheckInEditorAction
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.HomeCheckInEditorState
import com.coffeepeek.feature.homecheckin.impl.ui.data.HomeCheckInEditorMedia
import com.coffeepeek.feature.homecheckin.impl.ui.data.editorText
import com.coffeepeek.feature.homecheckin.impl.ui.compose.previewHomeEditorMedia
import com.coffeepeek.feature.homecheckin.impl.ui.compose.previewHomeEditorState
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun HomeCheckInEditorPrimaryFields(
    state: HomeCheckInEditorState, enabled: Boolean, formatDate: (String) -> String,
    media: HomeCheckInEditorMedia, pickerLoading: Boolean,
    onAction: (HomeCheckInEditorAction) -> Unit, onDate: () -> Unit,
    onAddPhotos: (Int) -> Unit,
) {
    val draft = state.draft
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        HomeCheckInEditorField(stringResource(Res.string.home_check_in_prepared_date),
            state.fieldErrors[HomePreparationField.Date]?.editorText()) {
            HomeCheckInEditorSelect(formatDate(draft.preparedOn.orEmpty()), CoffeePeekIcons.Calendar,
                stringResource(Res.string.home_check_in_choose_date), enabled,
                state.fieldErrors[HomePreparationField.Date]?.editorText(), onClick = onDate)
        }
        HomeCheckInEditorField(stringResource(Res.string.home_check_in_drink),
            state.fieldErrors[HomePreparationField.Drink]?.editorText()) {
            HomeCheckInEditorInput(draft.customDrinkName ?: state.entry?.drinkName.takeIf { draft.drinkSlug != null }.orEmpty(),
                { onAction(HomeCheckInEditorAction.DrinkNameChanged(it)) }, stringResource(Res.string.home_check_in_drink),
                stringResource(Res.string.home_check_in_drink_hint), enabled,
                state.fieldErrors[HomePreparationField.Drink]?.editorText(), maxLength = 100)
        }
        HomeCheckInEditorField("", state.fieldErrors[HomePreparationField.Rating]?.editorText()) {
            HomeCheckInEditorRating(draft.tasteScore, enabled, media.ratingArtwork) {
                onAction(HomeCheckInEditorAction.TasteRatingTapped(it))
            }
        }
        HomeCheckInEditorField(stringResource(Res.string.home_check_in_editor_note),
            state.fieldErrors[HomePreparationField.Note]?.editorText()) {
            HomeCheckInEditorInput(draft.text.orEmpty(), { onAction(HomeCheckInEditorAction.NoteChanged(it)) },
                stringResource(Res.string.home_check_in_editor_note), stringResource(Res.string.home_check_in_note_hint),
                enabled, state.fieldErrors[HomePreparationField.Note]?.editorText(), multiline = true, maxLength = 1000)
        }
        if (state.entry != null) Text(stringResource(Res.string.home_check_in_keep_photos),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else HomeCheckInEditorPhotos(draft.photos, enabled, pickerLoading, media.photo, onAddPhotos,
            { onAction(HomeCheckInEditorAction.RemovePhoto(it)) },
            { from, to -> onAction(HomeCheckInEditorAction.MovePhoto(from, to)) })
    }
}

@Composable private fun HomeEditorPrimaryPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { Column(Modifier.padding(16.dp)) {
        HomeCheckInEditorPrimaryFields(previewHomeEditorState(), true, { "17 октября 2026" },
            previewHomeEditorMedia(), false, {}, {}, {})
    } }
}

@Preview @Composable private fun HomeEditorPrimaryLightPreview() = HomeEditorPrimaryPreview(false)
@Preview @Composable private fun HomeEditorPrimaryDarkPreview() = HomeEditorPrimaryPreview(true)
