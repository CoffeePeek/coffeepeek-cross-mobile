package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CoffeePeekFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    CoffeePeekGlassButtonSurface(onClick, modifier.sizeIn(minWidth = 56.dp, minHeight = 56.dp), true,
            ButtonDefaults.buttonColors(containerColor, contentColor), PaddingValues(16.dp),
            interactionSource, CoffeePeekButtonStyle.Filled, shape, null) { content() }
}

@Composable
private fun CoffeePeekFloatingActionButtonPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column {
        CoffeePeekFloatingActionButton({}) { Icon(CoffeePeekIcons.Add, "Add") }
    }
}

@Preview @Composable private fun CoffeePeekFloatingActionButtonLightPreview() = CoffeePeekFloatingActionButtonPreview(false)
@Preview @Composable private fun CoffeePeekFloatingActionButtonDarkPreview() = CoffeePeekFloatingActionButtonPreview(true)
