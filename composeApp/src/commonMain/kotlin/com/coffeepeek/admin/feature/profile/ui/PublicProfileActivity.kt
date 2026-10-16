package com.coffeepeek.admin.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.feature.profile.domain.PublicProfileActivity
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.icons.CpIcons

private val weekdayLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
private val monthLabels = listOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")

@Composable
internal fun PublicProfileActivityCard(activity: PublicProfileActivity, onClick: () -> Unit) {
    val month = profileMonth(activity.month)
    val activeDays = activity.activeDays.filter { it in 1..month.days }
    ProfileCard(modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Открыть активность", onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(profileMonthTitle(activity.month), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(activeDaysLabel(activeDays.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(CpIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Row(Modifier.fillMaxWidth()) {
                weekdayLabels.forEachIndexed { index, label ->
                    val count = activeDays.count { day -> (month.firstWeekday + day - 1) % 7 == index }
                    Column(
                        Modifier.weight(1f).clearAndSetSemantics { contentDescription = "$label: ${activeDaysLabel(count)}" },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(Modifier.size(8.dp).background(
                            if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            CircleShape,
                        ))
                    }
                }
            }
        }
    }
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
    ProfileCard {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(profileMonthTitle(selectedMonth), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (onMonthChange != null) {
                    val previous = shiftProfileMonth(selectedMonth, -1)
                    val next = shiftProfileMonth(selectedMonth, 1)
                    IconButton(onClick = { previous?.let(onMonthChange) }, enabled = previous != null) {
                        Icon(CpIcons.ChevronLeft, contentDescription = "Предыдущий месяц", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { next?.let(onMonthChange) }, enabled = next != null) {
                        Icon(CpIcons.ChevronRight, contentDescription = "Следующий месяц", modifier = Modifier.size(20.dp))
                    }
                }
            }
            when {
                isLoading -> Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { CoffeePeekLoader() }
                error != null || displayedActivity == null -> Column(
                    Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Не удалось загрузить активность", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onRetry) { Text("Повторить") }
                }
                else -> ProfileActivityMonthGrid(displayedActivity)
            }
        }
    }
}

@Composable
private fun ProfileActivityMonthGrid(activity: PublicProfileActivity) {
    val month = profileMonth(activity.month)
    val activeDays = activity.activeDays.filter { it in 1..month.days }.toSet()
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(activeDaysLabel(activeDays.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val cells: List<Int?> = List(month.firstWeekday) { null } + (1..month.days).map { it }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                (0..6).forEach { column ->
                    val day = week.getOrNull(column)
                    if (day == null) Spacer(Modifier.weight(1f).height(42.dp))
                    else {
                        val active = day in activeDays
                        Column(
                            Modifier.weight(1f).height(42.dp).padding(horizontal = 3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
                                .clearAndSetSemantics { contentDescription = "$day, ${if (active) "есть чекины" else "нет чекинов"}" },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(day.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                            Spacer(Modifier.height(4.dp))
                            Box(Modifier.size(4.dp).background(
                                if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), CircleShape,
                            ))
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            Text("Дни с чекинами", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (activity.timeZone == "Europe/Minsk") {
            Text("По времени Минска", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class ProfileMonth(val year: Int, val number: Int, val days: Int, val firstWeekday: Int)

private fun profileMonth(value: String): ProfileMonth {
    val parts = value.split('-')
    val year = parts.firstOrNull()?.toIntOrNull()?.coerceIn(1, 9999) ?: 2000
    val month = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 12) ?: 1
    val leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    val days = when (month) { 2 -> if (leap) 29 else 28; 4, 6, 9, 11 -> 30; else -> 31 }
    // Gregorian weekday, shifted from Sunday-first to the Monday-first UI.
    val offsets = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    val adjustedYear = if (month < 3) year - 1 else year
    val sundayFirst = (adjustedYear + adjustedYear / 4 - adjustedYear / 100 + adjustedYear / 400 + offsets[month - 1] + 1) % 7
    return ProfileMonth(year, month, days, (sundayFirst + 6) % 7)
}

private fun profileMonthTitle(value: String): String {
    val month = profileMonth(value)
    return "${monthLabels[month.number - 1]} ${month.year}"
}

private fun shiftProfileMonth(value: String, offset: Int): String? {
    val month = profileMonth(value)
    val shifted = month.year * 12 + month.number - 1 + offset
    val year = shifted / 12
    val number = shifted % 12 + 1
    if (year !in 2000..2100) return null
    return "$year-${number.toString().padStart(2, '0')}"
}

private fun activeDaysLabel(count: Int): String {
    val word = when { count % 100 in 11..14 -> "дней"; count % 10 == 1 -> "день"; count % 10 in 2..4 -> "дня"; else -> "дней" }
    return "$count $word с чекинами"
}
