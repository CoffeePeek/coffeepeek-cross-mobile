package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Long-press/hover magnification for controls whose compact captions cannot grow.
 * The caller retains the full label and role in its own accessibility semantics.
 * The popup uses an opaque readable card, never a backdrop from another window.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoffeePeekLargeContentViewer(
    label: String, icon: ImageVector, modifier: Modifier = Modifier, content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    TooltipBox(
        modifier = modifier,
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        state = rememberTooltipState(),
        tooltip = {
            CompositionLocalProvider(LocalDensity provides Density(density.density, maxOf(1.3f, density.fontScale))) {
                LargeContentCard(label, icon)
            }
        },
        content = content,
    )
}

@Composable
private fun LargeContentCard(label: String, icon: ImageVector) {
    CoffeePeekContentCard(Modifier.widthIn(max = 280.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        }
    }
}

@Preview @Composable private fun CoffeePeekLargeContentViewerLightPreview() = CoffeePeekTheme(darkTheme = false) {
    LargeContentCard("CoffeePeek", CoffeePeekIcons.Search)
}
@Preview @Composable private fun CoffeePeekLargeContentViewerDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    LargeContentCard("CoffeePeek", CoffeePeekIcons.Search)
}
