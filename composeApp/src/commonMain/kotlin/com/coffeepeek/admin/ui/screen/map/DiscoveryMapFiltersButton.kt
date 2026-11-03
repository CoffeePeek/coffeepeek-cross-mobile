package com.coffeepeek.admin.ui.screen.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.discovery_map_filters
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.GlassIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun DiscoveryMapFiltersButton(count: Int, onClick: () -> Unit) {
    val description = stringResource(Res.string.discovery_map_filters)
    Box(Modifier.size(CpDimens.buttonHeight + 8.dp), contentAlignment = Alignment.Center) {
        GlassIconButton(onClick, if (count > 0) "$description: $count" else description, hazeState = null) {
            Icon(CoffeePeekIcons.Filter, null, Modifier.size(22.dp))
        }
        if (count > 0) Box(Modifier.align(Alignment.TopEnd).size(22.dp)
            .clip(CircleShape).background(MaterialTheme.colorScheme.primary).clearAndSetSemantics {},
            contentAlignment = Alignment.Center) {
            Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Preview @Composable private fun DiscoveryMapFiltersLightPreview() = CoffeePeekTheme(darkTheme = false) { DiscoveryMapFiltersButton(2, {}) }
@Preview @Composable private fun DiscoveryMapFiltersDarkPreview() = CoffeePeekTheme(darkTheme = true) { DiscoveryMapFiltersButton(2, {}) }
