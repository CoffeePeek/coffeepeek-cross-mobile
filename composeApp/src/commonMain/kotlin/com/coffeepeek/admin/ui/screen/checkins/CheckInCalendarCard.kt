package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton as IconButton
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIcon
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.calendar_next_month
import coffeepeek.composeapp.generated.resources.calendar_previous_month
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

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
    val description = stringResource(if (next) Res.string.calendar_next_month else Res.string.calendar_previous_month)
    IconButton(onClick = { platformHaptic(HapticEvent.Selection); onClick() }, enabled = enabled,
        modifier = Modifier.size(44.dp), contentDescription = description,
        nativeIcon = if (next) CoffeePeekGlassIcon.Forward else CoffeePeekGlassIcon.Back) {
        Box(Modifier.size(34.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center) {
            Icon(if (next) CpIcons.ChevronRight else CpIcons.ChevronLeft,
                contentDescription = null,
                modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CalendarNavigationPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface {
        Row(Modifier.padding(16.dp)) {
            MonthArrow(false, true, {})
            MonthArrow(true, false, {})
        }
    }
}

@Preview @Composable private fun CalendarNavigationLightPreview() = CalendarNavigationPreview(false)
@Preview @Composable private fun CalendarNavigationDarkPreview() = CalendarNavigationPreview(true)

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
    homeCount: Int,
    photos: List<CalendarPhoto>,
    isSelected: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val total = checkIns.size + homeCount
    val description = "${selectedDateTitle(date)}${if (isToday) ", сегодня" else ""}, ${visitCount(total)}" +
        if (homeCount > 0) ", $homeCount дома" else ""
    val photo = photos.firstOrNull()
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier.aspectRatio(1f).padding(1.dp)
            .clip(shape)
            .background(when {
                isSelected && photo == null -> MaterialTheme.colorScheme.primary
                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                else -> Color.Transparent
            })
            .then(if ((isToday && !isSelected) || (isSelected && photo != null)) {
                Modifier.border(1.dp, MaterialTheme.colorScheme.primary, shape)
            } else Modifier)
            .clickable(enabled = !isFuture, role = Role.Button, onClickLabel = "Выбрать дату", onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description; selected = isSelected },
        contentAlignment = Alignment.Center,
    ) {
        if (photo == null) {
            Text(day.toString(), style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    else -> MaterialTheme.colorScheme.onSurface
                }, modifier = Modifier.padding(4.dp))
        } else {
            Column(
                Modifier.fillMaxSize().padding(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    val imageModifier = Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true).clearAndSetSemantics {}
                    CpImage(data = photo.url, contentScale = ContentScale.Crop,
                        modifier = imageModifier.clip(RoundedCornerShape(6.dp)))
                }
                if (photos.size > 1) {
                    Text("+${photos.size - 1}", style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp, lineHeight = 12.sp,
                        color = MaterialTheme.colorScheme.primary, maxLines = 1)
                }
            }
        }
    }
}
