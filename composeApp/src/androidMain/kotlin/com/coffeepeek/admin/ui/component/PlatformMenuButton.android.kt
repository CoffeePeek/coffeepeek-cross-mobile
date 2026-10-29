package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.icons.CpIcons

@Composable
internal actual fun PlatformMenuButton(
    items: List<PlatformMenuItem>, contentDescription: String, modifier: Modifier, enabled: Boolean,
    valueDescription: String?, content: @Composable (onClick: () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(CpDimens.cardRadius)
    val margin = with(LocalDensity.current) { CpDimens.spacing2.roundToPx() }
    val position = remember(margin) { object : PopupPositionProvider {
        override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
            val x = if (layoutDirection == LayoutDirection.Ltr) anchorBounds.right - popupContentSize.width else anchorBounds.left
            val below = anchorBounds.bottom + margin
            val y = if (below + popupContentSize.height <= windowSize.height - margin) below else anchorBounds.top - popupContentSize.height - margin
            return IntOffset(x.coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)),
                y.coerceIn(margin, (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin)))
        }
    } }
    Box(modifier) {
        content { if (enabled) expanded = true }
        if (expanded && enabled) Popup(popupPositionProvider = position, onDismissRequest = { expanded = false }, properties = PopupProperties(focusable = true)) {
            Column(Modifier.widthIn(min = 224.dp, max = 304.dp).heightIn(max = 360.dp)
                .shadow(8.dp, shape).clip(shape).background(colors.surface).border(1.dp, colors.outlineVariant, shape)
                .verticalScroll(rememberScrollState())) {
                items.forEachIndexed { index, item ->
                    if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outlineVariant))
                    val interaction = remember(item.id) { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()
                    val tint = if (item.destructive) colors.error else colors.onSurface
                    Row(Modifier.fillMaxWidth().heightIn(min = CpDimens.controlHeight).graphicsLayer { alpha = if (!item.enabled) .4f else if (pressed) .55f else 1f }
                        .clickable(interactionSource = interaction, indication = null, enabled = item.enabled, role = Role.Button) { expanded = false; item.onClick() }
                        .semantics { item.selected?.let { selected = it } }
                        .padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing3), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                        val icon = when {
                            item.selected == true -> CpIcons.Check
                            item.selected == false -> null
                            else -> when (item.icon) {
                                PlatformMenuIcon.Edit -> CpIcons.Edit
                                PlatformMenuIcon.Hide, PlatformMenuIcon.Block -> CpIcons.VisibilityOff
                                PlatformMenuIcon.Report -> CpIcons.Error
                                PlatformMenuIcon.NoteEdit -> CpIcons.NoteEdit
                                PlatformMenuIcon.Add -> CpIcons.Add
                                PlatformMenuIcon.CoffeeBean -> CpIcons.CoffeeBean
                                PlatformMenuIcon.Logout -> CpIcons.Logout
                                PlatformMenuIcon.Share -> CpIcons.Share
                                null -> null
                            }
                        }
                        if (icon != null) Image(icon, null, Modifier.size(CpDimens.settingsIconSize), colorFilter = ColorFilter.tint(tint))
                        else if (item.selected != null) Spacer(Modifier.size(CpDimens.settingsIconSize))
                        BasicText(item.title, style = MaterialTheme.typography.bodyLarge.copy(color = tint))
                    }
                }
            }
        }
    }
}
