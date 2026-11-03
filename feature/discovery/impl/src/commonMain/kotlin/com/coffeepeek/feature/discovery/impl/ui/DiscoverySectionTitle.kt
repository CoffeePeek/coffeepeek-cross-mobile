package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.impl.resources.Res
import com.coffeepeek.feature.discovery.impl.resources.discovery_section_roasters
import com.coffeepeek.feature.discovery.impl.resources.discovery_section_shops
import com.coffeepeek.feature.discovery.impl.resources.discovery_show_all
import com.coffeepeek.feature.discovery.impl.resources.discovery_section_shops
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable
internal fun DiscoverySectionTitle(title: String, onShowAll: () -> Unit, modifier: Modifier = Modifier) {
    if (LocalDensity.current.fontScale > 1.2f) {
        Column(modifier.fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            ShowAllAction(onShowAll, Modifier.align(Alignment.End))
        }
        return
    }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        ShowAllAction(onShowAll)
    }
}

@Composable
private fun ShowAllAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick, modifier) {
        Text(stringResource(Res.string.discovery_show_all))
        Icon(CoffeePeekIcons.ChevronRight, null, Modifier.padding(start = CoffeePeekDimens.spacing1).size(16.dp))
    }
}

@Composable
private fun DiscoverySectionTitlePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    DiscoverySectionTitle(stringResource(Res.string.discovery_section_shops), {})
}

@Preview @Composable private fun DiscoverySectionTitlePreviewLight() = DiscoverySectionTitlePreview(false)
@Preview @Composable private fun DiscoverySectionTitlePreviewDark() = DiscoverySectionTitlePreview(true)
@Preview
@Composable
private fun DiscoverySectionTitleLargePreview() = CoffeePeekTheme {
    CompositionLocalProvider(LocalDensity provides androidx.compose.ui.unit.Density(LocalDensity.current.density, 2f)) {
        DiscoverySectionTitle(stringResource(Res.string.discovery_section_roasters), {}, Modifier.width(320.dp))
    }
}
