package com.coffeepeek.feature.checkinhistory.impl.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.impl.resources.Res
import com.coffeepeek.feature.checkinhistory.impl.resources.history_activity_failed
import com.coffeepeek.feature.checkinhistory.impl.resources.history_retry
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.component.HistoryCalendarCard
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.component.HistoryImageContent
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CombinedHistoryAction
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CombinedHistoryState
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.ProfileActivityAction
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.ProfileActivityState

@Composable
internal fun ProfileActivityCalendarContent(
    state: ProfileActivityState, onAction: (ProfileActivityAction) -> Unit, image: HistoryImageContent,
    selectedDate: String?, onSelectDate: (String?) -> Unit, expanded: Boolean, title: @Composable (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        HistoryCalendarCard(state.calendar.copy(selectedDate = selectedDate), { action ->
            when (action) {
                is CombinedHistoryAction.ChangeMonth -> onAction(ProfileActivityAction.ChangeMonth(action.offset))
                is CombinedHistoryAction.SelectDate -> onSelectDate(action.date?.takeUnless { it == selectedDate })
                else -> Unit
            }
        }, imageContent = image, framed = false, profileTitle = title, profileWeek = if (expanded) null else state.weekDates)
        if (state.failed && !state.loading) {
            Text(stringResource(Res.string.history_activity_failed), color = MaterialTheme.colorScheme.onSurfaceVariant)
            CoffeePeekTextButton(onClick = { onAction(ProfileActivityAction.Retry) }) {
                Text(stringResource(Res.string.history_retry))
            }
        }
    }
}

@Composable
private fun ActivityCalendarPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val state = ProfileActivityState(CombinedHistoryState("2026-10-25", HistoryMonth(2026, 10), true, 0))
    ProfileActivityCalendarContent(state, {}, { _, _, _ -> }, "2026-10-25", {}, true, { Text(it) })
}

@Preview @Composable private fun ActivityCalendarLightPreview() = ActivityCalendarPreview(false)
@Preview @Composable private fun ActivityCalendarDarkPreview() = ActivityCalendarPreview(true)
