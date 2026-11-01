package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton as IconButton
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIcon
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.*
import com.coffeepeek.feature.checkinhistory.impl.ui.data.*
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HistoryCalendarCard(state: CombinedHistoryState, onAction: (CombinedHistoryAction) -> Unit,
    modifier: Modifier = Modifier, imageContent: HistoryImageContent? = null) {
    val haptics = LocalHapticFeedback.current
    fun select(action: CombinedHistoryAction) {
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        onAction(action)
    }
    val homes = if (state.filter.includes(HistorySource.Home)) state.homeCalendar.content.values.sumOf { it.toLong() } else 0L
    val shops = if (state.filter.includes(HistorySource.Shop)) state.shopCalendar.content.values.flatten()
        .mapNotNull { it.entry.shop?.slug?.takeIf(String::isNotBlank) }.distinct().size else 0
    val counts = state.calendarCounts
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(historyMonthTitle(state.month), Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                MonthArrow(false, state.month != HistoryMonth(1, 1)) { select(CombinedHistoryAction.ChangeMonth(-1)) }
                MonthArrow(true, state.canGoNextMonth) { select(CombinedHistoryAction.ChangeMonth(1)) }
            }
            Row(Modifier.fillMaxWidth().padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                val loading = state.filter.includes(HistorySource.Shop) && state.shopCalendar.isLoading ||
                    state.filter.includes(HistorySource.Home) && state.homeCalendar.isLoading
                Text(if (loading) stringResource(Res.string.history_loading) else
                    stringResource(Res.string.history_month_counts, historyVisitCountLabel(counts.values.sum()),
                        if (homes > 0) stringResource(Res.string.history_home_count, homes) else historyShopCountLabel(shops)),
                    Modifier.weight(1f), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                val todayLabel = stringResource(Res.string.history_today_description)
                TextButton(onClick = { select(CombinedHistoryAction.Today) }, contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.heightIn(min = 44.dp).semantics { contentDescription = todayLabel }) {
                    Text(stringResource(Res.string.history_today), style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                stringArrayResource(Res.array.history_weekdays).forEach { label ->
                    Text(label, Modifier.weight(1f), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(Modifier.padding(top = 10.dp, bottom = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            historyCalendarCells(state.month).chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    week.forEach { day ->
                        if (day == null) Spacer(Modifier.weight(1f).aspectRatio(1f))
                        else {
                            val date = state.month.date(day)
                            HistoryCalendarDay(day, date, counts[date] ?: 0L,
                                if (state.filter.includes(HistorySource.Home)) state.homeCalendar.content[date] ?: 0 else 0,
                                state.selectedDate == date, state.today == date, date > state.today,
                                { select(CombinedHistoryAction.SelectDate(date)) }, Modifier.weight(1f),
                                historyCalendarPhotos(state, date), imageContent)
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun MonthArrow(next: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val description = stringResource(if (next) Res.string.history_next_month else Res.string.history_previous_month)
    IconButton(onClick, enabled = enabled, modifier = Modifier.size(44.dp), contentDescription = description,
        nativeIcon = if (next) CoffeePeekGlassIcon.Forward else CoffeePeekGlassIcon.Back) {
        Box(Modifier.size(34.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center) {
            Icon(if (next) CoffeePeekIcons.ChevronRight else CoffeePeekIcons.ChevronLeft,
                null, Modifier.size(18.dp))
        }
    }
}

@Composable private fun HistoryCalendarCardPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    HistoryCalendarCard(CombinedHistoryState("2026-10-14", HistoryMonth(2026, 10), true, 0,
        selectedDate = "2026-10-14", homeCalendar = CombinedHistoryLoadState(mapOf("2026-10-14" to 2), hasLoaded = true)), {})
}
@Preview @Composable private fun HistoryCalendarCardLightPreview() = HistoryCalendarCardPreview(false)
@Preview @Composable private fun HistoryCalendarCardDarkPreview() = HistoryCalendarCardPreview(true)
