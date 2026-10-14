package com.coffeepeek.core.designsystem.icons

import com.coffeepeek.core.designsystem.resources.Res
import com.coffeepeek.core.designsystem.resources.brew_aeropress
import com.coffeepeek.core.designsystem.resources.brew_coffee
import com.coffeepeek.core.designsystem.resources.brew_coffee_machine
import com.coffeepeek.core.designsystem.resources.brew_cold_brew
import com.coffeepeek.core.designsystem.resources.brew_turkish_coffee
import com.coffeepeek.core.designsystem.resources.brew_v60
import org.jetbrains.compose.resources.DrawableResource

/** Existing vector artwork only. Mapping business names/slugs belongs to each feature. */
object CoffeePeekBrewIcons {
    val Aeropress: DrawableResource get() = Res.drawable.brew_aeropress
    val Coffee: DrawableResource get() = Res.drawable.brew_coffee
    val CoffeeMachine: DrawableResource get() = Res.drawable.brew_coffee_machine
    val ColdBrew: DrawableResource get() = Res.drawable.brew_cold_brew
    val TurkishCoffee: DrawableResource get() = Res.drawable.brew_turkish_coffee
    val V60: DrawableResource get() = Res.drawable.brew_v60
}
