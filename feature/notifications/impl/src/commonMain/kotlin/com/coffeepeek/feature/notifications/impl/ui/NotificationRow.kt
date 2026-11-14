package com.coffeepeek.feature.notifications.impl.ui

import com.coffeepeek.feature.notifications.impl.resources.notifications_open
import com.coffeepeek.feature.notifications.impl.resources.Res
import com.coffeepeek.feature.notifications.impl.resources.notifications_mark_read
import com.coffeepeek.feature.notifications.impl.resources.notifications_read
import com.coffeepeek.feature.notifications.impl.resources.notifications_unread
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadge
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadgePalette
import com.coffeepeek.core.designsystem.component.CoffeePeekRowSeparator
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.notifications.api.NotificationItem
import com.coffeepeek.feature.notifications.api.NotificationKind
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun NotificationRow(item: NotificationItem, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val (icon, palette) = notificationAppearance(item)
    val readDescription = stringResource(if (item.read) Res.string.notifications_read else Res.string.notifications_unread)
    val clickAction = stringResource(if (item.canOpen) Res.string.notifications_open else Res.string.notifications_mark_read)
    Row(Modifier.fillMaxWidth().semantics { stateDescription = readDescription }
        .clickable(enabled = enabled && (item.canOpen || !item.read), role = Role.Button, onClickLabel = clickAction, onClick = onClick)
        .padding(vertical = CoffeePeekDimens.spacing3),
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        CoffeePeekIconBadge(icon, palette)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface,
                fontWeight = if (item.read) FontWeight.Medium else FontWeight.SemiBold)
            Text(localDateTime(item.createdAtUtc), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            if (item.body.isNotBlank()) {
                Text(item.body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        Box(Modifier.padding(top = CoffeePeekDimens.spacing2).size(6.dp)) {
            if (!item.read) Box(Modifier.matchParentSize().background(colors.primary, CircleShape))
        }
    }
}

private fun notificationAppearance(item: NotificationItem) = when {
    item.kind == NotificationKind.ShopRejected || item.kind == NotificationKind.RoasterRejected || item.status == "Rejected" ->
        CoffeePeekIcons.Close to CoffeePeekIconBadgePalette.Rose
    item.kind == NotificationKind.ShopApproved || item.kind == NotificationKind.RoasterApproved ->
        CoffeePeekIcons.CheckCircle to CoffeePeekIconBadgePalette.Mint
    item.kind == NotificationKind.NewShop -> CoffeePeekIcons.Location to CoffeePeekIconBadgePalette.Gold
    item.kind == NotificationKind.CheckInModeration -> CoffeePeekIcons.Review to CoffeePeekIconBadgePalette.Sky
    item.kind == NotificationKind.CorrectionModeration -> CoffeePeekIcons.Edit to CoffeePeekIconBadgePalette.Cyan
    item.kind == NotificationKind.Inactivity -> CoffeePeekIcons.Coffee to CoffeePeekIconBadgePalette.Gold
    else -> CoffeePeekIcons.Sparkle to CoffeePeekIconBadgePalette.Violet
}

@Composable
private fun NotificationRowPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface {
        Column(Modifier.padding(horizontal = CoffeePeekDimens.spacing5)) {
            listOf(false, true).forEachIndexed { index, read ->
                NotificationRow(NotificationItem("preview", NotificationKind.NewShop, "Новая кофейня на карте",
                    "На карте появилась новая кофейня. Откройте приложение, чтобы узнать подробности и заглянуть за чашкой кофе!",
                    "2026-10-21T08:00:00Z", read, canOpen = true), true, {})
                if (index == 0) CoffeePeekRowSeparator(Modifier)
            }
        }
    }
}

@Preview(widthDp = 360) @Composable private fun NotificationRowLightPreview() = NotificationRowPreview(false)
@Preview(widthDp = 360) @Composable private fun NotificationRowDarkPreview() = NotificationRowPreview(true)
