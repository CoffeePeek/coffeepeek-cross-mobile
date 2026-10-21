package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CoffeePeekSettingsSection(
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.padding(horizontal = CoffeePeekDimens.settingsPagePadding)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = TextUnit(0.08f, TextUnitType.Em),
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = CoffeePeekDimens.spacing1),
            )
        }
        Spacer(Modifier.height(CoffeePeekDimens.spacing2))
        Card(
            shape = RoundedCornerShape(CoffeePeekDimens.cardRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            content()
        }
    }
}

@Composable
fun CoffeePeekSettingsRow(
    icon: ImageVector,
    label: String,
    description: String? = null,
    onClick: () -> Unit,
    showArrow: Boolean = true,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconColors: CoffeePeekIconBadgeColors = CoffeePeekIconBadgePalette.Cyan,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(
                horizontal = CoffeePeekDimens.settingsRowPaddingH,
                vertical = CoffeePeekDimens.settingsRowPaddingV,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoffeePeekIconBadge(icon = icon, colors = iconColors)
        Spacer(Modifier.width(CoffeePeekDimens.spacing3))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(CoffeePeekDimens.spacing2))
        if (trailing != null) {
            trailing()
        } else if (showArrow) {
            Icon(
                imageVector = CoffeePeekIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(CoffeePeekDimens.settingsIconSize).scale(
                    scaleX = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f,
                    scaleY = 1f,
                ),
            )
        }
    }
}

@Composable
fun CoffeePeekSettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
        thickness = 1.dp,
    )
}

@Composable
private fun CoffeePeekSettingsListPreviewContent(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    Surface {
        CoffeePeekSettingsSection("Settings", description = "Presentation only") {
            CoffeePeekSettingsRow(CoffeePeekIcons.Settings, "Appearance", description = "System theme", onClick = {})
            CoffeePeekSettingsDivider()
            CoffeePeekSettingsRow(CoffeePeekIcons.Settings, "Unavailable", onClick = {}, enabled = false)
        }
    }
}

@Preview @Composable private fun CoffeePeekSettingsListLightPreview() = CoffeePeekSettingsListPreviewContent(false)
@Preview @Composable private fun CoffeePeekSettingsListDarkPreview() = CoffeePeekSettingsListPreviewContent(true)
