package com.coffeepeek.admin.ui.screen.map

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.discovery_map_filters
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun DiscoveryMapFiltersButton(count: Int, onClick: () -> Unit) {
    BadgedBox(badge = { if (count > 0) Badge { Text(count.toString()) } }) {
        CoffeePeekGlassIconButton(onClick, stringResource(Res.string.discovery_map_filters)) {
            Icon(CoffeePeekIcons.Filter, null, Modifier.size(22.dp))
        }
    }
}

@Preview @Composable private fun DiscoveryMapFiltersLightPreview() = CoffeePeekTheme(darkTheme = false) { DiscoveryMapFiltersButton(2, {}) }
@Preview @Composable private fun DiscoveryMapFiltersDarkPreview() = CoffeePeekTheme(darkTheme = true) { DiscoveryMapFiltersButton(2, {}) }
