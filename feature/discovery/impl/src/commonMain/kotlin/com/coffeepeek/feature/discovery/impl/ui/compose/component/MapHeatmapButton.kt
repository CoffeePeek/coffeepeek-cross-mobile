package com.coffeepeek.feature.discovery.impl.ui.compose.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.discovery.impl.resources.Res
import com.coffeepeek.feature.discovery.impl.resources.discovery_heatmap_hide
import com.coffeepeek.feature.discovery.impl.resources.discovery_heatmap_show
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun MapHeatmapButton(enabled: Boolean, onClick: () -> Unit) {
    CoffeePeekGlassIconButton(onClick,
        stringResource(if (enabled) Res.string.discovery_heatmap_hide else Res.string.discovery_heatmap_show),
        Modifier.semantics { selected = enabled },
    ) {
        Icon(CoffeePeekIcons.Fire, null, Modifier.size(26.dp),
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

@Preview @Composable private fun MapHeatmapButtonLightPreview() = CoffeePeekTheme { MapHeatmapButton(true, {}) }
@Preview @Composable private fun MapHeatmapButtonDarkPreview() = CoffeePeekTheme(darkTheme = true) { MapHeatmapButton(true, {}) }
