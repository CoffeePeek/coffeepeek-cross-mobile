package com.coffeepeek.feature.checkinhistory.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.component.*
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.*
import com.coffeepeek.feature.checkinhistory.impl.ui.data.historyDateTitle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ProfileHistoryListContent(state: CombinedHistoryState, onAction: (CombinedHistoryAction) -> Unit,
    listState: LazyListState, header: @Composable () -> Unit, image: HistoryImageContent?, formatDate: (String) -> String) {
    ProfileHistoryPagination(state, listState, onAction)
    CoffeePeekPullToRefresh(listState, state.isRefreshing, { onAction(CombinedHistoryAction.Refresh) },
        stringResource(Res.string.history_refresh), stringResource(Res.string.history_loading), Modifier.fillMaxSize()) { scroll ->
        LazyColumn(state = listState, modifier = scroll.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = coffeePeekScrollContentPadding(PaddingValues(bottom = 96.dp))) {
            item("profile-header") { header() }
            if (state.selectedDate != null) profileHistoryItem("profile-date") { ProfileHistorySelectedDate(state, onAction) }
            if (state.isTimelineLoading && state.timeline.isEmpty()) profileHistoryItem("profile-loading") {
                Box(Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
                    CoffeePeekLoader(stringResource(Res.string.history_loading))
                }
            }
            if (profileHistoryIsEmpty(state)) profileHistoryItem("profile-empty") {
                Text(stringResource(if (state.selectedDate == null) Res.string.history_list_empty else Res.string.history_day_empty),
                    Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(state.timeline, key = { it.key }) { item ->
                Box(Modifier.padding(horizontal = 16.dp)) { ProfileHistoryRow(item, onAction, image, formatDate) }
            }
            profileHistoryErrors(state, onAction)
            if (profileHistoryCanPage(state) || state.shops.isLoadingMore || state.homes.isLoadingMore) profileHistoryItem("profile-more") {
                ProfileHistoryMore(state, onAction)
            }
        }
    }
}

@Composable
private fun ProfileHistorySelectedDate(state: CombinedHistoryState, onAction: (CombinedHistoryAction) -> Unit) {
    val selected = requireNotNull(state.selectedDate)
    Column {
        Text(historyDateTitle(selected, selected.take(4) != state.today.take(4)),
            style = MaterialTheme.typography.titleMedium)
        CoffeePeekTextButton({ onAction(CombinedHistoryAction.SelectDate(null)) }) {
            Text(stringResource(Res.string.history_clear_date))
        }
    }
}

@Composable
private fun ProfileHistoryRow(item: HistoryTimelineItem, onAction: (CombinedHistoryAction) -> Unit,
    image: HistoryImageContent?, formatDate: (String) -> String) {
    val open = { onAction(CombinedHistoryAction.OpenEntry(item.key)) }
    when (item) {
        is HistoryTimelineItem.Shop -> {
            val card = LocalHistoryShopTimelineContent.current
            if (card != null) card(item.entry, open)
            else HistoryShopTimelineCard(item.entry, formatDate(item.timestamp), open,
                { onAction(CombinedHistoryAction.OpenEntryPhoto(item.key, it)) }, image)
        }
        is HistoryTimelineItem.Home -> {
            val card = LocalHistoryHomeTimelineContent.current
            if (card != null) card(item.entry, open)
            else HistoryHomeTimelineCard(item.entry, formatDate(item.timestamp), open,
                { onAction(CombinedHistoryAction.OpenEntryPhoto(item.key, it)) }, image)
        }
    }
}

@Composable
private fun ProfileHistoryMore(state: CombinedHistoryState, onAction: (CombinedHistoryAction) -> Unit) {
    val loading = state.isTimelineLoading || state.shops.isLoadingMore || state.homes.isLoadingMore
    Box(Modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
        if (loading) CoffeePeekLoader(stringResource(Res.string.history_loading))
        else CoffeePeekTextButton({ onAction(CombinedHistoryAction.LoadMore) }) { Text(stringResource(Res.string.history_more)) }
    }
}

private data class ProfileHistoryPageRequest(val shops: Int, val homes: Int, val rows: Int, val date: String?)

@Composable
private fun ProfileHistoryPagination(state: CombinedHistoryState, list: LazyListState, onAction: (CombinedHistoryAction) -> Unit) {
    val current by rememberUpdatedState(state)
    val dispatch by rememberUpdatedState(onAction)
    LaunchedEffect(list) {
        snapshotFlow {
            val snapshot = current
            if (profileHistoryCanPage(snapshot) && !snapshot.isTimelineLoading && !snapshot.shops.isLoadingMore &&
                !snapshot.homes.isLoadingMore && list.layoutInfo.visibleItemsInfo.any { it.key == "profile-more" }) {
                ProfileHistoryPageRequest(snapshot.shops.page, snapshot.homes.page, snapshot.timeline.size, snapshot.selectedDate)
            } else null
        }.distinctUntilChanged().filterNotNull().collect { dispatch(CombinedHistoryAction.LoadMore) }
    }
}

private fun profileHistoryCanPage(state: CombinedHistoryState): Boolean = !state.isCalendarUnavailable &&
    (state.selectedDate == null && state.shops.hasMore && !state.shops.hasError && !state.shops.hasLoadMoreError ||
        state.homes.hasMore && !state.homes.hasError && !state.homes.hasLoadMoreError)

private fun profileHistoryIsEmpty(state: CombinedHistoryState): Boolean = !state.isTimelineLoading && state.timeline.isEmpty() &&
    !state.isCalendarUnavailable && !(if (state.selectedDate == null) state.shops.hasError else state.shopCalendar.hasError) &&
    !state.homes.hasError

private fun LazyListScope.profileHistoryItem(key: String, content: @Composable () -> Unit) {
    item(key) { Box(Modifier.padding(horizontal = 16.dp)) { content() } }
}

private fun LazyListScope.profileHistoryErrors(state: CombinedHistoryState, onAction: (CombinedHistoryAction) -> Unit) {
    if (state.isCalendarUnavailable) profileHistoryItem("profile-clock-error") {
        HistorySourceError(stringResource(Res.string.history_clock_error)) { onAction(CombinedHistoryAction.Refresh) }
    } else HistorySource.entries.forEach { source ->
        profileHistoryCalendarError(state, source, onAction)
        profileHistoryTimelineError(state, source, onAction)
        profileHistoryPagingError(state, source, onAction)
    }
}

private fun LazyListScope.profileHistoryCalendarError(state: CombinedHistoryState, source: HistorySource,
    onAction: (CombinedHistoryAction) -> Unit) {
    val shop = source == HistorySource.Shop
    if (if (shop) state.shopCalendar.hasError else state.homeCalendar.hasError) profileHistoryItem("profile-calendar-error:$source") {
        HistorySourceError(stringResource(if (shop) Res.string.history_shop_calendar_error else Res.string.history_home_calendar_error)) {
            onAction(CombinedHistoryAction.RetryCalendar(source))
        }
    }
}

private fun LazyListScope.profileHistoryTimelineError(state: CombinedHistoryState, source: HistorySource,
    onAction: (CombinedHistoryAction) -> Unit) {
    val shop = source == HistorySource.Shop
    if (if (shop) state.selectedDate == null && state.shops.hasError else state.homes.hasError) profileHistoryItem("profile-timeline-error:$source") {
        HistorySourceError(stringResource(if (shop) Res.string.history_shop_timeline_error else Res.string.history_home_timeline_error)) {
            onAction(CombinedHistoryAction.RetryTimeline(source))
        }
    }
}

private fun LazyListScope.profileHistoryPagingError(state: CombinedHistoryState, source: HistorySource,
    onAction: (CombinedHistoryAction) -> Unit) {
    val shop = source == HistorySource.Shop
    if (if (shop) state.selectedDate == null && state.shops.hasLoadMoreError else state.homes.hasLoadMoreError) profileHistoryItem("profile-page-error:$source") {
        HistorySourceError(stringResource(if (shop) Res.string.history_shop_paging_error else Res.string.history_home_paging_error)) {
            onAction(CombinedHistoryAction.RetryLoadMore(source))
        }
    }
}

@Composable
private fun ProfileHistoryListPreview(dark: Boolean, failed: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    val shop = HistoryTimelineItem.Shop(previewHistoryCheckIn(), "2026-11-17")
    val home = HistoryTimelineItem.Home(previewHomeHistoryEntry())
    val state = CombinedHistoryState("2026-11-17", HistoryMonth(2026, 11), true, 0,
        shops = CombinedHistoryPageState(listOf(shop), hasLoaded = true, hasMore = false),
        homes = CombinedHistoryPageState(listOf(home), hasLoaded = true, hasMore = false, hasError = failed))
    ProfileHistoryListContent(state, {}, rememberLazyListState(), {}, null, { "17 ноября" })
}

@Preview @Composable private fun ProfileHistoryListLightPreview() = ProfileHistoryListPreview(false)
@Preview @Composable private fun ProfileHistoryListDarkPreview() = ProfileHistoryListPreview(true)
@Preview @Composable private fun ProfileHistoryListErrorLightPreview() = ProfileHistoryListPreview(false, true)
@Preview @Composable private fun ProfileHistoryListErrorDarkPreview() = ProfileHistoryListPreview(true, true)
