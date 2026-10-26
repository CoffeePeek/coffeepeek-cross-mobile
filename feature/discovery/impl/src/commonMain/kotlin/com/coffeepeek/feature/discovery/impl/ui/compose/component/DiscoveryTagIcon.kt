package com.coffeepeek.feature.discovery.impl.ui.compose.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

internal fun discoveryTagArtwork(slug: String?): ImageVector = when (slug?.trim()?.lowercase()) {
    "roastery" -> CoffeePeekIcons.Factory
    "beans-for-sale" -> CoffeePeekIcons.ShoppingBag
    "decaf" -> CoffeePeekIcons.CoffeeBean
    "to_go" -> CoffeePeekIcons.Footprints
    "plan-based-milk", "plant-based-milk" -> CoffeePeekIcons.Leaf
    "laptop_friendly" -> CoffeePeekIcons.Laptop
    "outside-seating" -> CoffeePeekIcons.Sun
    "breakfasts" -> CoffeePeekIcons.Egg
    "bakery" -> CoffeePeekIcons.Bread
    "confectionery" -> CoffeePeekIcons.Cake
    "pet_friendly" -> CoffeePeekIcons.PawPrint
    "vegan" -> CoffeePeekIcons.Avocado
    "gluten-free" -> CoffeePeekIcons.GrainsSlash
    else -> CoffeePeekIcons.ListStar
}

@Composable
internal fun DiscoveryTagIcon(slug: String) {
    Icon(discoveryTagArtwork(slug), null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Preview @Composable private fun DiscoveryTagIconLightPreview() = CoffeePeekTheme(darkTheme = false) { DiscoveryTagIcon("pet_friendly") }
@Preview @Composable private fun DiscoveryTagIconDarkPreview() = CoffeePeekTheme(darkTheme = true) { DiscoveryTagIcon("decaf") }
