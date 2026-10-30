package com.coffeepeek.core.designsystem.component

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
fun CoffeePeekFilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    CoffeePeekIconButton(onClick, modifier, enabled, colors, interactionSource, shape,
        prominent = true, content = content)
}

@Composable
private fun CoffeePeekFilledIconButtonPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column {
        CoffeePeekFilledIconButton({}) { Icon(CoffeePeekIcons.Add, "Add") }
        CoffeePeekFilledIconButton({}, enabled = false) { Icon(CoffeePeekIcons.Add, "Add") }
    }
}

@Preview @Composable private fun CoffeePeekFilledIconButtonLightPreview() = CoffeePeekFilledIconButtonPreview(false)
@Preview @Composable private fun CoffeePeekFilledIconButtonDarkPreview() = CoffeePeekFilledIconButtonPreview(true)
