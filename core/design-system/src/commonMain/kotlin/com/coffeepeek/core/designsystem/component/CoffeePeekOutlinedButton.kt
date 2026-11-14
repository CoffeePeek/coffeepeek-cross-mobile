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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Platform action control; preserves the caller's foreground slot and behavior. */
@Composable
fun CoffeePeekOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(28.dp),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    CoffeePeekGlassButtonSurface(onClick, modifier, enabled, colors, contentPadding,
        interactionSource, CoffeePeekButtonStyle.Outlined, shape, border, content)
}

@Composable
private fun CoffeePeekOutlinedButtonPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column {
        CoffeePeekOutlinedButton({}) { Text("Continue") }
        CoffeePeekOutlinedButton({}, enabled = false) { Text("Continue") }
        val colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
        )
        CoffeePeekOutlinedButton({}, colors = colors) { Text("Custom tint") }
        CoffeePeekOutlinedButton({}, colors = colors, enabled = false) { Text("Disabled tint") }
    }
}

@Preview @Composable private fun CoffeePeekOutlinedButtonLightPreview() = CoffeePeekOutlinedButtonPreview(false)
@Preview @Composable private fun CoffeePeekOutlinedButtonDarkPreview() = CoffeePeekOutlinedButtonPreview(true)
