package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens

class CoffeePeekMenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector? = null,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * The action menu of a "…" button: a card under the anchor with the rounded corners of the other cards, a hairline between rows and an icon before
 * each title. It replaces the plain Material dropdown, whose large rounded blob does not match the rest of the app.
 */
@Composable
fun CoffeePeekActionMenu(
    items: List<CoffeePeekMenuItem>,
    modifier: Modifier = Modifier,
    anchor: @Composable (open: () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(CoffeePeekDimens.cardRadius)
    val margin = with(LocalDensity.current) { CoffeePeekDimens.spacing2.roundToPx() }
    val position = remember(margin) {
        object : PopupPositionProvider {
            override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
                val x = if (layoutDirection == LayoutDirection.Ltr) anchorBounds.right - popupContentSize.width else anchorBounds.left
                val below = anchorBounds.bottom + margin
                val y = if (below + popupContentSize.height <= windowSize.height - margin) below else anchorBounds.top - popupContentSize.height - margin
                return IntOffset(
                    x.coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)),
                    y.coerceIn(margin, (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin)),
                )
            }
        }
    }
    Box(modifier) {
        anchor { expanded = true }
        if (expanded) Popup(popupPositionProvider = position, onDismissRequest = { expanded = false }, properties = PopupProperties(focusable = true)) {
            Column(
                Modifier.widthIn(min = 224.dp, max = 304.dp).heightIn(max = 360.dp)
                    .shadow(8.dp, shape).clip(shape).background(colors.surface).border(1.dp, colors.outlineVariant, shape)
                    .verticalScroll(rememberScrollState()),
            ) {
                items.forEachIndexed { index, item ->
                    if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outlineVariant))
                    val interaction = remember(item.id) { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()
                    val tint = if (item.destructive) colors.error else colors.onSurface
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = CoffeePeekDimens.controlHeight)
                            .graphicsLayer { alpha = if (!item.enabled) .4f else if (pressed) .55f else 1f }
                            .clickable(interactionSource = interaction, indication = null, enabled = item.enabled, role = Role.Button) { expanded = false; item.onClick() }
                            .padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing3),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
                    ) {
                        item.icon?.let { Icon(it, null, Modifier.size(CoffeePeekDimens.settingsIconSize), tint = tint) }
                        Text(item.title, style = MaterialTheme.typography.bodyLarge, color = tint)
                    }
                }
            }
        }
    }
}
