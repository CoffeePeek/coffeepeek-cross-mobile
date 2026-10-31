package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A glass filter pill with a tinted selected state and checkbox accessibility semantics.
 * The 44.dp visual sits inside a 48.dp touch target; both can grow with the label.
 */
@Composable
fun CoffeePeekFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    CoffeePeekGlassFilterChip(label, selected, onClick, modifier, leadingIcon)
}

@Composable
private fun CoffeePeekGlassFilterChip(
    label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier, leadingIcon: ImageVector?,
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val colors = MaterialTheme.colorScheme
    val foreground = if (selected) colors.onPrimary else colors.onSurface
    Box(modifier.heightIn(min = CoffeePeekDimens.glassControlSize)
        .coffeePeekGlassPress(pressed, true)
        .toggleable(selected, interactions, indication = null, role = Role.Checkbox, onValueChange = { onClick() }),
        contentAlignment = Alignment.Center) {
        Box(Modifier.padding(vertical = 2.dp).heightIn(min = CoffeePeekDimens.controlHeight),
            contentAlignment = Alignment.Center) {
            CoffeePeekNativeControlBackground(Modifier.matchParentSize(), prominent = selected,
                tint = if (selected) colors.primary else null, pressed = pressed)
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (leadingIcon != null) Icon(leadingIcon, null, Modifier.size(18.dp), tint = foreground)
                Text(label, color = foreground, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }
}

@Composable
private fun FilterChipPreviewContent(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    androidx.compose.material3.Surface {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CoffeePeekFilterChip("Открыто", selected = false, onClick = {}, leadingIcon = CoffeePeekIcons.Time)
            CoffeePeekFilterChip("Новые", selected = true, onClick = {}, leadingIcon = CoffeePeekIcons.Sparkle)
            CoffeePeekFilterChip("Декаф", selected = false, onClick = {})
        }
    }
}

@Preview @Composable private fun CoffeePeekFilterChipLightPreview() = FilterChipPreviewContent(darkTheme = false)
@Preview @Composable private fun CoffeePeekFilterChipDarkPreview() = FilterChipPreviewContent(darkTheme = true)
