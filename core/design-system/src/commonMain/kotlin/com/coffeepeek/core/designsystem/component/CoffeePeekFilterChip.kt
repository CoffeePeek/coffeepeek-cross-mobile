package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A pill that switches a filter on and off: dark with a light label when selected, outlined otherwise. A checkbox for accessibility,
 * the icon (when there is one) is always in the brand color.
 */
@Composable
fun CoffeePeekFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(percent = 50)
    val container = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface
    val content = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
    val border = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    Row(
        modifier = modifier.height(CoffeePeekDimens.buttonHeight).clip(shape).background(container).border(1.dp, border, shape)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() }).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leadingIcon != null) Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = content, maxLines = 1)
    }
}

@Composable
private fun FilterChipPreviewContent(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    androidx.compose.material3.Surface {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CoffeePeekFilterChip("Открыто", selected = false, onClick = {}, leadingIcon = CoffeePeekIcons.Time)
            CoffeePeekFilterChip("Новые", selected = true, onClick = {}, leadingIcon = CoffeePeekIcons.Sparkle)
            CoffeePeekFilterChip("Specialty", selected = false, onClick = {})
        }
    }
}

@Preview @Composable private fun CoffeePeekFilterChipLightPreview() = FilterChipPreviewContent(darkTheme = false)
@Preview @Composable private fun CoffeePeekFilterChipDarkPreview() = FilterChipPreviewContent(darkTheme = true)
