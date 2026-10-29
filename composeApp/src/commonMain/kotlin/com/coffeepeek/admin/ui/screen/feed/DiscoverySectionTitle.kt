package com.coffeepeek.admin.ui.screen.feed

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun DiscoverySectionTitle(title: String, onShowAll: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        if (onShowAll != null) TextButton(onClick = onShowAll) {
            Text("Показать все")
            Spacer(Modifier.width(CpDimens.spacing1))
            Icon(CpIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun DiscoverySectionTitlePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    DiscoverySectionTitle("Кофейни", {})
}

@Preview @Composable private fun DiscoverySectionTitleLightPreview() = DiscoverySectionTitlePreview(false)
@Preview @Composable private fun DiscoverySectionTitleDarkPreview() = DiscoverySectionTitlePreview(true)
