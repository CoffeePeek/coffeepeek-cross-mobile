package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.homecheckin.impl.resources.Res
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_cancel
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_done
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_prepared_date
import androidx.compose.runtime.*
import org.jetbrains.compose.resources.stringResource
import com.coffeepeek.core.designsystem.component.CoffeePeekDatePickerDialog
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
@OptIn(ExperimentalTime::class)
@Composable
internal fun HomeCheckInEditorDatePicker(date: String, today: String, onDismiss: () -> Unit, onSelected: (String) -> Unit) {
    val maximum = remember(today) { LocalDate.parse(today).plus(1, DateTimeUnit.DAY).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() - 1 }
    val selected = remember(date) { runCatching { LocalDate.parse(date).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() }.getOrNull() }
    CoffeePeekDatePickerDialog(
        title = stringResource(Res.string.home_check_in_prepared_date),
        initialMillis = selected,
        maximumMillis = maximum,
        confirmLabel = stringResource(Res.string.home_check_in_done),
        cancelLabel = stringResource(Res.string.home_check_in_cancel),
        onConfirm = { onSelected(Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date.toString()) },
        onDismiss = onDismiss,
    )
}

@Preview @Composable private fun HomeCheckInEditorDatePickerPreview() = CoffeePeekTheme {
    HomeCheckInEditorDatePicker("2026-10-17", "2026-10-17", {}, {})
}
