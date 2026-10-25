package com.coffeepeek.feature.checkinhistory.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.component.*
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.*
import com.coffeepeek.feature.checkinhistory.impl.ui.data.*
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import org.jetbrains.compose.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ProfileActivityCalendarContent(state: ProfileActivityState, onAction: (ProfileActivityAction) -> Unit,
    image: HistoryImageContent, timeLabel: (String) -> String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        HistoryCalendarCard(state.calendar, { action ->
            when (action) {
                is CombinedHistoryAction.ChangeMonth -> onAction(ProfileActivityAction.ChangeMonth(action.offset))
                is CombinedHistoryAction.SelectDate -> action.date?.let { onAction(ProfileActivityAction.SelectDate(it)) }
                CombinedHistoryAction.Today -> onAction(ProfileActivityAction.Today)
                else -> Unit
            }
        }, imageContent = image, framed = false)
        if (state.failed && !state.loading) {
            Text(stringResource(Res.string.history_activity_failed), color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { onAction(ProfileActivityAction.Retry) }) { Text(stringResource(Res.string.history_retry)) }
        }
        if (!state.loading && !state.failed && state.calendar.selectedDate != null) {
            ActivitySelectedDay(state, onAction, image, timeLabel)
        }
    }
}

@Composable
private fun ActivitySelectedDay(state: ProfileActivityState, onAction: (ProfileActivityAction) -> Unit,
    image: HistoryImageContent, timeLabel: (String) -> String) {
    val date = requireNotNull(state.calendar.selectedDate)
    val rows = state.selectedRows
    Column(Modifier.fillMaxWidth().padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(historyDateTitle(date, state.calendar.month.year != HistoryMonth.fromDate(state.calendar.today)?.year),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            val weekdays = stringArrayResource(Res.array.history_weekday_names)
            val weekday = historyCalendarCells(state.calendar.month).indexOf(date.takeLast(2).toInt()) % 7
            Text(stringResource(Res.string.history_activity_day_summary, weekdays[weekday], historyVisitCountLabel(rows.size.toLong())),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (rows.isEmpty()) Text(stringResource(Res.string.history_activity_empty_day),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)) {
            Column {
                rows.forEachIndexed { index, row ->
                    ActivityDiaryRow(row, image, timeLabel) { onAction(ProfileActivityAction.OpenRow(row.key)) }
                    if (index < rows.lastIndex) HorizontalDivider(Modifier.padding(start = 88.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable private fun ActivityCalendarPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ProfileActivityCalendarContent(ProfileActivityState(CombinedHistoryState("2026-10-25", HistoryMonth(2026, 10), true, 0,
        selectedDate = "2026-10-25")), {}, { _, _, _ -> }, { "13:00" })
}
@Preview @Composable private fun ActivityCalendarLightPreview() = ActivityCalendarPreview(false)
@Preview @Composable private fun ActivityCalendarDarkPreview() = ActivityCalendarPreview(true)
