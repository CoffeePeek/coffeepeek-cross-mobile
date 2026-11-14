package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import com.coffeepeek.core.designsystem.component.CoffeePeekDatePickerDialog
import androidx.compose.runtime.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import com.coffeepeek.feature.homecheckin.impl.resources.*

/** Picker millis represent a civil day at UTC midnight, not the eventual preparation instant. */
@Composable
internal fun HomeCheckInEditorDatePicker(date: String, today: String, onDismiss: () -> Unit, onSelected: (String) -> Unit) {
    val maximum = remember(today) { (checkNotNull(isoEpochDay(today)) + 1) * DAY_MILLIS - 1 }
    val selected = remember(date) { isoEpochDay(date)?.let { it * DAY_MILLIS } }
    CoffeePeekDatePickerDialog(
        title = stringResource(Res.string.home_check_in_choose_date),
        initialMillis = selected,
        maximumMillis = maximum,
        confirmLabel = stringResource(Res.string.home_check_in_done),
        cancelLabel = stringResource(Res.string.home_check_in_cancel),
        onConfirm = { onSelected(isoDate(it.floorDiv(DAY_MILLIS))) },
        onDismiss = onDismiss,
    )
}

private const val DAY_MILLIS = 86_400_000L

/** Days since 1970-01-01 of an ISO `yyyy-MM-dd` date, or null if it is not one. */
@OptIn(ExperimentalTime::class)
internal fun isoEpochDay(value: String): Long? =
    runCatching { Instant.parse("${value}T00:00:00Z") }.getOrNull()?.toEpochMilliseconds()?.floorDiv(DAY_MILLIS)

/** The ISO `yyyy-MM-dd` date [epochDay] days after 1970-01-01. */
@OptIn(ExperimentalTime::class)
internal fun isoDate(epochDay: Long): String = Instant.fromEpochMilliseconds(epochDay * DAY_MILLIS).toString().substringBefore('T')

@Composable private fun HomeCheckInEditorDatePickerPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    HomeCheckInEditorDatePicker("2026-10-17", "2026-10-17", {}, {})
}
@Preview @Composable private fun HomeCheckInEditorDatePickerLightPreview() = HomeCheckInEditorDatePickerPreview(false)
@Preview @Composable private fun HomeCheckInEditorDatePickerDarkPreview() = HomeCheckInEditorDatePickerPreview(true)
