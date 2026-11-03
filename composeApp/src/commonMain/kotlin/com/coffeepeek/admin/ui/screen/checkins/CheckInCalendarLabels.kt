package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.ui.component.pluralCountLabel
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringArrayResource as stringArrayResource

internal data class CheckInCalendarLabels(
    val months: List<String>,
    val genitiveMonths: List<String>,
    val weekdays: List<String>,
    val checkIns: List<String>,
    val shops: List<String>,
)

@Composable
internal fun checkInCalendarLabels() = CheckInCalendarLabels(
    months = stringArrayResource(Res.array.legacy_calendar_months),
    genitiveMonths = stringArrayResource(Res.array.legacy_calendar_months_genitive),
    weekdays = stringArrayResource(Res.array.legacy_weekdays_full),
    checkIns = stringArrayResource(Res.array.legacy_checkin_forms),
    shops = stringArrayResource(Res.array.legacy_shop_forms),
)

internal fun monthTitle(month: CalendarMonth, labels: CheckInCalendarLabels) = "${labels.months[month.month - 1]} ${month.year}"

internal fun selectedDateTitle(date: String, labels: CheckInCalendarLabels, includeYear: Boolean = true): String {
    val parts = date.split('-')
    val year = parts.getOrNull(0)?.toIntOrNull() ?: return date
    val month = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..12 } ?: return date
    val day = parts.getOrNull(2)?.toIntOrNull() ?: return date
    return "$day ${labels.genitiveMonths[month - 1]}" + if (includeYear) " $year" else ""
}

internal fun visitCount(count: Int, labels: CheckInCalendarLabels) = pluralCountLabel(count, labels.checkIns)
internal fun shopCount(count: Int, labels: CheckInCalendarLabels) = pluralCountLabel(count, labels.shops)
