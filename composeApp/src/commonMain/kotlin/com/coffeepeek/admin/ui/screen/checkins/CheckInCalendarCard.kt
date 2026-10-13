package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
) {
    val month = state.calendarMonth
    val total = state.calendarCheckIns.values.sumOf { it.size }
    val shops = state.calendarCheckIns.values.flatten().map(CheckIn::shopId).filter(String::isNotBlank).distinct().size
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                val compactHeader = maxWidth < 332.dp || LocalDensity.current.fontScale > 1.2f
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(monthTitle(month), style = MaterialTheme.typography.titleMedium,
                                fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            if (!compactHeader) CalendarSummary(state, total, shops)
                        }
                        MonthArrow(false, true, onPreviousMonth)
                        MonthArrow(true, state.canGoNextMonth, onNextMonth)
                        if (!compactHeader) TodayButton(onToday)
                    }
                    if (compactHeader) Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { CalendarSummary(state, total, shops) }
                        TodayButton(onToday)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { day ->
                    Text(day, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
            HorizontalDivider(Modifier.padding(top = 8.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            calendarCells(month).chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    week.forEach { day ->
                        if (day == null) Spacer(Modifier.weight(1f))
                        else {
                            val date = month.isoDate(day)
                            CalendarDay(
                                day = day, date = date, checkIns = state.calendarCheckIns[date].orEmpty(),
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
private fun CalendarSummary(state: VisitedPlacesUiState, total: Int, shops: Int) {
    Text(
        when {
            state.isCalendarLoading -> "Загрузка чекинов…"
            state.calendarError != null -> "Не удалось загрузить чекины"
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

@Composable
private fun CalendarDay(
    day: Int,
    date: String,
    checkIns: List<CheckIn>,
    isSelected: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val description = "${selectedDateTitle(date)}${if (isToday) ", сегодня" else ""}, ${visitCount(checkIns.size)}"
    Column(
        modifier.padding(horizontal = 1.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(enabled = !isFuture, role = Role.Button, onClickLabel = "Выбрать дату", onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description; selected = isSelected }
            .padding(top = 3.dp, bottom = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.defaultMinSize(minWidth = 28.dp, minHeight = 28.dp)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
            .clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
            Text(day.toString(), style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isFuture -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                }, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            if (isToday && !isSelected) Box(Modifier.align(Alignment.BottomCenter).size(14.dp, 2.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape))
        }
        Box(Modifier.fillMaxWidth().height(32.dp).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
            if (checkIns.isNotEmpty()) {
                CheckInDiaryThumbnail(checkIns.first(), Modifier.size(32.dp))
                if (checkIns.size > 1) Text(
                    "+${checkIns.size - 1}", fontSize = 10.sp, lineHeight = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.BottomEnd)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .padding(horizontal = 3.dp, vertical = 2.dp),
                )
            }
        }
    }
}
