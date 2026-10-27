package com.coffeepeek.feature.notifications.impl.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.feature.notifications.impl.R
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadge
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadgePalette
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.notifications.api.NotificationItem
import com.coffeepeek.feature.notifications.api.NotificationKind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview

@Composable
internal fun NotificationRow(item: NotificationItem, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val (icon, palette) = notificationAppearance(item)
    val readDescription = stringResource(if (item.read) R.string.notifications_read else R.string.notifications_unread)
    val clickAction = stringResource(if (item.canOpen) R.string.notifications_open else R.string.notifications_mark_read)
    Surface(shape = RoundedCornerShape(CoffeePeekDimens.radiusLg),
        color = if (item.read) colors.surfaceVariant.copy(alpha = .3f) else colors.primary.copy(alpha = .08f),
        border = BorderStroke(1.dp, if (item.read) colors.outlineVariant else colors.primary.copy(alpha = .25f))) {
        Row(Modifier.fillMaxWidth().semantics { stateDescription = readDescription }
            .clickable(enabled = enabled && (item.canOpen || !item.read), role = Role.Button, onClickLabel = clickAction, onClick = onClick)
            .padding(CoffeePeekDimens.spacing4), horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
            CoffeePeekIconBadge(icon, palette)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (item.read) FontWeight.Medium else FontWeight.SemiBold)
                Text(localDateTime(item.createdAtUtc), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Text(item.body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            if (!item.read) Box(Modifier.padding(top = CoffeePeekDimens.spacing2).size(6.dp).background(colors.primary, CircleShape))
            if (item.canOpen) Icon(CoffeePeekIcons.ChevronRight, null, Modifier.size(20.dp), tint = colors.onSurfaceVariant)
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
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        listOf(false, true).forEach { read ->
            NotificationRow(NotificationItem("preview", NotificationKind.NewShop, "Новая кофейня на карте",
                "На карте появилась новая кофейня. Загляните за чашкой кофе!", "2026-10-21T08:00:00Z", read, canOpen = true), true, {})
        }
    }
}

@Preview(widthDp = 360) @Composable private fun NotificationRowLightPreview() = NotificationRowPreview(false)
@Preview(widthDp = 360) @Composable private fun NotificationRowDarkPreview() = NotificationRowPreview(true)
