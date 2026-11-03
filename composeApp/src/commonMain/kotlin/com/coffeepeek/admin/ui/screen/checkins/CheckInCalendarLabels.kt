package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.runtime.Composable
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
