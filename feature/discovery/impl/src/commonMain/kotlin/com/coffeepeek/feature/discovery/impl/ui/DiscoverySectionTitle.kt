package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.impl.resources.Res
import com.coffeepeek.feature.discovery.impl.resources.discovery_show_all
import com.coffeepeek.feature.discovery.impl.resources.discovery_section_shops
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable
internal fun DiscoverySectionTitle(title: String, onShowAll: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        TextButton(onShowAll) {
            Text(stringResource(Res.string.discovery_show_all))
            Icon(CoffeePeekIcons.ChevronRight, null, Modifier.padding(start = CoffeePeekDimens.spacing1).height(16.dp).width(16.dp))
        }
    }
}

@Composable
private fun DiscoverySectionTitlePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    DiscoverySectionTitle(stringResource(Res.string.discovery_section_shops), {})
}

@Preview @Composable private fun DiscoverySectionTitlePreviewLight() = DiscoverySectionTitlePreview(false)
@Preview @Composable private fun DiscoverySectionTitlePreviewDark() = DiscoverySectionTitlePreview(true)
