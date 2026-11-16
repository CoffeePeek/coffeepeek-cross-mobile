package com.coffeepeek.feature.checkinhistory.impl.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.component.HistoryImageContent
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.component.ProfileActivityCalendarCard
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.*
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Profile-only calendar presentation; the full history calendar keeps its filtering controls. */
@Composable
internal fun ProfileActivitySummaryContent(state: ProfileActivityState, onAction: (ProfileActivityAction) -> Unit,
    image: HistoryImageContent, timeLabel: (String) -> String, expanded: Boolean,
    onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        ProfileActivityCalendarCard(state, expanded, onToggle, onAction, image)
        if (state.failed && !state.loading) {
            Text(stringResource(Res.string.history_activity_failed), color = MaterialTheme.colorScheme.onSurfaceVariant)
            CoffeePeekTextButton(onClick = { onAction(ProfileActivityAction.Retry) }) {
                Text(stringResource(Res.string.history_retry))
            }
        }
        if (expanded && !state.loading && !state.failed && state.calendar.selectedDate != null) {
            ActivitySelectedDay(state, onAction, image, timeLabel)
        }
    }
}

@Composable
private fun ProfileActivitySummaryPreview(dark: Boolean, expanded: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val state = ProfileActivityState(CombinedHistoryState("2026-11-17", HistoryMonth(2026, 11), true, 0,
        shopCalendar = CombinedHistoryLoadState(emptyMap(), hasLoaded = true),
        homeCalendar = CombinedHistoryLoadState(mapOf("2026-11-10" to 1, "2026-11-15" to 1), hasLoaded = true)))
    ProfileActivitySummaryContent(state, {}, { _, _, _ -> }, { "12:00" }, expanded, {})
}

@Preview @Composable private fun ProfileWeekLightPreview() = ProfileActivitySummaryPreview(false, false)
@Preview @Composable private fun ProfileWeekDarkPreview() = ProfileActivitySummaryPreview(true, false)
@Preview @Composable private fun ProfileMonthLightPreview() = ProfileActivitySummaryPreview(false, true)
@Preview @Composable private fun ProfileMonthDarkPreview() = ProfileActivitySummaryPreview(true, true)
