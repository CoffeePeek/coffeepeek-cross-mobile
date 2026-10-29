package com.coffeepeek.core.designsystem.icons

import com.coffeepeek.core.designsystem.resources.Res
import com.coffeepeek.core.designsystem.resources.maskot_astonishment
import com.coffeepeek.core.designsystem.resources.maskot_with_magnifying_glass
import org.jetbrains.compose.resources.DrawableResource

/** Shared existing mascot artwork; resource names retain their provenance. */
object CoffeePeekIllustrations {
    val LoadError: DrawableResource get() = Res.drawable.maskot_astonishment
    val CatalogSearch: DrawableResource get() = Res.drawable.maskot_with_magnifying_glass
}
