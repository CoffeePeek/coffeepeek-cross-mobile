package com.coffeepeek.feature.shop.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold as Scaffold
import com.coffeepeek.core.designsystem.component.coffeePeekScrollContentPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_list_title
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_list_empty
import com.coffeepeek.feature.shop.impl.resources.shop_detail_back
import com.coffeepeek.feature.shop.impl.ui.ShopCheckInListViewModel
import com.coffeepeek.feature.shop.impl.ui.compose.component.LocalShopPhotoPrefetch
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopCheckInCard
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopCheckInGuestPlaceholder
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopCheckInGuestPrompt
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopCheckInListStatus
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopCheckInRatingsOverview
import com.coffeepeek.feature.shop.impl.ui.compose.component.previewCheckIn
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInListAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInListEvent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInListState
import com.coffeepeek.feature.shop.impl.ui.data.ratingSummary
import com.coffeepeek.feature.shop.impl.ui.data.blockableAuthor
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInListScreen(
    viewModel: ShopCheckInListViewModel,
    sessionGeneration: Int,
    formatCheckInDate: (String) -> String,
    refreshGeneration: Long = 0,
    onEvent: (ShopCheckInListEvent) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnEvent by rememberUpdatedState(onEvent)
    val currentGeneration by rememberUpdatedState(sessionGeneration)
    LaunchedEffect(viewModel, sessionGeneration) {
        viewModel.events.collect { envelope ->
            if (envelope.sessionGeneration == currentGeneration) currentOnEvent(envelope.event)
        }
    }
    LaunchedEffect(viewModel, sessionGeneration) {
        viewModel.onAction(ShopCheckInListAction.SessionChanged(sessionGeneration))
    }
    LaunchedEffect(viewModel, refreshGeneration) {
        viewModel.onAction(ShopCheckInListAction.Refresh(refreshGeneration))
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onAction(ShopCheckInListAction.Resume) }
    // Suppress old-account content immediately, before the session-change action is processed.
    val visibleState = state.takeIf { it.sessionGeneration == sessionGeneration }
        ?: ShopCheckInListState(sessionGeneration = sessionGeneration)
    ShopCheckInListScreenContent(visibleState, viewModel::onAction, formatCheckInDate, sessionGeneration)
}

