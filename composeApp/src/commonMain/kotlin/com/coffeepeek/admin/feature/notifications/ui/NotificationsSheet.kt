package com.coffeepeek.admin.feature.notifications.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.currentUtcIsoDateTime
import com.coffeepeek.admin.utils.utcIsoToLocalDate
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.notifications.api.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun NotificationsContent(
    state: NotificationInboxState,
    onClose: () -> Unit,
    onPushChanged: (Boolean) -> Unit,
    onRead: (String) -> Unit,
    onReadAll: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing5)) {
        NotificationsHeader(onClose)
        Spacer(Modifier.height(CoffeePeekDimens.spacing4))
        // The sheet supplies a maximum; short lists must keep their content height.
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
            contentPadding = PaddingValues(bottom = CoffeePeekDimens.spacing6),
        ) {
            if (state.accountId == null) {
                item(key = "sign-in") {
                    NotificationEmptyState(signedOut = true, onSignIn = {
                        onClose()
                        Navigator.navigate(Navigator.Screen.Auth)
                    })
                }
            } else {
                item(key = "settings") { NotificationPushSettings(state, onPushChanged, onRefresh) }
                if (state.items.isNotEmpty()) item(key = "actions") { NotificationsReadActions(state, onReadAll) }
                notificationHistory(state, onRead)
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
}

@Composable
private fun NotificationsHeader(onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
            Text(stringResource(Res.string.notifications_title), style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
            Text(stringResource(Res.string.notifications_subtitle), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onClose) {
            Icon(CoffeePeekIcons.Close, stringResource(Res.string.notifications_close))
        }
    }
}

@Composable
private fun NotificationsReadActions(state: NotificationInboxState, onReadAll: () -> Unit) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.Center) {
        Text(if (state.unreadCount > 0) stringResource(Res.string.notifications_unread_count, state.unreadCount)
            else stringResource(Res.string.notifications_all_read), modifier = Modifier.padding(vertical = CoffeePeekDimens.spacing4),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onReadAll,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            enabled = state.unreadCount > 0 && !state.markingRead && !state.loading && !state.loadingMore) {
            Icon(CoffeePeekIcons.Check, null, Modifier.size(CoffeePeekDimens.spacing4))
            Spacer(Modifier.width(CoffeePeekDimens.spacing2))
            Text(stringResource(Res.string.notifications_read_all))
        }
    }
}

private fun LazyListScope.notificationHistory(state: NotificationInboxState, onRead: (String) -> Unit) {
    val today = utcIsoToLocalDate(currentUtcIsoDateTime())
    state.items.groupBy { utcIsoToLocalDate(it.createdAtUtc) == today }.forEach { (isToday, entries) ->
        item(key = if (isToday) "today" else "earlier") {
            Text(stringResource(if (isToday) Res.string.notifications_today else Res.string.notifications_earlier),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = CoffeePeekDimens.spacing1).semantics { heading() })
        }
        items(entries, key = NotificationItem::id) { item ->
            NotificationRow(item, enabled = !state.markingRead && !state.loading && !state.loadingMore,
                onClick = { onRead(item.id) })
        }
    }
}

@Composable
private fun NotificationsStatus(state: NotificationInboxState, onRefresh: () -> Unit) {
    val error = state.error
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
        unreadCount = count, items = List(count) { index ->
            NotificationItem("preview-$index", NotificationKind.ShopApproved, "Кофейня добавлена",
                "Спасибо! Новая кофейня уже появилась на карте.", "2026-10-21T08:00:00Z", false)
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
