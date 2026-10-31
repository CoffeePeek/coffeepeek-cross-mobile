package com.coffeepeek.core.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
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
import com.coffeepeek.core.designsystem.modifier.coffeePeekLiquidGlass
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

data class CoffeePeekMenuAction(
    val label: String,
    val icon: ImageVector,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
fun CoffeePeekActionMenu(
    actions: List<CoffeePeekMenuAction>,
    modifier: Modifier = Modifier,
    content: @Composable (open: () -> Unit) -> Unit,
) {
    if (CoffeePeekNativeMenu(actions, modifier, content)) return
    val expanded = remember { MutableTransitionState(false) }
    val margin = with(LocalDensity.current) { CoffeePeekDimens.spacing2.roundToPx() }
    val origin = remember { MenuOrigin() }
    val position = remember(margin) { object : PopupPositionProvider {
        override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize,
            layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset =
            coffeePeekActionMenuPosition(anchorBounds, windowSize, layoutDirection, popupContentSize, margin).also {
                origin.value = TransformOrigin(if (layoutDirection == LayoutDirection.Ltr) 1f else 0f, if (it.y < anchorBounds.top) 1f else 0f)
            }
    } }
    Box(modifier) {
        content { expanded.targetState = true }
        if (expanded.currentState || expanded.targetState) Popup(position, onDismissRequest = { expanded.targetState = false }, properties = PopupProperties(focusable = true)) {
            CoffeePeekActionMenuContent(actions, Modifier.menuAppearance(expanded, origin)) {
                if (expanded.targetState) {
                    expanded.targetState = false
                    it.onClick()
                }
            }
        }
    }
}

/** The menu grows from the corner of its anchor; the position provider sets the corner, which is only known once the menu is measured. */
private class MenuOrigin(var value: TransformOrigin = TransformOrigin(1f, 0f))

@Composable
private fun Modifier.menuAppearance(expanded: MutableTransitionState<Boolean>, origin: MenuOrigin): Modifier {
    val transition = rememberTransition(expanded, label = "action-menu")
    val progress by transition.animateFloat(
        transitionSpec = { tween(if (targetState) 160 else 110, easing = FastOutSlowInEasing) }, label = "action-menu-progress",
    ) { if (it) 1f else 0f }
    return graphicsLayer {
        transformOrigin = origin.value
        alpha = progress
        scaleX = 0.9f + 0.1f * progress
        scaleY = scaleX
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
private fun CoffeePeekActionMenuContent(
    actions: List<CoffeePeekMenuAction>,
    modifier: Modifier = Modifier,
    onAction: (CoffeePeekMenuAction) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(CoffeePeekDimens.cardRadius)
    Column(modifier.widthIn(min = 224.dp, max = 304.dp).heightIn(max = 360.dp)
        .coffeePeekLiquidGlass(shape, hazeState = LocalCoffeePeekMenuBackdrop.current, shadowElevation = 8.dp)
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

@Composable
private fun CoffeePeekActionMenuPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CoffeePeekActionMenuContent(listOf(
        CoffeePeekMenuAction("Поделиться профилем", CoffeePeekIcons.Share) {},
        CoffeePeekMenuAction("Заблокировать автора", CoffeePeekIcons.Block, destructive = true) {},
    )) {}
}

@Preview @Composable private fun CoffeePeekActionMenuLightPreview() = CoffeePeekActionMenuPreview(false)
@Preview @Composable private fun CoffeePeekActionMenuDarkPreview() = CoffeePeekActionMenuPreview(true)
