package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.coffeepeek.feature.shop.api.ShopExposure
import com.coffeepeek.feature.shop.impl.ui.data.shopExposure
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
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
import com.coffeepeek.feature.shop.impl.resources.shop_overview_closed
import com.coffeepeek.feature.shop.impl.resources.shop_overview_open
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_open_until
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_opens_today
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_opens_tomorrow
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_opens_day
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_today_closed
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_expanded_state
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_collapsed_state
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_monday_short
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_tuesday_short
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_wednesday_short
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_thursday_short
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_friday_short
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_saturday_short
import com.coffeepeek.feature.shop.impl.resources.shop_schedule_sunday_short
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopScheduleStatus
import com.coffeepeek.feature.shop.impl.ui.data.shopScheduleStatus
import com.coffeepeek.feature.shop.impl.ui.data.formatShopScheduleTime
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
    isOpen: Boolean? = null,
    minuteOfDay: Int? = null,
) {
    if (schedules.isEmpty()) return
    val status = scheduleStatusText(shopScheduleStatus(schedules, isOpen, todayDayOfWeek, minuteOfDay))
    Column(modifier.shopExposure(ShopExposure.Hours).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        ShopSectionTitle(stringResource(Res.string.shop_schedule_title))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(CoffeePeekDimens.radius2xl),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            Column(Modifier.padding(CoffeePeekDimens.spacing4)) {
                ScheduleHeader(status, expanded, onToggle)
                AnimatedVisibility(expanded,
                    enter = expandVertically(tween(250), expandFrom = Alignment.Top) + fadeIn(tween(180)),
                    exit = shrinkVertically(tween(250), shrinkTowards = Alignment.Top) + fadeOut(tween(150))) {
                    ScheduleDays(schedules)
                }
            }
        }
    }
}

@Composable
private fun ScheduleHeader(status: String, expanded: Boolean, onToggle: () -> Unit) {
    val toggleLabel = stringResource(if (expanded) Res.string.shop_schedule_collapse else Res.string.shop_schedule_expand)
    val expansionState = stringResource(if (expanded) Res.string.shop_schedule_expanded_state
        else Res.string.shop_schedule_collapsed_state)
    val rotation by animateFloatAsState(if (expanded) 90f else 0f, tween(250))
    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp)
        .semantics { stateDescription = expansionState }
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
            role = Role.Button, onClickLabel = toggleLabel, onClick = onToggle),
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(CoffeePeekIcons.Time, null, Modifier.size(24.dp))
        Text(status, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Icon(CoffeePeekIcons.ChevronRight, null, Modifier.size(18.dp).rotate(rotation),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ScheduleDays(schedules: List<ShopSchedule>) {
    Column {
        HorizontalDivider(Modifier.padding(vertical = CoffeePeekDimens.spacing2),
            color = MaterialTheme.colorScheme.outline)
        Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
            schedules.sortedBy { if (it.dayOfWeek == 0) 7 else it.dayOfWeek }.forEach { schedule ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
                    Text(stringResource(dayName(schedule.dayOfWeek)), Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(scheduleHours(schedule), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun scheduleHours(schedule: ShopSchedule?): String = when {
    schedule == null -> stringResource(Res.string.shop_schedule_unknown)
    schedule.isClosed -> stringResource(Res.string.shop_schedule_closed)
    schedule.intervals.isEmpty() -> stringResource(Res.string.shop_schedule_unknown)
    else -> schedule.intervals.joinToString("\n") {
        "${formatShopScheduleTime(it.openTime)} – ${formatShopScheduleTime(it.closeTime)}"
    }
}

@Composable
private fun scheduleStatusText(status: ShopScheduleStatus): String = when (status) {
    is ShopScheduleStatus.Open -> status.closingTime?.let { stringResource(Res.string.shop_schedule_open_until, it) }
        ?: stringResource(Res.string.shop_overview_open)
    is ShopScheduleStatus.Opens -> when (status.dayOffset) {
        0 -> stringResource(Res.string.shop_schedule_opens_today, status.time)
        1 -> stringResource(Res.string.shop_schedule_opens_tomorrow, status.time)
        else -> stringResource(Res.string.shop_schedule_opens_day, stringResource(shortDayName(status.dayOfWeek)), status.time)
    }
    ShopScheduleStatus.TodayClosed -> stringResource(Res.string.shop_schedule_today_closed)
    ShopScheduleStatus.Closed -> stringResource(Res.string.shop_overview_closed)
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

private fun shortDayName(day: Int) = when (day) {
    1 -> Res.string.shop_schedule_monday_short
    2 -> Res.string.shop_schedule_tuesday_short
    3 -> Res.string.shop_schedule_wednesday_short
    4 -> Res.string.shop_schedule_thursday_short
    5 -> Res.string.shop_schedule_friday_short
    6 -> Res.string.shop_schedule_saturday_short
    else -> Res.string.shop_schedule_sunday_short
}

@Preview @Composable private fun ShopScheduleSectionLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopScheduleSection(previewSchedules(), todayDayOfWeek = 1, expanded = false, onToggle = {}, isOpen = true, minuteOfDay = 720)
}

@Preview @Composable private fun ShopScheduleSectionDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopScheduleSection(previewSchedules(), todayDayOfWeek = 1, expanded = true, onToggle = {}, isOpen = false, minuteOfDay = 480)
}

@Preview @Composable private fun ShopScheduleNextOpeningLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopScheduleSection(previewSchedules(), 2, false, {}, isOpen = false, minuteOfDay = 22 * 60)
}
@Preview @Composable private fun ShopScheduleNextOpeningDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopScheduleSection(previewSchedules(), 2, false, {}, isOpen = false, minuteOfDay = 22 * 60)
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
