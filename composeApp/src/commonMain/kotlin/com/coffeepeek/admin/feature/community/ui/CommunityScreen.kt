package com.coffeepeek.admin.feature.community.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.AppButton
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CoffeePeekPullToRefresh
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.admin.ui.component.ReviewDisplayCard
import com.coffeepeek.admin.ui.component.ReviewTextInput
import com.coffeepeek.admin.utils.utcIsoToLocalDate
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.CheckInModerationState
import com.coffeepeek.domain.model.CheckInVisibility
import com.coffeepeek.domain.model.Review
import com.coffeepeek.domain.model.ReviewRating

@Composable
fun CommunityScreen() {
    val vm: CommunityViewModel = platformViewModel()
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    val clearance = LocalFloatingNavClearance.current
    var preview by remember { mutableStateOf<CheckInPhotoPreview?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }
    LaunchedEffect(state.sessionGeneration) { preview = null; listState.scrollToItem(0) }
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
    if (state.reportingId != null) AlertDialog(
        onDismissRequest = vm::dismissReport,
        title = { Text("Пожаловаться на чек-ин") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
                ReviewTextInput(state.reportText, vm::updateReport, "Опишите проблему", maxLength = 2000, isError = state.reportError != null)
                state.reportError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = vm::submitReport, enabled = !state.isReporting) { Text(if (state.isReporting) "Отправляем…" else "Отправить") } },
        dismissButton = { TextButton(onClick = vm::dismissReport, enabled = !state.isReporting) { Text("Отмена") } },
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        CoffeePeekPullToRefresh(
            listState, isRefreshing = state.isLoading && state.items.isNotEmpty(), onRefresh = vm::refresh,
            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
        ) { scrollModifier ->
            LazyColumn(
                state = listState, modifier = scrollModifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = CpDimens.spacing4, end = CpDimens.spacing4,
                    top = CpDimens.spacing3, bottom = clearance + CpDimens.spacing4,
                ),
                verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
            ) {
                item(key = "heading") {
                    Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
                        Text("Лента", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
                            FilterChip(selected = state.timeline == CommunityTimeline.Public,
                                onClick = { vm.selectTimeline(CommunityTimeline.Public) }, label = { Text("Все чек-ины") })
                            FilterChip(selected = state.timeline == CommunityTimeline.Mine,
                                onClick = { vm.selectTimeline(CommunityTimeline.Mine) }, label = { Text("Мои чек-ины") })
                        }
                    }
                }
                when {
                    state.isLoggedIn == null || (state.isLoading && state.items.isEmpty()) -> item {
                        CoffeePeekLoader()
                    }
                    state.timeline == CommunityTimeline.Mine && state.isLoggedIn == false -> item {
                        Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                            Text("Войдите, чтобы видеть и редактировать свои чек-ины.")
                            AppButton("Войти", onClick = { Navigator.navigate(Navigator.Screen.Auth) })
                        }
                    }
                    else -> {
                        items(state.items, key = CheckIn::id) { checkIn ->
                            TimelineCheckInCard(
                                checkIn = checkIn,
                                isOwn = state.owns(checkIn),
                                isPublicTimeline = state.timeline == CommunityTimeline.Public,
                                publishedAt = state.publishedAt[checkIn.id],
                                canAct = !state.isMutating,
                                changingVisibility = state.changingVisibilityId == checkIn.id,
                                onEdit = { vm.edit(checkIn.id) },
                                onVisibility = { vm.toggleVisibility(checkIn.id) },
                                onHelpful = { vm.toggleHelpful(checkIn.id) },
                                onReport = { vm.openReport(checkIn.id) },
                                onPhotoClick = { urls, index ->
                                    preview = CheckInPhotoPreview(state.sessionGeneration, urls, index)
                                },
                            )
                        }
                        when {
                            state.error != null -> item(key = "error") {
                                Column {
                                    Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                                    TextButton(onClick = vm::retry) { Text(if (state.restartPagination) "Обновить ленту" else "Попробовать снова") }
                                }
                            }
                            state.items.isEmpty() && !state.isLoading -> item(key = "empty") {
                                Text(if (state.timeline == CommunityTimeline.Public) "Пока нет опубликованных чек-инов."
                                    else "Пока нет чек-инов. Добавьте первый на странице кофейни.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (state.isLoadingMore) item(key = "loading-more") { CoffeePeekLoader() }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineCheckInCard(
    checkIn: CheckIn,
    isOwn: Boolean,
    isPublicTimeline: Boolean,
    publishedAt: String?,
    canAct: Boolean,
    changingVisibility: Boolean,
    onEdit: () -> Unit,
    onVisibility: () -> Unit,
    onHelpful: () -> Unit,
    onReport: () -> Unit,
    onPhotoClick: (List<String>, Int) -> Unit,
) {
    ReviewDisplayCard(
        review = Review(
            id = checkIn.id, username = checkIn.username.ifBlank { if (isOwn) "Вы" else "Пользователь" }, header = "", comment = checkIn.note,
            rating = checkIn.rating ?: ReviewRating(0, 0, 0), createdAt = checkIn.visitedAt.ifBlank { checkIn.createdAt },
            photoUrls = checkIn.photoUrls, drinkSlug = checkIn.drinkSlug, customDrinkName = checkIn.customDrinkName,
            drinkNameRu = checkIn.drinkNameRu, drinkNameEn = checkIn.drinkNameEn,
            helpfulCount = checkIn.helpfulCount, isHelpfulByCurrentUser = checkIn.isHelpfulByCurrentUser,
        ),
        fullVersion = true,
        dateLabel = publishedAt?.let { "Опубликован ${utcIsoToLocalDate(it)}" },
        onEditClick = onEdit.takeIf { canAct && isOwn },
        onReportClick = onReport.takeIf { canAct && isPublicTimeline && !isOwn },
        showHelpfulButton = isPublicTimeline && !isOwn,
        onHelpfulClick = onHelpful.takeIf { canAct },
        onPhotoClick = onPhotoClick,
        footer = {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
                    Text(
                        checkIn.shopName.ifBlank { "Кофейня" },
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.then(if (checkIn.shopId.isNotBlank()) Modifier.clickable {
                            Navigator.navigate(Navigator.Screen.ShopDetail(checkIn.shopId))
                        } else Modifier),
                    )
                    if (!isPublicTimeline) Text(checkIn.publicationLabel(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (isPublicTimeline && checkIn.visitedAt.isNotBlank()) Text("Визит ${utcIsoToLocalDate(checkIn.visitedAt)}",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if ((!isPublicTimeline || isOwn) && checkIn.helpfulCount > 0) Text(
                        "Полезно · " + checkIn.helpfulCount,
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isOwn) TextButton(onClick = onVisibility, enabled = canAct) {
                    Text(when {
                        changingVisibility -> "Сохраняем…"
                        checkIn.visibility == CheckInVisibility.Private -> "Опубликовать"
                        else -> "Скрыть"
                    })
                }
            }
            if (checkIn.moderationState == CheckInModerationState.Rejected) checkIn.rejectionReason?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        },
    )
}

internal fun CheckIn.publicationLabel(): String = if (visibility == CheckInVisibility.Private) "Приватный" else when (moderationState) {
    CheckInModerationState.Pending -> "На проверке"
    CheckInModerationState.Approved -> "Опубликован"
    CheckInModerationState.Rejected -> "Отклонён"
    CheckInModerationState.NotSubmitted -> "Публичный"
    CheckInModerationState.Unknown -> "Статус проверки неизвестен"
}

private data class CheckInPhotoPreview(val generation: Long, val urls: List<String>, val index: Int)
