package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.ui.component.HapticEvent
import com.coffeepeek.admin.ui.component.platformHaptic
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.domain.model.CheckIn

@Composable
internal fun CalendarCard(
    state: VisitedPlacesUiState,
    today: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (String) -> Unit,
    onToday: () -> Unit,
    homeCounts: Map<String, Int> = emptyMap(),
    homePhotos: Map<String, List<CalendarPhoto>> = emptyMap(),
    framed: Boolean = true,
) {
    val month = state.calendarMonth
    val homeTotal = homeCounts.values.sum()
    val total = state.calendarCheckIns.values.sumOf { it.size } + homeTotal
    val shops = state.calendarCheckIns.values.flatten().map(CheckIn::shopId).filter(String::isNotBlank).distinct().size
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (framed) 20.dp else 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (framed) MaterialTheme.colorScheme.surface else Color.Transparent,
        ),
        border = if (framed) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)) else null,
    ) {
        Column(Modifier.padding(if (framed) 12.dp else 0.dp)) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(monthTitle(month), modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                MonthArrow(false, true, onPreviousMonth)
                MonthArrow(true, state.canGoNextMonth, onNextMonth)
            }
            Row(Modifier.fillMaxWidth().padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { CalendarSummary(state, total, shops, homeTotal) }
                TodayButton(onToday)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { day ->
                    Text(day, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
            HorizontalDivider(Modifier.padding(top = 10.dp, bottom = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            calendarCells(month).chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    week.forEach { day ->
                        if (day == null) Spacer(Modifier.weight(1f).aspectRatio(1f))
                        else {
                            val date = month.isoDate(day)
                            CalendarDay(
                                day = day, date = date, checkIns = state.calendarCheckIns[date].orEmpty(),
                                homeCount = homeCounts[date] ?: 0,
                                photos = calendarPhotos(state.calendarCheckIns[date].orEmpty(), homePhotos[date].orEmpty()),
                                isSelected = state.selectedDate == date, isToday = today == date,
                                isFuture = date > today, modifier = Modifier.weight(1f),
                                onClick = { platformHaptic(HapticEvent.Selection); onSelectDate(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarSummary(state: VisitedPlacesUiState, total: Int, shops: Int, homeTotal: Int) {
    Text(
        when {
            state.isCalendarLoading -> "Загрузка чекинов…"
            state.calendarError != null -> "Не удалось загрузить чекины"
            homeTotal > 0 -> "${visitCount(total)} · $homeTotal дома"
            else -> "${visitCount(total)} · ${shopCount(shops)}"
        },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MonthArrow(next: Boolean, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = { platformHaptic(HapticEvent.Selection); onClick() }, enabled = enabled,
        modifier = Modifier.size(44.dp)) {
        Box(Modifier.size(34.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center) {
            Icon(if (next) CpIcons.ChevronRight else CpIcons.ChevronLeft,
                contentDescription = if (next) "Следующий месяц" else "Предыдущий месяц",
                modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun TodayButton(onClick: () -> Unit) {
    TextButton(onClick = { platformHaptic(HapticEvent.Selection); onClick() },
        contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.heightIn(min = 44.dp)
            .semantics { contentDescription = "Сегодня, показать чекины за текущую дату" }) {
        Text("Сегодня", style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}
