package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
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
import com.coffeepeek.core.designsystem.theme.coffeePeekReduceMotion
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.*
import com.coffeepeek.feature.checkinhistory.impl.ui.data.historyCalendarPhotos
import com.coffeepeek.feature.checkinhistory.impl.ui.data.profileHistoryDates
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ProfileHistoryCalendarCard(state: CombinedHistoryState, expanded: Boolean,
    onToggle: () -> Unit, onAction: (CombinedHistoryAction) -> Unit, image: HistoryImageContent) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Box {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
                ProfileCalendarHeader(state, expanded, onToggle, onAction)
                ProfileCalendarWeekdays()
                HorizontalDivider(Modifier.padding(top = 10.dp, bottom = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ProfileCalendarDates(state, expanded, onAction, image)
            }
            if (state.shopCalendar.isLoading || state.homeCalendar.isLoading) {
                Box(Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
                    CoffeePeekLoader(stringResource(Res.string.history_activity_loading), size = 20.dp)
                }
            }
        }
    }
}

@Composable
private fun ProfileCalendarHeader(state: CombinedHistoryState, expanded: Boolean,
    onToggle: () -> Unit, onAction: (CombinedHistoryAction) -> Unit) {
    val toggleLabel = stringResource(if (expanded) Res.string.history_activity_collapse else Res.string.history_activity_expand)
    val rotation by animateFloatAsState(if (expanded) -180f else 0f,
        tween(if (coffeePeekReduceMotion()) 0 else 280), label = "Profile calendar expansion")
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (expanded) ProfileMonthArrow(false, state.month != HistoryMonth(1, 1)) { onAction(CombinedHistoryAction.ChangeMonth(-1)) }
            Text(stringArrayResource(Res.array.history_months)[state.month.month - 1],
                Modifier.weight(1f, fill = false).padding(start = if (expanded) 0.dp else 8.dp),
                style = MaterialTheme.typography.titleMedium)
            if (expanded) ProfileMonthArrow(true, state.canGoNextMonth) { onAction(CombinedHistoryAction.ChangeMonth(1)) }
        }
        CoffeePeekIconButton(onToggle, contentDescription = toggleLabel, glass = false,
            modifier = Modifier.semantics { stateDescription = toggleLabel }) {
            CoffeePeekIcon(CoffeePeekIcons.ChevronDown, null, Modifier.size(22.dp).rotate(rotation))
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
private fun ProfileCalendarDates(state: CombinedHistoryState, expanded: Boolean,
    onAction: (CombinedHistoryAction) -> Unit, image: HistoryImageContent) {
    val reduced = coffeePeekReduceMotion()
    val page = ProfileCalendarPage(state, expanded)
    BoxWithConstraints(Modifier.fillMaxWidth().clipToBounds()) {
        val height = maxOf(maxWidth / 7, 44.dp * LocalDensity.current.fontScale.coerceAtLeast(1f))
        AnimatedContent(page, transitionSpec = { profileCalendarTransition(initialState, targetState, reduced) },
            contentKey = { if (it.expanded) it.month else profileHistoryDates(it.today, it.month, it.selected, false).firstOrNull() },
            label = "Profile calendar page") { target ->
            Column {
                ProfileCalendarWeekRows(target.state, target, height, onAction, image)
            }
        }
    }
}

private data class ProfileCalendarPage(val state: CombinedHistoryState, val expanded: Boolean) {
    val month get() = state.month
    val today get() = state.today
    val selected get() = state.selectedDate
}

private fun profileCalendarTransition(from: ProfileCalendarPage, to: ProfileCalendarPage, reduced: Boolean): ContentTransform {
    val duration = if (reduced) 0 else 280
    val slide = !reduced && from.expanded && to.expanded && from.month != to.month
    val direction = if (to.month.date(1) > from.month.date(1)) 1 else -1
    val enter = fadeIn(tween(duration)) + if (slide) slideInHorizontally(tween(duration)) { it * direction } else EnterTransition.None
    val exit = fadeOut(tween(duration)) + if (slide) slideOutHorizontally(tween(duration)) { -it * direction } else ExitTransition.None
    return ContentTransform(enter, exit, sizeTransform = SizeTransform(clip = true) { _, _ -> tween(duration) })
}

@Composable
private fun ProfileCalendarWeekRows(state: CombinedHistoryState, page: ProfileCalendarPage,
    height: androidx.compose.ui.unit.Dp, onAction: (CombinedHistoryAction) -> Unit, image: HistoryImageContent) {
    val dates = profileHistoryDates(page.today, page.month, page.selected, page.expanded)
    dates.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            week.forEach { date ->
                val slot = Modifier.weight(1f).height(height)
                if (date == null) Spacer(slot)
                else ProfileCalendarDay(state, date, onAction, image, slot)
            }
        }
    }
}

@Composable
private fun ProfileCalendarDay(state: CombinedHistoryState, date: String,
    onAction: (CombinedHistoryAction) -> Unit, image: HistoryImageContent, modifier: Modifier) {
    HistoryCalendarDay(date.takeLast(2).toInt(), date, state.shopCalendar.content[date].orEmpty().size.toLong(),
        state.homeCalendar.content[date] ?: 0, state.selectedDate == date, state.today == date, date > state.today,
        { onAction(CombinedHistoryAction.SelectDate(date.takeUnless { state.selectedDate == it })) },
        modifier, historyCalendarPhotos(state, date), image,
        actionLabel = stringResource(if (state.selectedDate == date) Res.string.history_clear_date else Res.string.history_select_day))
}

@Composable
private fun ProfileCalendarCardPreview(dark: Boolean, expanded: Boolean,
    initialMonth: HistoryMonth = HistoryMonth(2026, 11)) = CoffeePeekTheme(darkTheme = dark) {
    var month by remember { mutableStateOf(initialMonth) }
    var open by remember { mutableStateOf(expanded) }
    var selected by remember { mutableStateOf<String?>(null) }
    ProfileHistoryCalendarCard(CombinedHistoryState("2026-11-17", month, true, 0, selectedDate = selected),
        open, { open = !open }, { action ->
            when (action) {
                is CombinedHistoryAction.ChangeMonth -> {
                    month = month.plusMonths(action.offset)
                    selected = null
                }
                is CombinedHistoryAction.SelectDate -> selected = action.date
                else -> Unit
            }
        }, { _, _, _ -> })
}

@Preview @Composable private fun ProfileCalendarWeekLightPreview() = ProfileCalendarCardPreview(false, false)
@Preview @Composable private fun ProfileCalendarWeekDarkPreview() = ProfileCalendarCardPreview(true, false)
@Preview @Composable private fun ProfileCalendarMonthLightPreview() = ProfileCalendarCardPreview(false, true)
@Preview @Composable private fun ProfileCalendarMonthDarkPreview() = ProfileCalendarCardPreview(true, true)
@Preview @Composable private fun ProfileCalendarFourWeeksLightPreview() = ProfileCalendarCardPreview(false, true, HistoryMonth(2021, 2))
@Preview @Composable private fun ProfileCalendarFourWeeksDarkPreview() = ProfileCalendarCardPreview(true, true, HistoryMonth(2021, 2))
@Preview @Composable private fun ProfileCalendarFiveWeeksLightPreview() = ProfileCalendarCardPreview(false, true, HistoryMonth(2026, 10))
@Preview @Composable private fun ProfileCalendarFiveWeeksDarkPreview() = ProfileCalendarCardPreview(true, true, HistoryMonth(2026, 10))
