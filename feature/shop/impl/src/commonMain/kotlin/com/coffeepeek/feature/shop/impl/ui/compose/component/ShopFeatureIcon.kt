package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekBrewIcons
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.domain.model.ShopFeature
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Decorative icon: the adjacent feature label supplies its accessible meaning. */
@Composable
internal fun ShopFeatureIcon(feature: ShopFeature, modifier: Modifier = Modifier) {
    if (feature.isBrewMethod) {
        val name = feature.name.lowercase()
        val resource = when {
            "аэропресс" in name || "aeropress" in name -> CoffeePeekBrewIcons.Aeropress
            "v60" in name || "воронк" in name || "пуровер" in name || "pour" in name -> CoffeePeekBrewIcons.V60
            "колд" in name || "cold" in name -> CoffeePeekBrewIcons.ColdBrew
            "турк" in name || "turkish" in name || "джезв" in name || "cezve" in name -> CoffeePeekBrewIcons.TurkishCoffee
            "машин" in name || "machine" in name || "эспрессо" in name || "espresso" in name || "рожк" in name ->
                CoffeePeekBrewIcons.CoffeeMachine
            else -> CoffeePeekBrewIcons.Coffee
        }
        Icon(painterResource(resource), null, modifier)
    } else {
        val icon = when (feature.slug?.trim()?.lowercase()) {
            "roastery" -> CoffeePeekIcons.Factory
            "beans-for-sale" -> CoffeePeekIcons.ShoppingBag
            "decaf" -> CoffeePeekIcons.CoffeeBean
            "to_go" -> CoffeePeekIcons.Footprints
            "plan-based-milk", "plant-based-milk" -> CoffeePeekIcons.Leaf
            "laptop_friendly" -> CoffeePeekIcons.Laptop
            "outside-seating" -> CoffeePeekIcons.ThemeLight
            "breakfasts" -> CoffeePeekIcons.Egg
            "bakery" -> CoffeePeekIcons.Bread
            "confectionery" -> CoffeePeekIcons.Cake
            "pet_friendly" -> CoffeePeekIcons.PawPrint
            "vegan" -> CoffeePeekIcons.Avocado
            "gluten-free" -> CoffeePeekIcons.GrainsSlash
            else -> CoffeePeekIcons.ListStar
        }
        Icon(icon, null, modifier)
    }
}

@Preview @Composable private fun ShopFeatureIconLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopFeatureIcon(ShopFeature("V60", "v60", true), Modifier.size(28.dp))
}
@Preview @Composable private fun ShopFeatureIconDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopFeatureIcon(ShopFeature("V60", "v60", true), Modifier.size(28.dp))
}
@Preview @Composable private fun ShopTagIconLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopFeatureIcon(ShopFeature("Можно с животными", "pet_friendly", false), Modifier.size(28.dp))
}
@Preview @Composable private fun ShopTagIconDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopFeatureIcon(ShopFeature("Можно с животными", "pet_friendly", false), Modifier.size(28.dp))
}
