package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.shop.domain.model.ScheduleInterval
import com.coffeepeek.feature.shop.domain.model.ShopSchedule
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_closed
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_collapse
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_expand
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_friday
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_monday
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_saturday
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_sunday
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_thursday
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_title
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_tuesday
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_unknown
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_wednesday
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Day 0 is Sunday; expanded state and current day are supplied by the screen. */
@Composable
internal fun ShopScheduleSection(
    schedules: List<ShopSchedule>,
    todayDayOfWeek: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (schedules.isEmpty()) return
    val today = schedules.firstOrNull { it.dayOfWeek == todayDayOfWeek }
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(CpDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.shop_schedule_title), style = MaterialTheme.typography.titleMedium)
                    if (!expanded) {
                        Text(scheduleHours(today), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                TextButton(onClick = onToggle) {
                    Text(stringResource(if (expanded) Res.string.shop_schedule_collapse
                        else Res.string.shop_schedule_expand))
                }
            }
            if (expanded) {
                schedules.sortedBy { if (it.dayOfWeek == 0) 7 else it.dayOfWeek }.forEach { schedule ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
                        Text(
                            stringResource(dayName(schedule.dayOfWeek)),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (schedule.dayOfWeek == todayDayOfWeek) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(scheduleHours(schedule), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun scheduleHours(schedule: ShopSchedule?): String = when {
    schedule == null -> stringResource(Res.string.shop_schedule_unknown)
    schedule.isClosed || schedule.intervals.isEmpty() -> stringResource(Res.string.shop_schedule_closed)
    else -> schedule.intervals.joinToString(", ") { "${it.openTime}–${it.closeTime}" }
}

private fun dayName(day: Int) = when (day) {
    1 -> Res.string.shop_schedule_monday
    2 -> Res.string.shop_schedule_tuesday
    3 -> Res.string.shop_schedule_wednesday
    4 -> Res.string.shop_schedule_thursday
    5 -> Res.string.shop_schedule_friday
    6 -> Res.string.shop_schedule_saturday
    else -> Res.string.shop_schedule_sunday
}

@Preview @Composable private fun ShopScheduleSectionLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopScheduleSection(previewSchedules(), todayDayOfWeek = 1, expanded = false, onToggle = {})
}

@Preview @Composable private fun ShopScheduleSectionDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopScheduleSection(previewSchedules(), todayDayOfWeek = 1, expanded = true, onToggle = {})
}

private fun previewSchedules() = listOf(
    ShopSchedule(1, false, listOf(ScheduleInterval("09:00", "21:00"))),
    ShopSchedule(2, false, listOf(ScheduleInterval("09:00", "21:00"))),
    ShopSchedule(3, true),
    ShopSchedule(4, false, listOf(ScheduleInterval("09:00", "21:00"))),
    ShopSchedule(5, false, listOf(ScheduleInterval("09:00", "22:00"))),
    ShopSchedule(6, false, listOf(ScheduleInterval("10:00", "22:00"))),
    ShopSchedule(0, false, listOf(ScheduleInterval("10:00", "20:00"))),
)
