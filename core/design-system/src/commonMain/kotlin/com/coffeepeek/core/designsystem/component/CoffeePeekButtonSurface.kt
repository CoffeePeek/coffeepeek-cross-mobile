package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

internal enum class CoffeePeekButtonStyle { Filled, Tonal, Outlined, Text }

/** Glass decoration is platform-owned; Compose owns slots, touch and accessibility. */
@Composable
internal fun CoffeePeekGlassButtonSurface(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    colors: ButtonColors,
    contentPadding: PaddingValues,
    interactionSource: MutableInteractionSource?,
    style: CoffeePeekButtonStyle,
    shape: Shape,
    border: BorderStroke?,
    content: @Composable RowScope.() -> Unit,
) {
    val interactions = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val foreground = if (enabled) colors.contentColor else colors.disabledContentColor
    val background = if (enabled) colors.containerColor else colors.disabledContainerColor
    val tint = background.takeIf { it.isSpecified && it.alpha > 0f }
    Box(
        modifier.defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
            .coffeePeekGlassPress(pressed, enabled)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(interactions, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true,
    ) {
        if (style != CoffeePeekButtonStyle.Text || tint != null) CoffeePeekNativeControlBackground(
            Modifier.matchParentSize(), prominent = style == CoffeePeekButtonStyle.Filled,
            tint = tint,
            enabled = enabled, pressed = pressed, shape = shape,
        )
        CompositionLocalProvider(LocalContentColor provides foreground) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                Row(Modifier.padding(contentPadding), Arrangement.Center, Alignment.CenterVertically, content = content)
            }
        }
    }
}
