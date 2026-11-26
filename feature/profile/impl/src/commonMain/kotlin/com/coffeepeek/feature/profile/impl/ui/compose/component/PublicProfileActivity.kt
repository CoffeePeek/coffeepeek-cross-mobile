package com.coffeepeek.feature.profile.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton
import com.coffeepeek.core.designsystem.resources.coffeePeekStringArrayResource as stringArrayResource
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.api.ProfileCalendarHeaderContent
import com.coffeepeek.feature.profile.api.ProfileImage
import com.coffeepeek.feature.profile.domain.PublicProfileActivity
import com.coffeepeek.feature.profile.domain.PublicProfileCheckIn
import com.coffeepeek.feature.profile.impl.resources.*
import com.coffeepeek.feature.profile.impl.ui.data.PublicProfileWeekDay
import com.coffeepeek.feature.profile.impl.ui.data.currentPublicProfileDate
import com.coffeepeek.feature.profile.impl.ui.data.profileMonth
import com.coffeepeek.feature.profile.impl.ui.data.publicProfileWeek
import com.coffeepeek.feature.profile.impl.ui.data.publicProfileWeekDates
import com.coffeepeek.feature.profile.impl.ui.data.shiftProfileMonth
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileError

@Composable
internal fun PublicProfileActivityCalendar(
    activity: PublicProfileActivity?, selectedMonth: String, isLoading: Boolean, error: PublicProfileError?,
    checkIns: List<PublicProfileCheckIn>, image: ProfileImage,
    onMonthChange: (String) -> Unit, onRetry: () -> Unit, selectedDate: String?, onDateSelect: (String) -> Unit,
    expanded: Boolean, header: ProfileCalendarHeaderContent, profileKey: String? = null, today: String = currentPublicProfileDate(),
) {
    val weekDates = remember(today) { publicProfileWeekDates(today) }
    val weekActivity = rememberWeekActivity(profileKey, weekDates, activity)
    val shownMonth = if (expanded) selectedMonth else today.take(7)
    val displayed = activity?.takeIf { it.month == selectedMonth }
    val month = remember(shownMonth) { profileMonth(shownMonth) }
    val days = remember(expanded, shownMonth, weekDates, weekActivity, displayed, checkIns) {
        val dates = if (expanded) (1..month.days).map { day -> "$shownMonth-${day.toString().padStart(2, '0')}" }
            else weekDates
        val source = if (expanded) listOfNotNull(displayed).associateBy { it.month } else weekActivity
        publicProfileWeek(dates, source, checkIns)
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DisplayProfileMonth(shownMonth, expanded, onMonthChange, header)
        ProfileMonthGrid(days, if (expanded) month.firstWeekday else 0, selectedDate, onDateSelect, image,
            lastSelectableDate = if (expanded) null else today)
        if (expanded && error != null && !isLoading) {
            CoffeePeekTextButton(onClick = onRetry) { Text(stringResource(Res.string.profile_retry)) }
        }
    }
}

@Composable
private fun rememberWeekActivity(profileKey: String?, dates: List<String>, activity: PublicProfileActivity?): Map<String, PublicProfileActivity> {
    var loaded by remember(profileKey) { mutableStateOf(emptyMap<String, PublicProfileActivity>()) }
    val months = remember(dates) { dates.map { it.take(7) }.toSet() }
    val visible = activity?.takeIf { it.month in months }
    LaunchedEffect(profileKey, months, visible) {
        val current = loaded.filterKeys { it in months }
        loaded = if (visible == null) current else current + (visible.month to visible)
    }
    return if (visible == null) loaded else loaded + (visible.month to visible)
}

@Composable
private fun DisplayProfileMonth(monthValue: String, expanded: Boolean, onMonthChange: (String) -> Unit,
    header: ProfileCalendarHeaderContent) {
    val month = profileMonth(monthValue)
    val months = stringArrayResource(Res.array.profile_months)
    val previous = if (expanded) shiftProfileMonth(monthValue, -1)?.let { date -> { onMonthChange(date) } } else null
    val next = if (expanded) shiftProfileMonth(monthValue, 1)?.let { date -> { onMonthChange(date) } } else null
    header(stringResource(Res.string.profile_month_title, months[month.number - 1], month.year), previous, next)
}

@Composable
private fun ProfileMonthGrid(
    days: List<PublicProfileWeekDay>, firstWeekday: Int, selectedDate: String?, onDateSelect: (String) -> Unit, image: ProfileImage,
    lastSelectableDate: String? = null,
) {
    Row(Modifier.fillMaxWidth()) {
        stringArrayResource(Res.array.profile_weekdays).forEach { weekday ->
            Text(weekday, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    val cells = List(firstWeekday) { null } + days
    cells.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            repeat(7) { index ->
                val day = week.getOrNull(index)
                if (day == null) Spacer(Modifier.weight(1f).aspectRatio(1f))
                else ProfileMonthDay(day, day.date == selectedDate, { onDateSelect(day.date) }, image, Modifier.weight(1f),
                    enabled = lastSelectableDate == null || day.date <= lastSelectableDate, today = day.date == lastSelectableDate)
            }
        }
    }
}

@Composable
private fun ProfileMonthDay(day: PublicProfileWeekDay, selected: Boolean, onSelect: () -> Unit, image: ProfileImage, modifier: Modifier,
    enabled: Boolean, today: Boolean) {
    val number = day.date.takeLast(2).toInt()
    val photo = day.photos.firstOrNull()
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)
    val description = if (day.known) stringResource(
        if (day.count > 0) Res.string.profile_day_active else Res.string.profile_day_inactive, number,
    ) else day.date
    Box(modifier.aspectRatio(1f).padding(2.dp).clip(shape)
        .then(if (today) Modifier.background(colors.primaryContainer) else Modifier)
        .then(if (selected) Modifier.border(2.dp, colors.primary, shape) else Modifier)
        .selectable(selected, enabled = enabled, role = Role.Button, onClick = onSelect)
        .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center) {
        if (photo == null) {
            Text(number.toString(), style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface.copy(alpha = if (enabled) 1f else 0.45f), modifier = Modifier.clearAndSetSemantics {})
            if (day.count > 0) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp)
                .size(4.dp).background(colors.primary, CircleShape))
        } else {
            image(photo, null, Modifier.fillMaxSize().clearAndSetSemantics {})
            Text(number.toString(), style = MaterialTheme.typography.labelSmall, color = colors.onSurface,
                modifier = Modifier.align(Alignment.BottomEnd).background(colors.surface)
                    .padding(horizontal = 3.dp).clearAndSetSemantics {})
        }
    }
}

@Composable
private fun PublicProfileMonthPreview(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    val checkIn = PublicProfileCheckIn("preview", null, "", "", null, null, "", "2026-11-16", listOf("preview"))
    PublicProfileActivityCalendar(PublicProfileActivity("2026-11", setOf(16, 18)), "2026-11", false, null,
        listOf(checkIn), { _, _, modifier -> Box(modifier.background(MaterialTheme.colorScheme.tertiaryContainer)) },
        {}, {}, "2026-11-16", {}, true, { month, _, _ -> Text(month) }, today = "2026-11-16")
}

@Preview @Composable private fun PublicProfileMonthLightPreview() = PublicProfileMonthPreview(false)
@Preview @Composable private fun PublicProfileMonthDarkPreview() = PublicProfileMonthPreview(true)
