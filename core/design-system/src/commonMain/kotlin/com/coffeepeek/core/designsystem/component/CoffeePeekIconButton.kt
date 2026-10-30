package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIcon
import com.coffeepeek.core.designsystem.modifier.LocalCoffeePeekNativeGlassPresenter
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.modifier.LocalCoffeePeekGlassHazeState
import dev.chrisbanes.haze.HazeState
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CoffeePeekIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = CircleShape,
    prominent: Boolean = false,
    tint: Color? = null,
    nativeIcon: CoffeePeekGlassIcon? = null,
    contentDescription: String? = null,
    hazeState: HazeState? = LocalCoffeePeekGlassHazeState.current,
    content: @Composable () -> Unit,
) {
    if (nativeIcon != null && contentDescription != null && LocalCoffeePeekNativeGlassPresenter.current
            ?.IconButton(nativeIcon, onClick, contentDescription, modifier, enabled) == true) return
    val accessibleModifier = if (contentDescription != null) modifier.semantics {
        this.contentDescription = contentDescription
    } else modifier
    val interactions = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val background = tint ?: if (enabled) colors.containerColor else colors.disabledContainerColor
    Box(
        accessibleModifier.sizeIn(minWidth = CoffeePeekDimens.glassControlSize, minHeight = CoffeePeekDimens.glassControlSize)
            .coffeePeekGlassPress(pressed, enabled)
            .clickable(interactions, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CoffeePeekNativeControlBackground(Modifier.matchParentSize(), prominent,
            background.takeIf { it.isSpecified && it.alpha > 0f }, enabled, pressed, shape, hazeState)
        CompositionLocalProvider(LocalContentColor provides if (enabled) colors.contentColor else colors.disabledContentColor) {
            content()
        }
    }
}

@Composable
private fun CoffeePeekIconButtonPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CoffeePeekIconButton({}) { Icon(CoffeePeekIcons.Share, "Share", Modifier.size(24.dp)) }
}

@Preview @Composable private fun CoffeePeekIconButtonLightPreview() = CoffeePeekIconButtonPreview(false)
@Preview @Composable private fun CoffeePeekIconButtonDarkPreview() = CoffeePeekIconButtonPreview(true)
