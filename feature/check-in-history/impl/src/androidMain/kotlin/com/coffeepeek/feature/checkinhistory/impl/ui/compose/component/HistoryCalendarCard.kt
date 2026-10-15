package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistoryCalendarState
import com.coffeepeek.feature.checkinhistory.impl.ui.data.historyCalendarCells
import com.coffeepeek.feature.checkinhistory.impl.ui.data.historyDayStreak
import com.coffeepeek.feature.checkinhistory.impl.ui.data.historyMonthTitle
import com.coffeepeek.feature.checkinhistory.impl.ui.data.historyVisitCountLabel
import com.coffeepeek.feature.checkinhistory.impl.ui.data.historyShopCountLabel
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HistoryCalendarCard(state: HistoryCalendarState, today: String, canGoNext: Boolean,
    onChangeMonth: (Int) -> Unit, onSelectDate: (String) -> Unit, modifier: Modifier = Modifier,
    imageContent: HistoryImageContent? = null) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(CoffeePeekDimens.radius2xl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(horizontal = CoffeePeekDimens.spacing2, vertical = CoffeePeekDimens.spacing3),
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onChangeMonth(-1) }, enabled = state.month != HistoryMonth(1, 1)) {
                    Icon(CoffeePeekIcons.ChevronLeft, stringResource(Res.string.history_previous_month))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(historyMonthTitle(state.month), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val visits = historyVisitCountLabel(state.visitCount)
                    val shops = historyShopCountLabel(state.shopCount)
                    Text(stringResource(Res.string.history_month_counts, visits, shops), style = MaterialTheme.typography.labelMedium)
                }
                IconButton(onClick = { onChangeMonth(1) }, enabled = canGoNext) {
                    Icon(CoffeePeekIcons.ChevronRight, stringResource(Res.string.history_next_month))
                }
            }
            Row(Modifier.fillMaxWidth()) {
                stringArrayResource(Res.array.history_weekdays).forEach { label ->
                    Text(label, Modifier.weight(1f), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val dates = state.checkInsByDate.filterValues { it.isNotEmpty() }.keys
            historyCalendarCells(state.month).chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEachIndexed { weekday, day ->
                        if (day == null) Spacer(Modifier.weight(1f).height(52.dp))
                        else {
                            val date = state.month.date(day)
                            val streak = historyDayStreak(state.month, day, dates)
                            HistoryCalendarDay(day, date, state.checkInsByDate[date].orEmpty(), state.selectedDate == date,
                                today == date, streak.previous && weekday > 0, streak.next && weekday < 6,
                                streak.isConnected, { onSelectDate(date) }, Modifier.weight(1f), imageContent)
                        }
                    }
                }
            }
        }
    }
}

@PreviewLightDark @Composable private fun HistoryCalendarCardPreview() = CoffeePeekTheme {
    val checkIn = previewHistoryCheckIn()
    HistoryCalendarCard(HistoryCalendarState(HistoryMonth(2026, 10),
        mapOf("2026-10-13" to listOf(checkIn), "2026-10-14" to listOf(checkIn)), "2026-10-14", true),
        "2026-10-14", false, {}, {})
}
