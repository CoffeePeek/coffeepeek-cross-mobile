package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.impl.resources.*
import com.coffeepeek.feature.shelf.impl.ui.compose.previewToday
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfDay
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val DAY_MILLIS = 86_400_000L

/** Optional roast date; the picker works in UTC days and offers nothing after [today]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShelfRoastDateField(day: ShelfDay?, today: ShelfDay, onChange: (ShelfDay?) -> Unit, modifier: Modifier = Modifier) {
    var picking by remember { mutableStateOf(false) }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Text(stringResource(Res.string.shelf_form_roasted), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton({ picking = true }) { Text(day?.text(today) ?: stringResource(Res.string.shelf_form_roasted_pick)) }
    }
    if (!picking) return
    val state = rememberDatePickerState(initialSelectedDateMillis = day?.let { it.epochDay * DAY_MILLIS },
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis / DAY_MILLIS <= today.epochDay
        })
    DatePickerDialog(onDismissRequest = { picking = false },
        confirmButton = {
            TextButton({ onChange(state.selectedDateMillis?.let { ShelfDay.ofEpochDay(it / DAY_MILLIS) }); picking = false }) {
                Text(stringResource(Res.string.shelf_date_done))
            }
        },
        dismissButton = {
            TextButton({ onChange(null); picking = false }) { Text(stringResource(Res.string.shelf_form_roasted_clear)) }
        }) { DatePicker(state, showModeToggle = false) }
}

@Composable
private fun ShelfRoastDateFieldPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ShelfRoastDateField(ShelfDay(2026, 10, 12), previewToday, {},
        Modifier.background(MaterialTheme.colorScheme.background).padding(CoffeePeekDimens.spacing4))
}

@Preview @Composable private fun ShelfRoastDateFieldLightPreview() = ShelfRoastDateFieldPreview(false)
@Preview @Composable private fun ShelfRoastDateFieldDarkPreview() = ShelfRoastDateFieldPreview(true)
