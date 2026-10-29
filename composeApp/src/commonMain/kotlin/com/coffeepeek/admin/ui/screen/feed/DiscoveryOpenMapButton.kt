package com.coffeepeek.admin.ui.screen.feed

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.discovery_open_map
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun DiscoveryOpenMapButton(onClick: () -> Unit) {
    CoffeePeekGlassIconButton(onClick, stringResource(Res.string.discovery_open_map)) {
        Icon(CoffeePeekIcons.Map, null, Modifier.size(22.dp))
    }
}

@Preview @Composable private fun DiscoveryOpenMapLightPreview() = CoffeePeekTheme(darkTheme = false) { DiscoveryOpenMapButton({}) }
@Preview @Composable private fun DiscoveryOpenMapDarkPreview() = CoffeePeekTheme(darkTheme = true) { DiscoveryOpenMapButton({}) }
