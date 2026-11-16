package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekIcon
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.resources.coffeePeekStringArrayResource as stringArrayResource
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.*
import com.coffeepeek.feature.checkinhistory.impl.ui.data.historyCalendarPhotos
import com.coffeepeek.feature.checkinhistory.impl.ui.data.historyMonthTitle
import com.coffeepeek.feature.checkinhistory.impl.ui.data.profileActivityDates
import androidx.compose.ui.tooling.preview.Preview

@Composable
internal fun ProfileActivityCalendarCard(state: ProfileActivityState, expanded: Boolean,
    onToggle: () -> Unit, onAction: (ProfileActivityAction) -> Unit, image: HistoryImageContent) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
            ProfileCalendarHeader(state.calendar, expanded, onToggle, onAction)
            ProfileCalendarWeekdays()
            HorizontalDivider(Modifier.padding(top = 10.dp, bottom = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ProfileCalendarDates(state, expanded, onAction, image)
            if (state.loading) Text(stringResource(Res.string.history_activity_loading),
                Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProfileCalendarHeader(state: CombinedHistoryState, expanded: Boolean,
    onToggle: () -> Unit, onAction: (ProfileActivityAction) -> Unit) {
    val toggleLabel = stringResource(if (expanded) Res.string.history_activity_collapse else Res.string.history_activity_expand)
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (expanded) ProfileMonthArrow(false, state.month != HistoryMonth(1, 1)) { onAction(ProfileActivityAction.ChangeMonth(-1)) }
            Text(historyMonthTitle(state.month), Modifier.weight(1f, fill = false).padding(start = if (expanded) 0.dp else 8.dp),
                style = MaterialTheme.typography.titleMedium)
            if (expanded) ProfileMonthArrow(true, state.canGoNextMonth) { onAction(ProfileActivityAction.ChangeMonth(1)) }
        }
        CoffeePeekIconButton(onToggle, contentDescription = toggleLabel, glass = false,
            modifier = Modifier.semantics { stateDescription = toggleLabel }) {
            CoffeePeekIcon(if (expanded) CoffeePeekIcons.ChevronUp else CoffeePeekIcons.ChevronDown,
                null, Modifier.size(22.dp))
        }
    }
}

@Composable
private fun ProfileMonthArrow(next: Boolean, enabled: Boolean, onClick: () -> Unit) {
    CoffeePeekIconButton(onClick, enabled = enabled, glass = false,
        contentDescription = stringResource(if (next) Res.string.history_next_month else Res.string.history_previous_month)) {
        CoffeePeekIcon(if (next) CoffeePeekIcons.ChevronRight else CoffeePeekIcons.ChevronLeft,
            null, Modifier.size(20.dp))
    }
}

@Composable
private fun ProfileCalendarWeekdays() {
    Row(Modifier.fillMaxWidth()) {
        stringArrayResource(Res.array.history_weekdays).forEach { day ->
            Text(day, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ProfileCalendarDates(state: ProfileActivityState, expanded: Boolean,
    onAction: (ProfileActivityAction) -> Unit, image: HistoryImageContent) {
    profileActivityDates(state.calendar.today, state.calendar.month, expanded).chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            week.forEach { date ->
                if (date == null) Spacer(Modifier.weight(1f).aspectRatio(1f))
                else ProfileCalendarDay(state.calendar, date, expanded, onAction, image, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProfileCalendarDay(state: CombinedHistoryState, date: String, expanded: Boolean,
    onAction: (ProfileActivityAction) -> Unit, image: HistoryImageContent, modifier: Modifier) {
    HistoryCalendarDay(date.takeLast(2).toInt(), date, state.calendarCounts[date] ?: 0L,
        state.homeCalendar.content[date] ?: 0, state.selectedDate == date, state.today == date, date > state.today,
        { onAction(if (expanded) ProfileActivityAction.SelectDate(date) else ProfileActivityAction.OpenWeek()) },
        modifier, historyCalendarPhotos(state, date), image)
}

@Composable
private fun ProfileCalendarCardPreview(dark: Boolean, expanded: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ProfileActivityCalendarCard(ProfileActivityState(CombinedHistoryState("2026-11-17", HistoryMonth(2026, 11), true, 0)),
        expanded, {}, {}, { _, _, _ -> })
}

@Preview @Composable private fun ProfileCalendarWeekLightPreview() = ProfileCalendarCardPreview(false, false)
@Preview @Composable private fun ProfileCalendarWeekDarkPreview() = ProfileCalendarCardPreview(true, false)
@Preview @Composable private fun ProfileCalendarMonthLightPreview() = ProfileCalendarCardPreview(false, true)
@Preview @Composable private fun ProfileCalendarMonthDarkPreview() = ProfileCalendarCardPreview(true, true)
