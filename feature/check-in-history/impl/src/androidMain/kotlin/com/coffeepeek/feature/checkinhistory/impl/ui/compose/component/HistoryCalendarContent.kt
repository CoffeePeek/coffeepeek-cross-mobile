package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.coffeepeek.core.designsystem.component.CoffeePeekPullToRefresh
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CheckInHistoryAction
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistoryCalendarState
import com.coffeepeek.feature.checkinhistory.impl.ui.data.historyDateTitle
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HistoryCalendarContent(state: HistoryCalendarState, today: String, canGoNext: Boolean,
    onAction: (CheckInHistoryAction) -> Unit, formatCheckInDate: (String) -> String,
    modifier: Modifier = Modifier, imageContent: HistoryImageContent? = null) {
    val listState = rememberLazyListState()
    CoffeePeekPullToRefresh(listState, state.isLoading, { onAction(CheckInHistoryAction.Refresh) },
        stringResource(Res.string.history_refresh), stringResource(Res.string.history_loading),
        modifier = modifier.fillMaxSize(), enabled = state.hasLoaded) { scrollModifier ->
        LazyColumn(state = listState, modifier = scrollModifier.fillMaxSize(),
            contentPadding = PaddingValues(CoffeePeekDimens.spacing4),
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
            item(key = "calendar") {
                HistoryCalendarCard(state, today, canGoNext,
                    { onAction(CheckInHistoryAction.ChangeMonth(it)) },
                    { onAction(CheckInHistoryAction.SelectDate(it)) }, imageContent = imageContent)
            }
            if (state.isLoading || state.hasError) item(key = "status") {
                HistoryLoadStatus(state.isLoading, state.hasError, state.hasLoaded, { onAction(CheckInHistoryAction.Retry) })
            }
            if (state.hasLoaded) {
                state.selectedDate?.let { date ->
                    item(key = "selected-date") { Text(historyDateTitle(date), style = MaterialTheme.typography.titleMedium) }
                    if (state.selectedCheckIns.isEmpty()) item(key = "empty-day") { Text(stringResource(Res.string.history_day_empty)) }
                    items(state.selectedCheckIns, key = { "visit:${it.id}" }) { checkIn ->
                        HistoryCheckInCard(checkIn, if (checkIn.createdAt.isBlank()) "" else formatCheckInDate(checkIn.createdAt),
                            { onAction(CheckInHistoryAction.OpenShop(checkIn.id)) },
                            { onAction(CheckInHistoryAction.OpenPhoto(checkIn.id, it)) }, imageContent = imageContent)
                    }
                } ?: item(key = "empty-month") { Text(stringResource(Res.string.history_month_empty)) }
            }
        }
    }
}

@PreviewLightDark @Composable private fun HistoryCalendarContentPreview() = CoffeePeekTheme {
    HistoryCalendarContent(HistoryCalendarState(HistoryMonth(2026, 10),
        mapOf("2026-10-14" to listOf(previewHistoryCheckIn())), "2026-10-14", true),
        "2026-10-14", false, {}, { "14 октября 2026" })
}
