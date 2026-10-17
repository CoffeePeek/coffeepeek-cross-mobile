package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationUpload
import com.coffeepeek.feature.homecheckin.impl.R
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.*

@Composable
internal fun HomeCheckInEditorCutout(
    state: HomeCheckInPhotoState, enabled: Boolean, onAction: (HomeCheckInEditorAction) -> Unit,
    photo: @Composable (HomePreparationUpload, Modifier, Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.home_check_in_cutout_title), style = MaterialTheme.typography.labelLarge)
        Text(stringResource(when {
            state.isProcessing -> R.string.home_check_in_cutout_processing
            state.isSkipped -> R.string.home_check_in_cutout_skipped
            state.hasError -> R.string.home_check_in_cutout_failed
            state.candidates.size > 1 && state.selected == null -> R.string.home_check_in_cutout_choose
            else -> R.string.home_check_in_cutout_check
        }), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        if (state.isProcessing) CoffeePeekLoader(stringResource(R.string.home_check_in_cutout_processing), size = 24.dp)
        if (state.candidates.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.candidates.forEachIndexed { index, upload ->
                val selected = state.selected == upload
                val description = stringResource(R.string.home_check_in_cutout_candidate, index + 1)
                val shape = RoundedCornerShape(12.dp)
                photo(upload, Modifier.size(96.dp).clip(shape).border(if (selected) 2.dp else 1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, shape)
                    .selectable(selected, enabled = enabled && !state.isProcessing, role = Role.RadioButton) {
                        onAction(HomeCheckInEditorAction.ChooseDrinkPhoto(index))
                    }.semantics { contentDescription = description }.padding(8.dp), true)
            }
        }
        FlowRow {
            if (!state.isProcessing && (state.hasError || state.isSkipped)) TextButton(
                onClick = { onAction(HomeCheckInEditorAction.RetryDrinkPhoto) }, enabled = enabled) {
                Text(stringResource(R.string.home_check_in_cutout_retry))
            }
            if (!state.isSkipped) TextButton(onClick = { onAction(HomeCheckInEditorAction.SkipDrinkPhoto) }, enabled = enabled) {
                Text(stringResource(R.string.home_check_in_cutout_skip))
            }
        }
    }
}

@PreviewLightDark @Composable private fun HomeCheckInEditorCutoutPreview() = CoffeePeekTheme {
    Surface { HomeCheckInEditorCutout(HomeCheckInPhotoState(hasError = true), true, {}, { _, modifier, _ -> Box(modifier) }) }
}
