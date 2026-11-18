package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.coffeepeek.core.designsystem.component.coffeePeekNativeImage
import com.coffeepeek.feature.discovery.domain.model.CatalogEntry
import com.coffeepeek.feature.discovery.impl.ui.compose.component.discoveryBrewArtwork
import com.coffeepeek.feature.discovery.impl.ui.compose.component.discoveryTagArtwork
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun discoveryNativeFilterIcon(group: DiscoveryFilterGroup, entry: CatalogEntry): platform.UIKit.UIImage? = when (group) {
    DiscoveryFilterGroup.BrewMethods -> discoveryNativeBrewIcon(entry.name)
    DiscoveryFilterGroup.Tags -> coffeePeekNativeImage(rememberVectorPainter(discoveryTagArtwork(entry.id)), size = 28f)
    else -> null
}

@Composable
internal fun discoveryNativeBrewIcon(name: String): platform.UIKit.UIImage? = coffeePeekNativeImage(painterResource(discoveryBrewArtwork(name)), size = 28f)
