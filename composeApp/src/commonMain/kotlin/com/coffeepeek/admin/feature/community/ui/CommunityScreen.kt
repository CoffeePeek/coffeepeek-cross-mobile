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
    val shouldLoadMore by remember(state.items.size, state.hasMore, state.isLoading, state.isLoadingMore, state.error, state.isSaving, state.changingVisibilityId) {
        derivedStateOf {
            state.hasMore && !state.isLoading && !state.isLoadingMore && !state.isSaving && state.changingVisibilityId == null && state.error == null &&
                (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= state.items.size - 3
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) vm.loadMore() }

    preview?.takeIf { state.isLoggedIn == true && it.generation == state.sessionGeneration }?.let {
        FullScreenImageDialog(imageUrls = it.urls, initialIndex = it.index, onDismiss = { preview = null })
    }
    if (state.editing != null) key(state.sessionGeneration, state.editing?.source?.id) { EditCheckInSheet(state, vm) }

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
                        Text("Ваши чек-ины", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                when {
                    state.isLoggedIn == null || (state.isLoading && state.items.isEmpty()) -> item {
                        CoffeePeekLoader()
                    }
                    state.isLoggedIn == false -> item {
                        Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                            Text("Войдите, чтобы видеть и редактировать свои чек-ины.")
                            AppButton("Войти", onClick = { Navigator.navigate(Navigator.Screen.Auth) })
                        }
                    }
                    else -> {
                        items(state.items, key = CheckIn::id) { checkIn ->
                            PersonalCheckInCard(
                                checkIn = checkIn,
                                canEdit = !state.isSaving && state.changingVisibilityId == null,
                                changingVisibility = state.changingVisibilityId == checkIn.id,
                                onEdit = { vm.edit(checkIn.id) },
                                onVisibility = { vm.toggleVisibility(checkIn.id) },
                                onPhotoClick = { urls, index ->
                                    preview = CheckInPhotoPreview(state.sessionGeneration, urls, index)
                                },
                            )
                        }
                        when {
                            state.error != null -> item(key = "error") {
                                Column {
                                    Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                                    TextButton(onClick = vm::retry) { Text("Попробовать снова") }
                                }
                            }
                            state.items.isEmpty() && !state.isLoading -> item(key = "empty") {
                                Text("Пока нет чек-инов. Добавьте первый на странице кофейни.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun PersonalCheckInCard(
    checkIn: CheckIn,
    canEdit: Boolean,
    changingVisibility: Boolean,
    onEdit: () -> Unit,
    onVisibility: () -> Unit,
    onPhotoClick: (List<String>, Int) -> Unit,
) {
    ReviewDisplayCard(
        review = Review(
            id = checkIn.id, username = checkIn.username.ifBlank { "Вы" }, header = "", comment = checkIn.note,
            rating = checkIn.rating ?: ReviewRating(0, 0, 0), createdAt = checkIn.visitedAt.ifBlank { checkIn.createdAt },
            photoUrls = checkIn.photoUrls, drinkSlug = checkIn.drinkSlug, customDrinkName = checkIn.customDrinkName,
            drinkNameRu = checkIn.drinkNameRu, drinkNameEn = checkIn.drinkNameEn,
        ),
        fullVersion = true,
        onEditClick = onEdit.takeIf { canEdit },
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
                    Text(checkIn.publicationLabel(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (checkIn.helpfulCount > 0) Text(
                        "Полезно · " + checkIn.helpfulCount,
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onVisibility, enabled = canEdit) {
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
