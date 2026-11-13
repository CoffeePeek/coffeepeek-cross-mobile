package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.impl.resources.Res
import com.coffeepeek.feature.discovery.impl.resources.discovery_location_pending
import com.coffeepeek.feature.discovery.impl.resources.discovery_location_unavailable
import com.coffeepeek.feature.discovery.impl.resources.discovery_message_dismiss
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import com.coffeepeek.core.designsystem.component.CoffeePeekContentCard
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton
import com.coffeepeek.core.designsystem.component.CoffeePeekIcon
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable
internal fun DiscoveryLocationNotice(pending: Boolean, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    CoffeePeekContentCard(modifier.padding(horizontal = 16.dp).semantics { liveRegion = LiveRegionMode.Polite },
        contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(if (pending) Res.string.discovery_location_pending else Res.string.discovery_location_unavailable),
                Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            CoffeePeekIconButton(onDismiss) {
                CoffeePeekIcon(CoffeePeekIcons.Close, stringResource(Res.string.discovery_message_dismiss), Modifier.size(20.dp))
            }
        }
    }
}

@Preview @Composable private fun DiscoveryLocationNoticePreview() = CoffeePeekTheme {
    DiscoveryLocationNotice(false, {})
}

@Preview @Composable private fun DiscoveryLocationNoticeLargePreview() = CoffeePeekTheme {
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
        Box(Modifier.width(320.dp)) { DiscoveryLocationNotice(false, {}) }
    }
}
