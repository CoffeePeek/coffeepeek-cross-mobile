package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Platform action control; preserves the caller's foreground slot and behavior. */
@Composable
fun CoffeePeekTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(28.dp),
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(
        containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    CoffeePeekGlassButtonSurface(onClick, modifier, enabled, colors, contentPadding,
        interactionSource, CoffeePeekButtonStyle.Tonal, shape, border, content)
}

@Composable
private fun CoffeePeekTonalButtonPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column {
        CoffeePeekTonalButton({}) { Text("Continue") }
        CoffeePeekTonalButton({}, enabled = false) { Text("Continue") }
    }
}

@Preview @Composable private fun CoffeePeekTonalButtonLightPreview() = CoffeePeekTonalButtonPreview(false)
@Preview @Composable private fun CoffeePeekTonalButtonDarkPreview() = CoffeePeekTonalButtonPreview(true)