@Composable
internal fun ShopCheckInListScreenContent(
    state: ShopCheckInListState,
    onAction: (ShopCheckInListAction) -> Unit,
    formatCheckInDate: (String) -> String = { it.take(10) },
    sessionGeneration: Int = 0,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(sessionGeneration) { listState.scrollToItem(0) }
    LocalShopPhotoPrefetch.current?.invoke(listState, state.checkIns
        .take(if (state.isLoggedIn) Int.MAX_VALUE else 1)
        .mapIndexed { index, checkIn -> "check-in:${checkIn.id}:$index" to checkIn.photoThumbnailUrls.take(3) })
    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        topBar = {
            CoffeePeekTopBar(stringResource(Res.string.shop_checkin_list_title),
                stringResource(Res.string.shop_detail_back), onBack = { onAction(ShopCheckInListAction.Back) })
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets), contentAlignment = Alignment.Center) {
            if (!state.hasLoaded) {
                ShopCheckInListStatus(state.isLoading, false, state.hasLoadError, false,
                    onRetry = { onAction(ShopCheckInListAction.Retry) },
                    modifier = Modifier.padding(CoffeePeekDimens.spacing6))
            } else LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = coffeePeekScrollContentPadding(PaddingValues(CoffeePeekDimens.spacing4)),
                verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
            ) {
                item(key = "shop-title") { Text(state.shopTitle, style = MaterialTheme.typography.titleLarge) }
                item(key = "ratings") {
                    ShopCheckInRatingsOverview(emptyList(), state.overallRating, state.checkInCount,
                        summary = state.ratingSummary, sampleSize = state.ratingSampleSize)
                }
                if (state.isLoading || state.hasLoadError || state.hasVoteError) item(key = "status") {
                    ShopCheckInListStatus(state.isLoading, true, state.hasLoadError, state.hasVoteError,
                        onRetry = { onAction(ShopCheckInListAction.Retry) }, modifier = Modifier.fillMaxWidth())
                }
                if (state.checkIns.isEmpty()) item(key = "empty") {
                    Text(stringResource(Res.string.shop_checkin_list_empty),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                itemsIndexed(state.checkIns, key = { index, checkIn -> "check-in:${checkIn.id}:$index" }) { _, checkIn ->
                    val isOwn = state.owns(checkIn)
                    ShopCheckInCard(checkIn,
                        onOpenPhoto = { _, index -> onAction(ShopCheckInListAction.OpenPhoto(checkIn.id, index)) },
                        showAuthor = true, showHelpful = true, isOwn = isOwn,
                        isVoting = state.pendingVoteId == checkIn.id,
                        onVote = if (state.pendingVoteId != null || state.isLoading) null
                            else { { onAction(ShopCheckInListAction.ToggleHelpful(checkIn.id)) } },
                        onReport = if (isOwn) null else { { onAction(ShopCheckInListAction.Report(checkIn.id)) } },
                        onBlockAuthor = checkIn.blockableAuthor(isOwn, state.isLoggedIn, state.currentUserId)
                            ?.takeUnless { state.isLoading }?.let {
                                { onAction(ShopCheckInListAction.BlockAuthor(checkIn.id)) }
                            },
                        onOpenAuthor = checkIn.authorSlug?.takeIf(String::isNotBlank)?.let {
                            { onAction(ShopCheckInListAction.OpenAuthor(checkIn.id)) }
                        },
                        dateLabel = formatCheckInDate(checkIn.createdAt), fullVersion = true)
                }
                if (!state.isLoggedIn && state.checkIns.isNotEmpty()) item(key = "guest-prompt") {
                    ShopCheckInGuestPrompt({ onAction(ShopCheckInListAction.SignIn) },
                        { onAction(ShopCheckInListAction.Register) })
                }
                items(state.hiddenCheckInCount, key = { "hidden:$it" }) {
                    ShopCheckInGuestPlaceholder(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

private fun previewCheckInListState(guest: Boolean = false): ShopCheckInListState {
    val checkIns = listOf(previewCheckIn(), previewCheckIn().copy(id = "check-in-2", userId = "user-2"))
    return ShopCheckInListState(isLoading = false, hasLoaded = true, shopTitle = "Кофейня",
        checkIns = if (guest) checkIns.take(1) else checkIns, hiddenCheckInCount = if (guest) 1 else 0,
        checkInCount = 2, ratingSampleSize = 2, ratingSummary = checkIns.ratingSummary(),
        isLoggedIn = !guest, currentUserId = if (guest) null else "user-2")
}

@Preview @Composable private fun ShopCheckInListLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInListScreenContent(previewCheckInListState(), {})
}
@Preview @Composable private fun ShopCheckInListDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInListScreenContent(previewCheckInListState(), {})
}
@Preview @Composable private fun ShopCheckInListGuestLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInListScreenContent(previewCheckInListState(guest = true), {})
}
@Preview @Composable private fun ShopCheckInListGuestDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInListScreenContent(previewCheckInListState(guest = true), {})
}
@Preview @Composable private fun ShopCheckInListEmptyLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInListScreenContent(ShopCheckInListState(isLoading = false, hasLoaded = true, shopTitle = "Кофейня"), {})
}
@Preview @Composable private fun ShopCheckInListEmptyDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInListScreenContent(ShopCheckInListState(isLoading = false, hasLoaded = true, shopTitle = "Кофейня"), {})
}
@Preview @Composable private fun ShopCheckInListErrorLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInListScreenContent(ShopCheckInListState(isLoading = false, hasLoadError = true), {})
}
@Preview @Composable private fun ShopCheckInListErrorDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInListScreenContent(ShopCheckInListState(isLoading = false, hasLoadError = true), {})
}
@Preview @Composable private fun ShopCheckInListLoadingLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInListScreenContent(ShopCheckInListState(), {})
}
@Preview @Composable private fun ShopCheckInListLoadingDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInListScreenContent(ShopCheckInListState(), {})
}
