package com.coffeepeek.feature.profile.impl.ui.compose.component

import androidx.compose.foundation.background
import com.coffeepeek.feature.profile.impl.resources.Res
import com.coffeepeek.feature.profile.impl.resources.profile_days_with_check_ins
import com.coffeepeek.feature.profile.impl.resources.profile_months
import com.coffeepeek.feature.profile.impl.resources.profile_weekdays
import com.coffeepeek.feature.profile.impl.resources.profile_activity_failed
import com.coffeepeek.feature.profile.impl.resources.profile_activity_legend
import com.coffeepeek.feature.profile.impl.resources.profile_activity_minsk_time
import com.coffeepeek.feature.profile.impl.resources.profile_day_active
import com.coffeepeek.feature.profile.impl.resources.profile_day_inactive
import com.coffeepeek.feature.profile.impl.resources.profile_month_title
import com.coffeepeek.feature.profile.impl.resources.profile_next_month
import com.coffeepeek.feature.profile.impl.resources.profile_previous_month
import com.coffeepeek.feature.profile.impl.resources.profile_retry
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.feature.profile.domain.PublicProfileActivity
import com.coffeepeek.feature.profile.impl.ui.data.profileMonth
import com.coffeepeek.feature.profile.impl.ui.data.shiftProfileMonth

@Composable
private fun daysLabel(count: Int): String = pluralStringResource(Res.plurals.profile_days_with_check_ins, count, count)

@Composable
private fun monthTitle(value: String): String {
    val month = profileMonth(value)
    return stringResource(Res.string.profile_month_title, stringArrayResource(Res.array.profile_months)[month.number - 1], month.year)
}

@Composable
internal fun PublicProfileActivityCalendar(
    activity: PublicProfileActivity?,
    selectedMonth: String = activity?.month.orEmpty(),
    isLoading: Boolean = false,
    error: String? = null,
    onMonthChange: ((String) -> Unit)? = null,
    onRetry: () -> Unit = {},
) {
    val displayedActivity = activity?.takeIf { it.month == selectedMonth }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(monthTitle(selectedMonth), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (onMonthChange != null) {
                val previous = shiftProfileMonth(selectedMonth, -1)
                val next = shiftProfileMonth(selectedMonth, 1)
                IconButton(onClick = { previous?.let(onMonthChange) }, enabled = previous != null) {
                    Icon(CoffeePeekIcons.ChevronLeft, contentDescription = stringResource(Res.string.profile_previous_month))
                }
                IconButton(onClick = { next?.let(onMonthChange) }, enabled = next != null) {
                    Icon(CoffeePeekIcons.ChevronRight, contentDescription = stringResource(Res.string.profile_next_month))
                }
            }
        }
        when {
            isLoading -> ProfileSectionLoader(180.dp)
            error != null || displayedActivity == null -> Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(Res.string.profile_activity_failed), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onRetry) { Text(stringResource(Res.string.profile_retry)) }
            }
            else -> ProfileActivityMonthGrid(displayedActivity)
        }
    }
}

@Composable
private fun ProfileActivityMonthGrid(activity: PublicProfileActivity) {
    val month = profileMonth(activity.month)
    val activeDays = activity.activeDays.filter { it in 1..month.days }.toSet()
    val weekdays = stringArrayResource(Res.array.profile_weekdays)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(daysLabel(activeDays.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            weekdays.forEach { label ->
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val cells: List<Int?> = List(month.firstWeekday) { null } + (1..month.days).map { it }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0..6).forEach { column ->
                    val day = week.getOrNull(column)
                    if (day == null) Spacer(Modifier.weight(1f).aspectRatio(1f))
                    else {
                        val active = day in activeDays
                        val description = stringResource(if (active) Res.string.profile_day_active else Res.string.profile_day_inactive, day)
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(10.dp))
                                .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
                                .clearAndSetSemantics { contentDescription = description },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(day.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            Text(stringResource(Res.string.profile_activity_legend), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (activity.timeZone == "Europe/Minsk") {
            Text(stringResource(Res.string.profile_activity_minsk_time), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
