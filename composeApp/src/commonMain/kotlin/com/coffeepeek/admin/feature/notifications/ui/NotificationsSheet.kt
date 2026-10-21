package com.coffeepeek.admin.feature.notifications.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.*
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.utcIsoToLocalDate
import com.coffeepeek.admin.utils.utcIsoToLocalDateTime
import com.coffeepeek.admin.utils.currentUtcIsoDateTime
import com.coffeepeek.feature.notifications.api.*
import org.koin.compose.koinInject

@Composable
internal fun NotificationsButton(feature: NotificationFeature = koinInject()) {
    val state by feature.state.collectAsState()
    var open by remember { mutableStateOf(false) }
    LaunchedEffect(state.generation) { open = false }
    IconButton(onClick = { open = true; feature.refresh() },
        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.size(CpDimens.buttonHeight)) {
        BadgedBox(badge = {
            if (state.unreadCount > 0) Badge(containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary) {
                Text(if (state.unreadCount > 99) "99+" else state.unreadCount.toString())
            }
        }) {
            Icon(CpIcons.Notifications, contentDescription = if (state.unreadCount > 0)
                "Уведомления: ${state.unreadCount} непрочитанных" else "Уведомления")
        }
    }
    if (open) SwipeDismissModalBottomSheet(onDismissRequest = { open = false }, maxSheetHeightFraction = .88f) {
        NotificationsContent(state, { open = false }, feature::setPushEnabled, feature::markRead,
            feature::markAllRead, feature::refresh, feature::loadMore)
    }
}

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
    val colors = MaterialTheme.colorScheme
    val accent = if (colors.surface.luminance() < .5f) CpColor.AccentTextDark else CpColor.AccentTextLight
    val today = utcIsoToLocalDate(currentUtcIsoDateTime())
    Column(Modifier.fillMaxSize().padding(horizontal = CpDimens.spacing5)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(top = CpDimens.spacing2)) {
                Text("Уведомления", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                Text("Всё важное о вашем кофе", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            IconButton(onClick = onClose) { Icon(CpIcons.Close, "Закрыть уведомления") }
        }
        Spacer(Modifier.height(CpDimens.spacing4))
        if (state.accountId == null) {
            CpEmptyState("Войдите в CoffeePeek", "Здесь будут решения модерации, новые кофейни и новости.",
                CpIcons.Notifications, actionLabel = "Войти", onAction = {
                    onClose(); Navigator.navigate(Navigator.Screen.Auth)
                })
            return@Column
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
            contentPadding = PaddingValues(bottom = CpDimens.spacing6)) {
            item(key = "settings") {
                val canToggle = state.settingsLoaded && state.deviceAvailable && !state.savingSettings
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(CpDimens.radiusLg)).background(colors.surfaceVariant.copy(alpha = .6f))
                    .toggleable(value = state.pushEnabled && state.permissionGranted, enabled = canToggle, role = Role.Switch, onValueChange = onPushChanged)
                    .padding(CpDimens.spacing4), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                    Column(Modifier.weight(1f)) {
                        Text("Push-уведомления", style = MaterialTheme.typography.titleSmall)
                        Text(when {
                            !state.deviceAvailable -> "Push недоступны на этом устройстве"
                            state.savingSettings -> "Сохраняем…"
                            !state.permissionGranted -> "Для включения нужно разрешение системы"
                            else -> "Решения, новые кофейни и новости"
                        }, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    PlatformSwitch(state.pushEnabled && state.permissionGranted, null, canToggle, Modifier.clearAndSetSemantics { })
                }
                Text("История доступна и с выключенными push", style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant, modifier = Modifier.padding(top = CpDimens.spacing2))
                state.settingsError?.let {
                    Text(it, color = colors.error, style = MaterialTheme.typography.bodySmall)
                    if (!state.settingsLoaded) TextButton(onClick = onRefresh) { Text("Повторить загрузку настроек") }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (state.unreadCount > 0) "Непрочитанных: ${state.unreadCount}" else "Всё прочитано",
                        Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    TextButton(onClick = onReadAll,
                        enabled = state.unreadCount > 0 && state.items.isNotEmpty() && !state.markingRead && !state.loading && !state.loadingMore,
                        colors = ButtonDefaults.textButtonColors(contentColor = accent)) { Text("Прочитать все") }
                }
            }
            if (state.loading && state.items.isEmpty()) item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CoffeePeekLoader() } }
            if (!state.loading && state.error == null && state.items.isEmpty()) item {
                CpEmptyState("Пока тихо", "Новые уведомления появятся здесь. Загляните за чашкой кофе чуть позже.", CpIcons.Coffee)
            }
            state.items.groupBy { utcIsoToLocalDate(it.createdAtUtc) == today }.forEach { (isToday, entries) ->
                item(key = if (isToday) "today" else "earlier") {
                    Text(if (isToday) "СЕГОДНЯ" else "РАНЕЕ", style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant, modifier = Modifier.padding(top = CpDimens.spacing3, bottom = CpDimens.spacing1).semantics { heading() })
                }
                items(entries, key = NotificationItem::id) { item ->
                    NotificationRow(item, enabled = !state.markingRead && !state.loading && !state.loadingMore,
                        onClick = { onRead(item.id) })
                }
            }
            state.error?.let { error -> item {
                Text(error, color = colors.error, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onRefresh) { Text("Повторить") }
            } }
            if (state.nextCursor != null && state.error == null) item {
                TextButton(onClick = onLoadMore, enabled = !state.loading && !state.loadingMore && !state.markingRead,
                    modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.textButtonColors(contentColor = accent)) {
                    Text(if (state.loadingMore) "Загружаем…" else "Показать ещё")
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: NotificationItem, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val rejected = item.kind == NotificationKind.ShopRejected || item.kind == NotificationKind.RoasterRejected || item.status == "Rejected"
    val (icon, palette) = when {
        rejected -> CpIcons.Close to SettingsIconPalette.Rose
        item.kind == NotificationKind.ShopApproved || item.kind == NotificationKind.RoasterApproved -> CpIcons.CheckCircle to SettingsIconPalette.Mint
        item.kind == NotificationKind.NewShop -> CpIcons.Location to SettingsIconPalette.Gold
        item.kind == NotificationKind.CheckInModeration -> CpIcons.Review to SettingsIconPalette.Sky
        item.kind == NotificationKind.CorrectionModeration -> CpIcons.Edit to SettingsIconPalette.Cyan
        item.kind == NotificationKind.Inactivity -> CpIcons.Coffee to SettingsIconPalette.Gold
        else -> CpIcons.Sparkle to SettingsIconPalette.Violet
    }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(CpDimens.radiusLg))
        .semantics { stateDescription = if (item.read) "Прочитано" else "Не прочитано" }
        .background(if (item.read) colors.surface else colors.primary.copy(alpha = .10f))
        .clickable(enabled = enabled && !item.read, role = Role.Button, onClickLabel = "Отметить прочитанным", onClick = onClick)
        .padding(CpDimens.spacing4), horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
        SettingsIconBadge(icon, palette)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = if (item.read) FontWeight.Medium else FontWeight.SemiBold)
            Text(item.body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
            Text(utcIsoToLocalDateTime(item.createdAtUtc), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        if (!item.read) Box(Modifier.padding(top = CpDimens.spacing2).size(6.dp).background(colors.primary, CircleShape))
    }
}
