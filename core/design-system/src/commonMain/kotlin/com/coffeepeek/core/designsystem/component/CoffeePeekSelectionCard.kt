package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** One selectable option; the caller owns its selectable group and selection state. */
@Composable
fun CoffeePeekSelectionCard(
    title: String, description: String, icon: ImageVector,
    selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
    iconColors: CoffeePeekIconBadgeColors = CoffeePeekIconBadgePalette.Gold,
) {
    val shape = RoundedCornerShape(CoffeePeekDimens.cardRadius)
    Row(
        modifier.fillMaxWidth().clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                else MaterialTheme.colorScheme.outline, shape)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 72.dp).padding(CoffeePeekDimens.spacing3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
    ) {
        CoffeePeekIconBadge(icon, iconColors)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) Icon(CoffeePeekIcons.CheckCircle, null,
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun CoffeePeekSelectionCardPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CoffeePeekSelectionCard("CoffeePeek", "Selected option", CoffeePeekIcons.Coffee, true, {})
        CoffeePeekSelectionCard("CoffeePeek", "Another option", CoffeePeekIcons.Location, false, {})
    }
}

@Preview @Composable private fun CoffeePeekSelectionCardLightPreview() = CoffeePeekSelectionCardPreview(false)
@Preview @Composable private fun CoffeePeekSelectionCardDarkPreview() = CoffeePeekSelectionCardPreview(true)
