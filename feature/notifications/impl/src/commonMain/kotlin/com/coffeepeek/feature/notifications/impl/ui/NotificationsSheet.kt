package com.coffeepeek.feature.notifications.impl.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import com.coffeepeek.feature.notifications.impl.resources.Res
import com.coffeepeek.feature.notifications.impl.resources.notifications_close
import com.coffeepeek.feature.notifications.impl.resources.notifications_earlier
import com.coffeepeek.feature.notifications.impl.resources.notifications_inbox_load_failed
import com.coffeepeek.feature.notifications.impl.resources.notifications_load_more
import com.coffeepeek.feature.notifications.impl.resources.notifications_loading
import com.coffeepeek.feature.notifications.impl.resources.notifications_mark_read_failed
import com.coffeepeek.feature.notifications.impl.resources.notifications_read_all
import com.coffeepeek.feature.notifications.impl.resources.notifications_retry
import com.coffeepeek.feature.notifications.impl.resources.notifications_title
import com.coffeepeek.feature.notifications.impl.resources.notifications_today
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton as IconButton
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekRowSeparator
import com.coffeepeek.core.designsystem.component.CoffeePeekIcon
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.reducedMotionDuration
import com.coffeepeek.feature.notifications.api.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun NotificationsContent(
    state: NotificationInboxState,
    onClose: () -> Unit,
    onRead: (String) -> Unit,
    onReadAll: () -> Unit,
    onSeen: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onSignIn: () -> Unit,
    openingId: String? = null,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing5)) {
        NotificationsHeader(onClose)
        if (state.accountId != null && state.items.isNotEmpty()) {
            NotificationsReadAll(state, onReadAll, openingId == null, Modifier.align(Alignment.End))
        }
        Spacer(Modifier.height(CoffeePeekDimens.spacing4))
        NotificationsList(state, onClose, onRead, onSeen, onRefresh, onLoadMore, onSignIn,
            openingId, Modifier.weight(1f, fill = false))
    }
}

@Composable
private fun NotificationsReadAll(state: NotificationInboxState, onReadAll: () -> Unit,
    canInteract: Boolean, modifier: Modifier = Modifier) {
    TextButton(onClick = onReadAll, modifier = modifier,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        enabled = canInteract && state.unreadCount > 0 && !state.markingRead && !state.loading && !state.loadingMore) {
        CoffeePeekIcon(CoffeePeekIcons.Check, contentDescription = null, modifier = Modifier.size(CoffeePeekDimens.spacing4))
        Spacer(Modifier.width(CoffeePeekDimens.spacing2))
        Text(stringResource(Res.string.notifications_read_all))
    }
}

@Composable
private fun NotificationsList(state: NotificationInboxState, onClose: () -> Unit,
    onRead: (String) -> Unit, onSeen: (String) -> Unit, onRefresh: () -> Unit,
    onLoadMore: () -> Unit, onSignIn: () -> Unit, openingId: String?, modifier: Modifier) {
    val listState = rememberLazyListState()
    // The sheet supplies a maximum; short lists must keep their content height.
    LazyColumn(state = listState, modifier = modifier.navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = CoffeePeekDimens.spacing6)) {
        if (state.accountId == null) {
            item(key = "sign-in") {
                NotificationEmptyState(signedOut = true, onSignIn = {
                    onClose()
                    onSignIn()
                })
            }
        } else {
            notificationHistory(state, onRead, onSeen, openingId, listState)
            if (state.items.isEmpty() || state.error != null) {
                item(key = "status") { NotificationsStatus(state, onRefresh) }
            }
            if (state.nextCursor != null && state.error == null) item(key = "load-more") {
                TextButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    enabled = !state.loading && !state.loadingMore && !state.markingRead) {
                    Text(stringResource(if (state.loadingMore) Res.string.notifications_loading else Res.string.notifications_load_more))
                }
            }
        }
    }
}

@Composable
private fun NotificationsHeader(onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(Res.string.notifications_title), style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).semantics { heading() })
        IconButton(onClick = onClose) {
            CoffeePeekIcon(CoffeePeekIcons.Close, stringResource(Res.string.notifications_close))
        }
    }
}

