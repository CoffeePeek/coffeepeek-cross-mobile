package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Plain action with a 44dp target, pressed opacity and no Material ripple or button chrome. */
@Composable
fun CoffeePeekTextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, destructive: Boolean = false, emphasized: Boolean = false) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val colors = MaterialTheme.colorScheme
    Box(modifier.heightIn(min = CoffeePeekDimens.controlHeight)
        .graphicsLayer { alpha = if (!enabled) .4f else if (pressed) .55f else 1f }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = CoffeePeekDimens.spacing3, vertical = CoffeePeekDimens.spacing2), contentAlignment = Alignment.Center) {
        BasicText(text, style = MaterialTheme.typography.bodyLarge.copy(
            color = if (destructive) colors.error else colors.onSurface,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center))
    }
}

@Composable private fun TextActionPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column {
        CoffeePeekTextAction("Повторить", {})
        CoffeePeekTextAction("Удалить", {}, destructive = true)
        CoffeePeekTextAction("Недоступно", {}, enabled = false)
    }
}
@Preview @Composable private fun CoffeePeekTextActionLightPreview() = TextActionPreview(false)
@Preview @Composable private fun CoffeePeekTextActionDarkPreview() = TextActionPreview(true)
