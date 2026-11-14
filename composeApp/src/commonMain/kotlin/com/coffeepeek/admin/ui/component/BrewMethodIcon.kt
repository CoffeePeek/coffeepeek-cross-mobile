package com.coffeepeek.admin.ui.component

import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.brew_aeropress
import com.coffeepeek.core.designsystem.icons.CoffeePeekBrewIcons
import coffeepeek.composeapp.generated.resources.brew_coffee_machine
import coffeepeek.composeapp.generated.resources.brew_cold_brew
import coffeepeek.composeapp.generated.resources.brew_turkish_coffee
import coffeepeek.composeapp.generated.resources.brew_v60
import org.jetbrains.compose.resources.DrawableResource

/** Keyword match on the method name (RU/EN). Generic coffee icon is the fallback. */
fun brewMethodIcon(name: String): DrawableResource {
    val n = name.lowercase()
    return when {
        "аэропресс" in n || "aeropress" in n -> Res.drawable.brew_aeropress
        "v60" in n || "воронк" in n || "пуровер" in n || "pour" in n -> Res.drawable.brew_v60
        "колд" in n || "cold" in n -> Res.drawable.brew_cold_brew
        "турк" in n || "turkish" in n || "джезв" in n || "cezve" in n -> Res.drawable.brew_turkish_coffee
        "машин" in n || "machine" in n || "эспрессо" in n || "espresso" in n || "рожк" in n ->
            Res.drawable.brew_coffee_machine
        else -> CoffeePeekBrewIcons.Coffee
    }
}
