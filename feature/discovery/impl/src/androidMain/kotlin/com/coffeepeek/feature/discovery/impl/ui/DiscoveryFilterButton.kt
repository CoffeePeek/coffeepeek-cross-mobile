package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.impl.R

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
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark

@Composable
internal fun DiscoveryFilterButton(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = if (count > 0) stringResource(R.string.discovery_filters_active, count) else stringResource(R.string.discovery_filters)
    Box(modifier.size(CoffeePeekDimens.buttonHeight + 8.dp), contentAlignment = Alignment.Center) {
        CoffeePeekGlassIconButton(onClick, description, hazeState = null) {
            Icon(CoffeePeekIcons.Filter, null, modifier = Modifier.size(22.dp))
        }
        if (count > 0) Box(
            Modifier.align(Alignment.TopEnd).size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) { Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary) }
    }
}

@PreviewLightDark
@Composable
private fun DiscoveryFilterButtonPreview() = CoffeePeekTheme { DiscoveryFilterButton(2, {}) }
