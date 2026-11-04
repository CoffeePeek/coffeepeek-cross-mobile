package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekPullToRefresh
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.*
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.previewCombinedHistoryState
import com.coffeepeek.feature.checkinhistory.impl.ui.data.historyDateTitle
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun HistoryTimelineContent(state: CombinedHistoryState, onAction: (CombinedHistoryAction) -> Unit,
    formatTime: (String) -> String, modifier: Modifier = Modifier, imageContent: HistoryImageContent? = null) {
    val listState = rememberLazyListState()
    val entries = state.timeline
    CoffeePeekPullToRefresh(listState, state.isRefreshing, { onAction(CombinedHistoryAction.Refresh) },
        stringResource(Res.string.history_refresh), stringResource(Res.string.history_loading), modifier.fillMaxSize()) { scroll ->
        LazyColumn(state = listState, modifier = scroll.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)) {
            item("calendar") { HistoryCalendarCard(state, onAction, imageContent = imageContent) }
            if (state.isTimelineLoading && entries.isEmpty()) item("initial-loading") {
                Box(Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
                    CoffeePeekLoader(stringResource(Res.string.history_loading))
                }
            }
            if (state.selectedDate != null) item("clear-date") {
                TextButton({ onAction(CombinedHistoryAction.SelectDate(null)) }) { Text(stringResource(Res.string.history_clear_date)) }
            }
            if (state.isCalendarUnavailable) item("clock-error") {
                HistorySourceError(stringResource(Res.string.history_clock_error)) { onAction(CombinedHistoryAction.Refresh) }
            }
            if (!state.isTimelineLoading && entries.isEmpty() && !state.isCalendarUnavailable &&
                !(state.filter.includes(HistorySource.Shop) && (if (state.selectedDate == null) state.shops.hasError else state.shopCalendar.hasError)) &&
                !(state.filter.includes(HistorySource.Home) && state.homes.hasError)) item("empty") {
                Text(stringResource(if (state.selectedDate != null) Res.string.history_day_empty else Res.string.history_list_empty),
                    Modifier.padding(vertical = 32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            itemsIndexed(entries, key = { _, item -> item.key }) { index, item ->
                val first = index == 0 || entries[index - 1].date != item.date
                val last = index == entries.lastIndex || entries[index + 1].date != item.date
                Column {
                    if (first) Text(historyDateTitle(item.date, item.date.take(4) != state.today.take(4)),
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp))
                    HistoryTimelineRail(item is HistoryTimelineItem.Home, first, last) {
                        when (item) {
                            is HistoryTimelineItem.Shop -> HistoryShopTimelineCard(item.entry, formatTime(item.timestamp)) {
                                onAction(CombinedHistoryAction.OpenEntry(item.key))
                            }
                            is HistoryTimelineItem.Home -> HistoryHomeTimelineCard(item.entry, formatTime(item.timestamp)) {
                                onAction(CombinedHistoryAction.OpenEntry(item.key))
                            }
                        }
                    }
                }
            }
            if (!state.isCalendarUnavailable) {
                HistorySource.entries.filter(state.filter::includes).forEach { source ->
                    val shop = source == HistorySource.Shop
                    val calendarError = if (shop) state.shopCalendar.hasError else state.homeCalendar.hasError
                    val timelineError = if (shop) state.selectedDate == null && state.shops.hasError else state.homes.hasError
                    val pagingError = if (shop) state.selectedDate == null && state.shops.hasLoadMoreError else state.homes.hasLoadMoreError
                    if (calendarError) item("calendar-error:$source") {
                        HistorySourceError(stringResource(if (shop) Res.string.history_shop_calendar_error else Res.string.history_home_calendar_error)) {
                            onAction(CombinedHistoryAction.RetryCalendar(source))
                        }
                    }
                    if (timelineError) item("timeline-error:$source") {
                        HistorySourceError(stringResource(if (shop) Res.string.history_shop_timeline_error else Res.string.history_home_timeline_error)) {
                            onAction(CombinedHistoryAction.RetryTimeline(source))
                        }
                    }
                    if (pagingError) item("paging-error:$source") {
                        HistorySourceError(stringResource(if (shop) Res.string.history_shop_paging_error else Res.string.history_home_paging_error)) {
                            onAction(CombinedHistoryAction.RetryLoadMore(source))
                        }
                    }
                }
                val canPageShops = state.filter.includes(HistorySource.Shop) && state.selectedDate == null &&
                    state.shops.hasMore && !state.shops.hasError && !state.shops.hasLoadMoreError
                val canPageHomes = state.filter.includes(HistorySource.Home) && state.homes.hasMore &&
                    !state.homes.hasError && !state.homes.hasLoadMoreError
                if (canPageShops || canPageHomes || state.shops.isLoadingMore || state.homes.isLoadingMore) item("more") {
                    TextButton({ onAction(CombinedHistoryAction.LoadMore) }, Modifier.fillMaxWidth(),
                        enabled = !state.isTimelineLoading && !state.shops.isLoadingMore && !state.homes.isLoadingMore) {
                        Text(stringResource(if (state.shops.isLoadingMore || state.homes.isLoadingMore) Res.string.history_loading else Res.string.history_more))
                    }
                }
            }
        }
    }
}

@Composable private fun HistoryTimelinePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    HistoryTimelineContent(previewCombinedHistoryState(), {}, { "13:00" })
}
@Preview @Composable private fun HistoryTimelineLightPreview() = HistoryTimelinePreview(false)
@Preview @Composable private fun HistoryTimelineDarkPreview() = HistoryTimelinePreview(true)

@Composable private fun HistoryTimelineLoadingPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    HistoryTimelineContent(previewCombinedHistoryState().copy(homeCalendar = CombinedHistoryLoadState(emptyMap(), isLoading = true)), {}, { "13:00" })
}
@Preview @Composable private fun HistoryTimelineLoadingLightPreview() = HistoryTimelineLoadingPreview(false)
@Preview @Composable private fun HistoryTimelineLoadingDarkPreview() = HistoryTimelineLoadingPreview(true)

@Composable private fun HistoryTimelineRefreshingPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    HistoryTimelineContent(previewCombinedHistoryState().copy(isRefreshing = true), {}, { "13:00" })
}
@Preview @Composable private fun HistoryTimelineRefreshingLightPreview() = HistoryTimelineRefreshingPreview(false)
@Preview @Composable private fun HistoryTimelineRefreshingDarkPreview() = HistoryTimelineRefreshingPreview(true)
