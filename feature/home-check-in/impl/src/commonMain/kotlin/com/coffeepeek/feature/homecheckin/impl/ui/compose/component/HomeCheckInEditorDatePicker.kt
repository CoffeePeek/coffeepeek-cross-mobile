package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.material3.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.homecheckin.impl.resources.Res
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_cancel
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_done
import androidx.compose.runtime.*
import org.jetbrains.compose.resources.stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Material's millis represent a civil day at UTC midnight, not the eventual preparation instant. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
internal fun HomeCheckInEditorDatePicker(date: String, today: String, onDismiss: () -> Unit, onSelected: (String) -> Unit) {
    val maximum = remember(today) { LocalDate.parse(today).plus(1, DateTimeUnit.DAY).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() - 1 }
    val selected = remember(date) { runCatching { LocalDate.parse(date).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() }.getOrNull() }
    val picker = rememberDatePickerState(initialSelectedDateMillis = selected, selectableDates = remember(maximum) {
        object : SelectableDates { override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= maximum }
    })
    DatePickerDialog(onDismissRequest = onDismiss,
        confirmButton = { TextButton(enabled = picker.selectedDateMillis != null, onClick = {
            picker.selectedDateMillis?.let { onSelected(Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date.toString()) }
        }) { Text(stringResource(Res.string.home_check_in_done)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.home_check_in_cancel)) } },
    ) { DatePicker(picker, showModeToggle = false) }
}

@Preview @Composable private fun HomeCheckInEditorDatePickerPreview() = CoffeePeekTheme {
    HomeCheckInEditorDatePicker("2026-10-17", "2026-10-17", {}, {})
}
