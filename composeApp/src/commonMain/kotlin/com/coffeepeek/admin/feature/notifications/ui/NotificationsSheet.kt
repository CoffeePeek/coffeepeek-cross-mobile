package com.coffeepeek.admin.feature.notifications.ui

import androidx.compose.foundation.layout.*
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
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.currentUtcIsoDateTime
import com.coffeepeek.admin.utils.utcIsoToLocalDate
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekRowSeparator
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.notifications.api.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun NotificationsContent(
    state: NotificationInboxState,
    onClose: () -> Unit,
    // Retained for historical callers; notification preferences now belong to the Settings destination.
    @Suppress("UNUSED_PARAMETER") onPushChanged: (Boolean) -> Unit,
    onRead: (String) -> Unit,
    onSeen: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing5)) {
        NotificationsHeader(onClose)
        Spacer(Modifier.height(CoffeePeekDimens.spacing4))
        NotificationsList(state, onRead, onSeen, onRefresh, onLoadMore, onClose,
            Modifier.weight(1f, fill = false))
    }
}

@Composable
private fun NotificationsList(state: NotificationInboxState, onRead: (String) -> Unit,
    onSeen: (String) -> Unit, onRefresh: () -> Unit, onLoadMore: () -> Unit,
    onClose: () -> Unit, modifier: Modifier) {
    val listState = rememberLazyListState()
    // The sheet supplies a maximum; short lists must keep their content height.
    LazyColumn(state = listState, modifier = modifier,
        contentPadding = PaddingValues(bottom = CoffeePeekDimens.spacing6)) {
        if (state.accountId == null) {
            item(key = "sign-in") {
                NotificationEmptyState(signedOut = true, onSignIn = {
                    onClose()
                    Navigator.navigate(Navigator.Screen.Auth)
                })
            }
        } else {
            notificationHistory(state, onRead, onSeen, listState)
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
        val closeDescription = stringResource(Res.string.notifications_close)
        IconButton(onClick = onClose,
            nativeIcon = com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIcon.Close,
            contentDescription = closeDescription) {
            Icon(CoffeePeekIcons.Close, closeDescription)
        }
    }
}

private fun LazyListScope.notificationHistory(state: NotificationInboxState, onRead: (String) -> Unit,
    onSeen: (String) -> Unit, listState: LazyListState) {
    val today = utcIsoToLocalDate(currentUtcIsoDateTime())
    state.items.groupBy { utcIsoToLocalDate(it.createdAtUtc) == today }.forEach { (isToday, entries) ->
        item(key = if (isToday) "today" else "earlier") {
            Text(stringResource(if (isToday) Res.string.notifications_today else Res.string.notifications_earlier),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = CoffeePeekDimens.spacing1).semantics { heading() })
        }
        itemsIndexed(entries, key = { _, item -> item.id }) { index, item ->
            Column {
                NotificationSeenEffect(listState, item, state.generation, onSeen)
                NotificationRow(item, enabled = !state.loading && !state.loadingMore,
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
    val error = state.error?.let {
        stringResource(when (it) {
            NotificationInboxError.LoadFailed -> Res.string.legacy_notifications_inbox_load_failed
            NotificationInboxError.MarkReadFailed -> Res.string.legacy_notifications_mark_read_failed
        })
    }
    when {
        state.loading && state.items.isEmpty() -> Box(
            Modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing6), contentAlignment = Alignment.Center,
        ) { CoffeePeekLoader(stringResource(Res.string.notifications_loading)) }
        error != null -> Column {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onRefresh,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                Text(stringResource(Res.string.notifications_retry))
            }
        }
        state.items.isEmpty() -> NotificationEmptyState()
    }
}

@Composable
private fun NotificationsPreview(dark: Boolean, count: Int) = CoffeePeekTheme(darkTheme = dark) {
    val state = NotificationInboxState(accountId = "preview", settingsLoaded = true, deviceAvailable = true,
        unreadCount = (count + 1) / 2, items = List(count) { index ->
            NotificationItem("preview-$index", NotificationKind.ShopApproved, "Кофейня добавлена",
                "Спасибо! Новая кофейня уже появилась на карте.", "2026-10-21T08:00:00Z", read = index % 2 == 1)
        })
    Surface(Modifier.width(360.dp).heightIn(max = 640.dp)) {
        NotificationsContent(state, {}, {}, {}, {}, {}, {})
    }
}

@Preview @Composable private fun NotificationsEmptyLightPreview() = NotificationsPreview(false, 0)
@Preview @Composable private fun NotificationsEmptyDarkPreview() = NotificationsPreview(true, 0)
@Preview @Composable private fun NotificationsListLightPreview() = NotificationsPreview(false, 1)
@Preview @Composable private fun NotificationsListDarkPreview() = NotificationsPreview(true, 1)
@Preview @Composable private fun NotificationsLongListLightPreview() = NotificationsPreview(false, 15)
@Preview @Composable private fun NotificationsLongListDarkPreview() = NotificationsPreview(true, 15)
