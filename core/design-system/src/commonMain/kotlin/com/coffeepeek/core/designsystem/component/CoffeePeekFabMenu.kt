package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Neutral action specification; no routes, destinations or business models. */
data class CoffeePeekFabMenuAction(
    val icon: ImageVector,
    val description: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)

/** Platform floating action group; callbacks and placement remain screen-owned. */
@Composable
fun CoffeePeekFabMenu(actions: List<CoffeePeekFabMenuAction>, modifier: Modifier = Modifier) {
    CoffeePeekGlassActionGroup(actions, modifier)
}

@Composable
private fun CoffeePeekGlassActionGroup(actions: List<CoffeePeekFabMenuAction>, modifier: Modifier) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    Box(modifier) {
        CoffeePeekNativeControlBackground(Modifier.matchParentSize(), pressed = pressed)
        Row(Modifier.padding(4.dp)) {
            actions.forEach { action ->
                CoffeePeekTextButton(onClick = action.onClick, enabled = action.enabled,
                    interactionSource = interactions, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                    Icon(action.icon, contentDescription = action.description)
                }
            }
        }
    }
}

@Composable
private fun CoffeePeekFabMenuPreviewContent(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    Surface {
        CoffeePeekFabMenu(listOf(CoffeePeekFabMenuAction(CoffeePeekIcons.Settings, "Settings", {}),
            CoffeePeekFabMenuAction(CoffeePeekIcons.Close, "Close", {}),
            CoffeePeekFabMenuAction(CoffeePeekIcons.Search, "Search unavailable", {}, enabled = false)))
    }
}

@Preview @Composable private fun CoffeePeekFabMenuLightPreview() = CoffeePeekFabMenuPreviewContent(false)
@Preview @Composable private fun CoffeePeekFabMenuDarkPreview() = CoffeePeekFabMenuPreviewContent(true)
