package com.coffeepeek.feature.notifications.impl.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton as IconButton
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Density
import com.coffeepeek.feature.notifications.impl.R
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.notifications.api.NotificationFeature
import com.coffeepeek.feature.notifications.api.NotificationInboxState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview

@Composable
internal fun NotificationsButton(feature: NotificationFeature, onSignIn: () -> Unit,
    onOpen: suspend (String, String, Long) -> Boolean) {
    val state by feature.state.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }
    LaunchedEffect(state.generation) { open = false }
    NotificationsIconButton(state.unreadCount, onClick = {
        open = true
        feature.refresh()
    })
    if (open) NotificationSheet(feature, state, { open = false }, onSignIn, onOpen)
}

@Composable
private fun NotificationSheet(feature: NotificationFeature, state: NotificationInboxState,
    onClose: () -> Unit, onSignIn: () -> Unit, onOpen: suspend (String, String, Long) -> Boolean) {
    val scope = rememberCoroutineScope()
    var openingId by remember { mutableStateOf<String?>(null) }
    CoffeePeekSwipeDismissModalBottomSheet(
        onDismissRequest = onClose,
        dismissDescription = stringResource(R.string.notifications_close),
        maxSheetHeightFraction = .88f,
    ) {
        NotificationsContent(state, onClose, feature::setPushEnabled, { id ->
            if (openingId == null) openNotification(scope, feature, state, id, { openingId = it }, onClose, onOpen)
        }, feature::markAllRead, feature::refresh, feature::loadMore, onSignIn, openingId)
    }
}

private fun openNotification(scope: CoroutineScope, feature: NotificationFeature, expected: NotificationInboxState,
    id: String, onOpening: (String?) -> Unit, onClose: () -> Unit, onOpen: suspend (String, String, Long) -> Boolean) {
    val state = feature.state.value
    if (state.generation != expected.generation || state.accountId != expected.accountId) return
    val item = state.items.firstOrNull { it.id == id } ?: return
    val account = state.accountId ?: return
    if (!item.canOpen) {
        feature.markRead(id)
        return
    }
    onOpening(id)
    scope.launch {
        try {
            if (onOpen(id, account, state.generation)) onClose()
        } finally {
            onOpening(null)
        }
    }
}

@Composable
private fun NotificationsIconButton(unreadCount: Int, onClick: () -> Unit) {
    Box(Modifier.sizeIn(minWidth = CoffeePeekDimens.spacing12, minHeight = CoffeePeekDimens.spacing12)) {
        IconButton(onClick = onClick, modifier = Modifier.align(Alignment.Center).size(CoffeePeekDimens.buttonHeight),
            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Icon(CoffeePeekIcons.Notifications, contentDescription = if (unreadCount > 0)
                stringResource(R.string.notifications_button_unread, unreadCount) else stringResource(R.string.notifications_title))
        }
        // A sibling stays outside the circular IconButton's clip and inside the button's bounds.
        if (unreadCount > 0) Badge(modifier = Modifier.align(Alignment.TopEnd).clearAndSetSemantics { },
            containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
            Text(if (unreadCount > 99) stringResource(R.string.notifications_badge_max) else unreadCount.toString(), maxLines = 1)
        }
    }
}

@Composable
private fun NotificationsButtonPreview(dark: Boolean, fontScale: Float = 1f) = CoffeePeekTheme(darkTheme = dark) {
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
        Surface {
            Row(Modifier.padding(CoffeePeekDimens.spacing4), horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4)) {
                listOf(0, 1, 12, 128).forEach { NotificationsIconButton(it, {}) }
            }
        }
    }
}

@Preview @Composable private fun NotificationsButtonLightPreview() = NotificationsButtonPreview(false)
@Preview @Composable private fun NotificationsButtonDarkPreview() = NotificationsButtonPreview(true)
@Preview @Composable private fun NotificationsButtonLargeTextLightPreview() = NotificationsButtonPreview(false, 2f)
@Preview @Composable private fun NotificationsButtonLargeTextDarkPreview() = NotificationsButtonPreview(true, 2f)
