package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.discovery.impl.R

@Composable
internal fun DiscoverySectionTitle(title: String, onShowAll: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        TextButton(onShowAll) {
            Text(stringResource(R.string.discovery_show_all))
            Icon(CoffeePeekIcons.ChevronRight, null, Modifier.padding(start = CoffeePeekDimens.spacing1).height(16.dp).width(16.dp))
        }
    }
}

@PreviewLightDark
@Composable
private fun DiscoverySectionTitlePreview() = CoffeePeekTheme {
    DiscoverySectionTitle(stringResource(R.string.discovery_section_shops), {})
}
