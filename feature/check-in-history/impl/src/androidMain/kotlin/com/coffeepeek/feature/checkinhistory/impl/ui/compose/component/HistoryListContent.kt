package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.coffeepeek.core.designsystem.component.CoffeePeekPullToRefresh
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CheckInHistoryAction
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistoryListState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HistoryListContent(state: HistoryListState, onAction: (CheckInHistoryAction) -> Unit,
    formatCheckInDate: (String) -> String, modifier: Modifier = Modifier, imageContent: HistoryImageContent? = null) {
    val listState = rememberLazyListState()
    val needsPage by remember(listState, state) { derivedStateOf {
        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        state.hasLoaded && state.hasMore && !state.isLoading && !state.isLoadingMore && !state.hasError &&
            !state.hasLoadMoreError && last >= 0 && last >= state.checkIns.size - 3
    } }
    LaunchedEffect(needsPage) { if (needsPage) onAction(CheckInHistoryAction.LoadMore) }
    CoffeePeekPullToRefresh(listState, state.isLoading, { onAction(CheckInHistoryAction.Refresh) },
        stringResource(Res.string.history_refresh), stringResource(Res.string.history_loading),
        modifier = modifier.fillMaxSize(), enabled = state.hasLoaded) { scrollModifier ->
        LazyColumn(state = listState, modifier = scrollModifier.fillMaxSize(),
            contentPadding = PaddingValues(CoffeePeekDimens.spacing4),
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
            if (state.isLoading || state.hasError) item(key = "status") {
                HistoryLoadStatus(state.isLoading, state.hasError, state.hasLoaded, { onAction(CheckInHistoryAction.Retry) })
            }
            if (state.hasLoaded && state.checkIns.isEmpty() && !state.isLoading && !state.hasError) {
                item(key = "empty") { Text(stringResource(Res.string.history_list_empty)) }
            }
            items(state.checkIns, key = { "visit:${it.id}" }) { checkIn ->
                HistoryCheckInCard(checkIn, if (checkIn.createdAt.isBlank()) "" else formatCheckInDate(checkIn.createdAt),
                    { onAction(CheckInHistoryAction.OpenShop(checkIn.id)) },
                    { onAction(CheckInHistoryAction.OpenPhoto(checkIn.id, it)) }, imageContent = imageContent)
            }
            if (state.isLoadingMore || state.hasLoadMoreError) item(key = "paging-status") {
                HistoryLoadStatus(state.isLoadingMore, state.hasLoadMoreError, true,
                    { onAction(CheckInHistoryAction.RetryLoadMore) }, paging = true)
            }
        }
    }
}

@PreviewLightDark @Composable private fun HistoryListContentPreview() = CoffeePeekTheme {
    HistoryListContent(HistoryListState(listOf(previewHistoryCheckIn()), page = 1, hasLoaded = true), {}, { "14 октября 2026" })
}