private fun LazyListScope.notificationHistory(state: NotificationInboxState, onRead: (String) -> Unit,
    onSeen: (String) -> Unit, openingId: String?, listState: LazyListState) {
    state.items.groupBy { isToday(it.createdAtUtc) }.forEach { (isToday, entries) ->
        item(key = if (isToday) "today" else "earlier") {
            Text(stringResource(if (isToday) Res.string.notifications_today else Res.string.notifications_earlier),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = CoffeePeekDimens.spacing1).semantics { heading() })
        }
        itemsIndexed(entries, key = { _, item -> item.id }) { index, item ->
            Column {
                NotificationSeenEffect(listState, item, state.generation, onSeen)
                NotificationRow(item, enabled = openingId == null && !state.loading && !state.loadingMore,
                    onClick = { onRead(item.id) })
                if (index < entries.lastIndex) CoffeePeekRowSeparator()
            }
        }
    }
}

@Composable
private fun NotificationSeenEffect(listState: LazyListState, item: NotificationItem,
    generation: Long, onSeen: (String) -> Unit) {
    val currentOnSeen = rememberUpdatedState(onSeen)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(listState, item.id, item.read, generation, lifecycle) {
        if (!item.read) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snapshotFlow { notificationMostlyVisible(listState, item.id) }
                .distinctUntilChanged().collectLatest { visible ->
                    if (visible) {
                        delay(2_000)
                        currentOnSeen.value(item.id)
                    }
                }
        }
    }
}

private fun notificationMostlyVisible(listState: LazyListState, id: String): Boolean {
    val layout = listState.layoutInfo
    val row = layout.visibleItemsInfo.firstOrNull { it.key == id } ?: return false
    val visible = (minOf(row.offset + row.size, layout.viewportEndOffset) -
        maxOf(row.offset, layout.viewportStartOffset)).coerceAtLeast(0)
    val viewport = layout.viewportEndOffset - layout.viewportStartOffset
    return visible > 0 && visible >= minOf(row.size, viewport) / 2
}

@Composable
private fun NotificationsStatus(state: NotificationInboxState, onRefresh: () -> Unit) {
    val error = state.error
    Crossfade(targetState = when {
        state.loading && state.items.isEmpty() -> 0
        error != null -> 1
        else -> 2
    }, animationSpec = tween(reducedMotionDuration(220)), label = "notifications-status") { status ->
        when (status) {
            0 -> Box(
                Modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing6), contentAlignment = Alignment.Center,
            ) { CoffeePeekLoader(stringResource(Res.string.notifications_loading)) }
            1 -> error?.let { currentError ->
                Column {
                    Text(
                        stringResource(when (currentError) {
                            NotificationInboxError.LoadFailed -> Res.string.notifications_inbox_load_failed
                            NotificationInboxError.MarkReadFailed -> Res.string.notifications_mark_read_failed
                        }),
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(
                        onClick = onRefresh,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    ) { Text(stringResource(Res.string.notifications_retry)) }
                }
            }
            else -> NotificationEmptyState()
        }
    }
}

@Composable
private fun NotificationsPreview(dark: Boolean, count: Int) = CoffeePeekTheme(darkTheme = dark) {
    val state = NotificationInboxState(accountId = "preview", settingsLoaded = true, deviceAvailable = true,
        unreadCount = (count + 1) / 2, items = List(count) { index ->
            NotificationItem("preview-$index", NotificationKind.ShopApproved, "Кофейня добавлена",
                "Спасибо! Новая кофейня уже появилась на карте.", "2026-10-21T08:00:00Z", read = index % 2 == 1, canOpen = true)
        })
    Surface(Modifier.width(360.dp).heightIn(max = 640.dp)) {
        NotificationsContent(state, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview @Composable private fun NotificationsEmptyLightPreview() = NotificationsPreview(false, 0)
@Preview @Composable private fun NotificationsEmptyDarkPreview() = NotificationsPreview(true, 0)
@Preview @Composable private fun NotificationsListLightPreview() = NotificationsPreview(false, 1)
@Preview @Composable private fun NotificationsListDarkPreview() = NotificationsPreview(true, 1)
@Preview @Composable private fun NotificationsLongListLightPreview() = NotificationsPreview(false, 15)
@Preview @Composable private fun NotificationsLongListDarkPreview() = NotificationsPreview(true, 15)

@Composable
private fun NotificationsErrorsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column(Modifier.width(360.dp), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4)) {
        NotificationInboxError.entries.forEach { NotificationsStatus(NotificationInboxState(error = it), {}) }
    }
}

@Preview @Composable private fun NotificationsErrorsLightPreview() = NotificationsErrorsPreview(false)
@Preview @Composable private fun NotificationsErrorsDarkPreview() = NotificationsErrorsPreview(true)
