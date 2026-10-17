package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.impl.R
import java.time.LocalDate
import java.time.ZoneOffset

/** Material's millis represent a civil day at UTC midnight, not the eventual preparation instant. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeCheckInEditorDatePicker(date: String, today: String, onDismiss: () -> Unit, onSelected: (String) -> Unit) {
    val maximum = remember(today) { LocalDate.parse(today).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() - 1 }
    val selected = remember(date) { runCatching { LocalDate.parse(date).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull() }
    val picker = rememberDatePickerState(initialSelectedDateMillis = selected, selectableDates = remember(maximum) {
        object : SelectableDates { override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= maximum }
    })
    DatePickerDialog(onDismissRequest = onDismiss,
        confirmButton = { TextButton(enabled = picker.selectedDateMillis != null, onClick = {
            picker.selectedDateMillis?.let { onSelected(java.time.Instant.ofEpochMilli(it).atOffset(ZoneOffset.UTC).toLocalDate().toString()) }
        }) { Text(stringResource(R.string.home_check_in_done)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.home_check_in_cancel)) } },
    ) { DatePicker(picker, showModeToggle = false) }
}

@PreviewLightDark @Composable private fun HomeCheckInEditorDatePickerPreview() = CoffeePeekTheme {
    HomeCheckInEditorDatePicker("2026-10-17", "2026-10-17", {}, {})
}
