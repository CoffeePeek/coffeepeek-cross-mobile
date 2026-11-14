package com.coffeepeek.core.designsystem.component

import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

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
                override fun isSelectableDate(utcTimeMillis: Long) = maximumMillis == null || utcTimeMillis <= maximumMillis
            }
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = state.selectedDateMillis != null, onClick = { state.selectedDateMillis?.let(onConfirm) }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancelLabel) } },
    ) { DatePicker(state, showModeToggle = false, colors = coffeePeekDatePickerColors()) }
}

/** Unfilled dates need a foreground role; selected dates retain the brand fill. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun coffeePeekDatePickerColors() = DatePickerDefaults.colors(
    todayContentColor = MaterialTheme.colorScheme.primary,
    todayDateBorderColor = MaterialTheme.colorScheme.primary,
    currentYearContentColor = MaterialTheme.colorScheme.primary,
    navigationContentColor = MaterialTheme.colorScheme.onSurface,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoffeePeekDatePickerPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    DatePicker(rememberDatePickerState(initialSelectedDateMillis = 1794614400000L),
        showModeToggle = false, colors = coffeePeekDatePickerColors())
}

@Preview @Composable private fun CoffeePeekDatePickerLightPreview() = CoffeePeekDatePickerPreview(false)
@Preview @Composable private fun CoffeePeekDatePickerDarkPreview() = CoffeePeekDatePickerPreview(true)
