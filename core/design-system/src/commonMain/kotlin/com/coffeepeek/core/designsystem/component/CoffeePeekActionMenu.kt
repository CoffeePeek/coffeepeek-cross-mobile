package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

data class CoffeePeekMenuAction(
    val label: String,
    val icon: ImageVector,
    val destructive: Boolean = false,
    /** The glyph a native menu draws for this row; none draws the row without one. */
    val nativeIcon: CoffeePeekMenuIcon? = null,
    val onClick: () -> Unit,
)

@Composable
fun CoffeePeekActionMenu(
    actions: List<CoffeePeekMenuAction>,
    modifier: Modifier = Modifier,
    /** The accessibility label of the trigger when a native menu replaces the popup. */
    menuDescription: String = "",
    content: @Composable (open: () -> Unit) -> Unit,
) {
    val native = LocalCoffeePeekNativeControlsPresenter.current
    if (native != null && native.Menu(
            actions.mapIndexed { index, action ->
                CoffeePeekNativeMenuItem(index.toString(), action.label, action.nativeIcon, action.destructive, action.onClick)
            },
            menuDescription, modifier, content,
        )
    ) return
    var expanded by remember { mutableStateOf(false) }
    val margin = with(LocalDensity.current) { CoffeePeekDimens.spacing2.roundToPx() }
    val position = remember(margin) { object : PopupPositionProvider {
        override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize,
            layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset =
            coffeePeekActionMenuPosition(anchorBounds, windowSize, layoutDirection, popupContentSize, margin)
    } }
    Box(modifier) {
        content { expanded = true }
        if (expanded) Popup(position, onDismissRequest = { expanded = false }, properties = PopupProperties(focusable = true)) {
            CoffeePeekActionMenuContent(actions) {
                expanded = false
                it.onClick()
            }
        }
    }
}

internal fun coffeePeekActionMenuPosition(
    anchor: IntRect, window: IntSize, direction: LayoutDirection, menu: IntSize, margin: Int,
): IntOffset {
    val x = if (direction == LayoutDirection.Ltr) anchor.right - menu.width else anchor.left
    val below = anchor.bottom + margin
    val y = if (below + menu.height <= window.height - margin) below else anchor.top - menu.height - margin
    return IntOffset(
        x.coerceIn(margin, (window.width - menu.width - margin).coerceAtLeast(margin)),
        y.coerceIn(margin, (window.height - menu.height - margin).coerceAtLeast(margin)),
    )
}

@Composable
private fun CoffeePeekActionMenuContent(actions: List<CoffeePeekMenuAction>, onAction: (CoffeePeekMenuAction) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(CoffeePeekDimens.cardRadius)
    Column(Modifier.widthIn(min = 224.dp, max = 304.dp).heightIn(max = 360.dp)
        .shadow(8.dp, shape).clip(shape).background(colors.surface).border(1.dp, colors.outlineVariant, shape)
        .verticalScroll(rememberScrollState())) {
        actions.forEachIndexed { index, action ->
            if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outlineVariant))
            CoffeePeekActionMenuRow(action) { onAction(action) }
        }
    }
}

@Composable
private fun CoffeePeekActionMenuRow(action: CoffeePeekMenuAction, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val colors = MaterialTheme.colorScheme
    val tint = if (action.destructive) colors.error else colors.onSurface
    Row(Modifier.fillMaxWidth().heightIn(min = CoffeePeekDimens.controlHeight)
        .graphicsLayer { alpha = if (pressed) .55f else 1f }
        .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
        .padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        Image(action.icon, null, Modifier.size(CoffeePeekDimens.settingsIconSize), colorFilter = ColorFilter.tint(tint))
        BasicText(action.label, style = MaterialTheme.typography.bodyLarge.copy(color = tint))
    }
}

@Preview
@Composable
private fun CoffeePeekActionMenuPreview() = CoffeePeekTheme {
    CoffeePeekActionMenuContent(listOf(
        CoffeePeekMenuAction("Поделиться профилем", CoffeePeekIcons.Share) {},
        CoffeePeekMenuAction("Заблокировать автора", CoffeePeekIcons.Block, destructive = true) {},
    )) {}
}
