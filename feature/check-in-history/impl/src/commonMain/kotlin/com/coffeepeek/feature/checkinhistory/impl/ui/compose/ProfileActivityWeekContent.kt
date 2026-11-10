package com.coffeepeek.feature.checkinhistory.impl.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.component.HistoryImageContent
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.*
import com.coffeepeek.feature.checkinhistory.impl.ui.data.*
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import org.jetbrains.compose.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ProfileActivityWeekContent(state: ProfileActivityState, onAction: (ProfileActivityAction) -> Unit,
    image: HistoryImageContent, modifier: Modifier = Modifier) {
    val weekdays = stringArrayResource(Res.array.history_weekdays)
    val photos = state.weekDates.map { historyCalendarPhotos(state.calendar, it, maxPhotos = Int.MAX_VALUE) }
    val days = state.weekDates.mapIndexed { index, date ->
        val count = state.calendar.calendarCounts[date] ?: 0L
        CoffeePeekActivityDay(weekdays[index], stringResource(Res.string.history_activity_day, date, historyVisitCountLabel(count)),
            date == state.calendar.today, count > 0, photos[index].size)
    }
    CoffeePeekActivityWeek(days, CoffeePeekActivityWeekLabels(stringResource(Res.string.history_activity_open),
        stringResource(Res.string.history_activity_loading), stringResource(Res.string.history_activity_failed),
        stringResource(Res.string.history_retry)), state.loading, state.failed,
        { onAction(ProfileActivityAction.OpenWeek()) }, { onAction(ProfileActivityAction.Retry) }, modifier) { index, imageModifier ->
        photos[index].firstOrNull()?.let { image(it.url, imageModifier, ContentScale.Crop) }
    }
}

@Composable private fun ActivityWeekPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ProfileActivityWeekContent(ProfileActivityState(CombinedHistoryState("2026-10-25", HistoryMonth(2026, 10), true, 0)), {}, { _, _, _ -> })
}
@Preview @Composable private fun ActivityWeekLightPreview() = ActivityWeekPreview(false)
@Preview @Composable private fun ActivityWeekDarkPreview() = ActivityWeekPreview(true)
