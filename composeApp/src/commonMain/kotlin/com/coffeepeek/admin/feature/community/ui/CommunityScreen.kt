package com.coffeepeek.admin.feature.community.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CoffeePeekPullToRefresh
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.admin.ui.component.LoadErrorContent
import com.coffeepeek.admin.ui.component.ReviewTextInput
import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateButton
import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateFlow
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.CheckInModerationState
import com.coffeepeek.domain.model.CheckInVisibility

@Composable
fun CommunityScreen(onCreateCheckIn: () -> Unit) {
    val vm: CommunityViewModel = platformViewModel()
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    val clearance = LocalFloatingNavClearance.current
    var preview by remember { mutableStateOf<CheckInPhotoPreview?>(null) }
    var displayedGeneration by rememberSaveable { mutableStateOf(state.sessionGeneration) }
    var homeEditorOpened by rememberSaveable { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (homeEditorOpened) { homeEditorOpened = false; vm.refresh() }
    }

    LaunchedEffect(state.sessionGeneration) {
        if (displayedGeneration != state.sessionGeneration) {
            preview = null
            showCreate = false
            listState.scrollToItem(0)
            displayedGeneration = state.sessionGeneration
        }
    }
    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let { snackbar.showSnackbar(it); vm.clearActionMessage() }
    }
    LaunchedEffect(state.needsLogin) {
        if (state.needsLogin) { vm.loginHandled(); Navigator.navigate(Navigator.Screen.Auth) }
    }
    val shouldLoadMore by remember(state.items.size, state.hasMore, state.isLoading, state.isLoadingMore, state.error, state.isMutating) {
        derivedStateOf {
            state.hasMore && !state.isLoading && !state.isLoadingMore && !state.isMutating && state.error == null &&
                (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= state.items.size - 3
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) vm.loadMore() }

    preview?.takeIf { it.generation == state.sessionGeneration }?.let {
        FullScreenImageDialog(imageUrls = it.urls, initialIndex = it.index, onDismiss = { preview = null })
    }
    if (state.editing != null) key(state.sessionGeneration, state.editing?.source?.id) { EditCheckInSheet(state, vm) }
    if (state.reportingId != null) com.coffeepeek.admin.ui.component.SwipeDismissModalBottomSheet(
        onDismissRequest = vm::dismissReport, dismissEnabled = !state.isReporting,
    ) {
        Column(Modifier.fillMaxWidth().padding(CpDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
            Text("Пожаловаться на чекин", style = MaterialTheme.typography.titleLarge)
            ReviewTextInput(state.reportText, vm::updateReport, "Опишите проблему", maxLength = 2000, isError = state.reportError != null)
            state.reportError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            TextButton(onClick = vm::submitReport, enabled = !state.isReporting) { Text(if (state.isReporting) "Отправляем…" else "Отправить") }
            TextButton(onClick = vm::dismissReport, enabled = !state.isReporting) { Text("Отмена") }
        }
    }

    if (showCreate) CheckInCreateFlow(
        onDismiss = { showCreate = false },
        onShop = { showCreate = false; onCreateCheckIn() },
        onHomeSaved = vm::refresh,
    )

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            CoffeePeekPullToRefresh(
                listState, isRefreshing = state.isLoading && state.items.isNotEmpty(), onRefresh = vm::refresh,
                modifier = Modifier.fillMaxSize().statusBarsPadding().padding(top = padding.calculateTopPadding()),
            ) { scrollModifier ->
                LazyColumn(
                    state = listState, modifier = scrollModifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = CpDimens.spacing4, end = CpDimens.spacing4,
                        top = CpDimens.spacing4, bottom = clearance + 88.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
                ) {
                    item(key = "heading") {
                        Text(
                            "Лента",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    when {
                        state.isLoggedIn == null || (state.isLoading && state.items.isEmpty()) -> item(key = "loading") {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                                CoffeePeekLoader()
                            }
                        }
                        else -> {
                            itemsIndexed(state.items, key = { _, checkIn -> checkIn.id }) { index, checkIn ->
                                Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    TimelineCheckInCard(
                                        checkIn = checkIn,
                                        isOwn = state.owns(checkIn),
                                        isPublicTimeline = state.timeline == CommunityTimeline.Public,
                                        canAct = !state.isMutating,
                                        changingVisibility = state.changingVisibilityId == checkIn.id,
                                        onShopClick = { Navigator.navigate(Navigator.Screen.ShopDetail(checkIn.shopId)) },
                                        onEdit = {
                                            if (checkIn.isHome) {
                                                homeEditorOpened = true
                                                Navigator.navigate(Navigator.Screen.EditHomeCheckIn(checkIn.id))
                                            } else vm.edit(checkIn.id)
                                        },
                                        onVisibility = { vm.toggleVisibility(checkIn.id) },
                                        onHelpful = { vm.toggleHelpful(checkIn.id) },
                                        onReport = { vm.openReport(checkIn.id) },
                                        onPhotoClick = { urls, photoIndex ->
                                            preview = CheckInPhotoPreview(state.sessionGeneration, urls, photoIndex)
                                        },
                                    )
                                }
                            }
                            when {
                                state.error != null -> item(key = "error") {
                                    LoadErrorContent(
                                        onRetry = vm::retry,
                                        retryLabel = if (state.restartPagination) "Обновить ленту" else "Попробовать снова",
                                    )
                                }
                                state.items.isEmpty() && !state.isLoading -> item(key = "empty") {
                                    Text("Пока нет опубликованных чек-инов.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (state.isLoadingMore) item(key = "loading-more") {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                                    CoffeePeekLoader()
                                }
                            }
                        }
                    }
                }
            }
        }
        // Android supplies the panel's full clearance; UIKit already bounds this screen above its tab bar.
        // Position explicitly: Scaffold's built-in FAB inset would add another 16 dp.
        CheckInCreateButton(Modifier.align(Alignment.BottomEnd).padding(end = CpDimens.spacing4, bottom = clearance + 12.dp)) {
            if (state.isLoggedIn == true) showCreate = true
            else Navigator.navigate(Navigator.Screen.Auth)
        }
    }
}

internal fun CheckIn.publicationLabel(): String = if (visibility == CheckInVisibility.Private) "Приватный" else when (moderationState) {
    CheckInModerationState.Pending -> "На проверке"
    CheckInModerationState.Approved -> "Опубликован"
    CheckInModerationState.Rejected -> "Отклонён"
    CheckInModerationState.NotSubmitted -> "Публичный"
    CheckInModerationState.Unknown -> "Статус проверки неизвестен"
}

private data class CheckInPhotoPreview(val generation: Long, val urls: List<String>, val index: Int)
