package com.coffeepeek.core.designsystem.component

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * A date picker dialog: the platform's native wheels when it installs them, Material's picker otherwise.
 * Millis are the UTC-midnight value of a civil day; days after [maximumMillis] cannot be chosen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoffeePeekDatePickerDialog(
    title: String,
    initialMillis: Long?,
    maximumMillis: Long?,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val native = LocalCoffeePeekNativeDatePickerPresenter.current
    val start = initialMillis ?: maximumMillis
    if (native != null && start != null && native.Present(title, start, maximumMillis, false, onConfirm, onDismiss)) return
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        selectableDates = remember(maximumMillis) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis > 0 && (maximumMillis == null || utcTimeMillis <= maximumMillis)
            }
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = state.selectedDateMillis != null, onClick = { state.selectedDateMillis?.let(onConfirm) }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancelLabel) } },
    ) { DatePicker(state, showModeToggle = false) }
}
