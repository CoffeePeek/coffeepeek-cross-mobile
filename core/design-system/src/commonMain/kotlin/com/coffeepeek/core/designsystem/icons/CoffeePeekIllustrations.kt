package com.coffeepeek.core.designsystem.icons

import com.coffeepeek.core.designsystem.resources.Res
import com.coffeepeek.core.designsystem.resources.maskot_astonishment
import com.coffeepeek.core.designsystem.resources.maskot_with_magnifying_glass
import com.coffeepeek.core.designsystem.resources.shop_maskot_with_book
import org.jetbrains.compose.resources.DrawableResource

/** Shared existing mascot artwork; resource names retain their provenance. */
object CoffeePeekIllustrations {
    val LoadError: DrawableResource get() = Res.drawable.maskot_astonishment
    val CatalogSearch: DrawableResource get() = Res.drawable.maskot_with_magnifying_glass
    val CreationSuccess: DrawableResource get() = Res.drawable.shop_maskot_with_book

    @OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
    suspend fun loadErrorBytes(): ByteArray = Res.readBytes("drawable/maskot_astonishment.png")

}
