package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CoffeePeekOutlinedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    colors: IconButtonColors = IconButtonDefaults.outlinedIconButtonColors(),
    border: BorderStroke? = IconButtonDefaults.outlinedIconButtonBorder(enabled),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    CoffeePeekIconButton(onClick, modifier.then(if (border != null) Modifier.border(border, shape) else Modifier),
        enabled, colors, interactionSource, shape, content = content)
}

@Composable
private fun CoffeePeekOutlinedIconButtonPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column {
        CoffeePeekOutlinedIconButton({}) { Icon(CoffeePeekIcons.Add, "Add") }
        CoffeePeekOutlinedIconButton({}, enabled = false) { Icon(CoffeePeekIcons.Add, "Add") }
    }
}

@Preview @Composable private fun CoffeePeekOutlinedIconButtonLightPreview() = CoffeePeekOutlinedIconButtonPreview(false)
@Preview @Composable private fun CoffeePeekOutlinedIconButtonDarkPreview() = CoffeePeekOutlinedIconButtonPreview(true)
