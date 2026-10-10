package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CpColor
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.icons.CpIcons
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Neutral action specification; no routes, destinations or business models. */
data class FabMenuAction(
    val icon: ImageVector,
    val description: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)

/** Joined floating actions. Placement and visibility belong to the screen. */
@Composable
fun FabMenu(actions: List<FabMenuAction>, modifier: Modifier = Modifier) {
    Row(modifier) {
        actions.forEachIndexed { index, action ->
            Surface(
                onClick = action.onClick,
                enabled = action.enabled,
                color = CpColor.Success,
                contentColor = CpColor.LightSurface,
                shape = RoundedCornerShape(
                    topStart = if (index == 0) 18.dp else CpDimens.radiusSm,
                    bottomStart = if (index == 0) 18.dp else CpDimens.radiusSm,
                    topEnd = if (index == actions.lastIndex) 18.dp else CpDimens.radiusSm,
                    bottomEnd = if (index == actions.lastIndex) 18.dp else CpDimens.radiusSm,
                ),
                modifier = Modifier.padding(1.dp).sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(action.icon, contentDescription = action.description)
                }
            }
        }
    }
}

@Composable
private fun FabMenuPreviewContent(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    Surface {
        FabMenu(listOf(FabMenuAction(CpIcons.Settings, "Settings", {}),
            FabMenuAction(CpIcons.Close, "Close", {}),
            FabMenuAction(CpIcons.Search, "Search unavailable", {}, enabled = false)))
    }
}

@Preview @Composable private fun FabMenuLightPreview() = FabMenuPreviewContent(false)
@Preview @Composable private fun FabMenuDarkPreview() = FabMenuPreviewContent(true)
