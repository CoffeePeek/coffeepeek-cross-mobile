package com.coffeepeek.feature.contributions.impl.ui.component

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekBrewIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Contributions' catalog-name compatibility; shared artwork belongs to the design-system. */
internal fun contributionBrewArtwork(name: String): DrawableResource {
    val n = name.lowercase()
    return when {
        "аэропресс" in n || "aeropress" in n -> CoffeePeekBrewIcons.Aeropress
        "v60" in n || "воронк" in n || "пуровер" in n || "pour" in n -> CoffeePeekBrewIcons.V60
        "колд" in n || "cold" in n -> CoffeePeekBrewIcons.ColdBrew
        "турк" in n || "turkish" in n || "джезв" in n || "cezve" in n -> CoffeePeekBrewIcons.TurkishCoffee
        "машин" in n || "machine" in n || "эспрессо" in n || "espresso" in n || "рожк" in n -> CoffeePeekBrewIcons.CoffeeMachine
        else -> CoffeePeekBrewIcons.Coffee
    }
}

@Composable
internal fun ContributionBrewMethodIcon(name: String, size: Dp = 22.dp) {
    Icon(painterResource(contributionBrewArtwork(name)), null, Modifier.size(size), tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Preview @Composable private fun ContributionBrewMethodIconLightPreview() = CoffeePeekTheme(darkTheme = false) { ContributionBrewMethodIcon("V60", 22.dp) }
@Preview @Composable private fun ContributionBrewMethodIconDarkPreview() = CoffeePeekTheme(darkTheme = true) { ContributionBrewMethodIcon("Эспрессо", 22.dp) }
