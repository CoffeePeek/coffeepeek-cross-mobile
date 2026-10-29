package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.impl.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark

@Composable
internal fun DiscoveryFilterButton(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = if (count > 0) stringResource(R.string.discovery_filters_active, count) else stringResource(R.string.discovery_filters)
    BadgedBox(badge = {
        if (count > 0) Box(
            Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) { Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary) }
    }, modifier = modifier) {
        OutlinedIconButton(
            onClick = onClick, modifier = Modifier.size(CoffeePeekDimens.buttonHeight).semantics { contentDescription = description },
            shape = CircleShape, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) { Icon(CoffeePeekIcons.Filter, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp)) }
    }
}

@PreviewLightDark
@Composable
private fun DiscoveryFilterButtonPreview() = CoffeePeekTheme { DiscoveryFilterButton(2, {}) }
