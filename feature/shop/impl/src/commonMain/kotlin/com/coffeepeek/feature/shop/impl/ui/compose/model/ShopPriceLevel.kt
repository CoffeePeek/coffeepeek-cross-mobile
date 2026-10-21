package com.coffeepeek.feature.shop.impl.ui.compose.model

/** Display tiers match the current shop screen; backend decoding remains in data. */
internal enum class ShopPriceLevel(val indicatorCount: Int) {
    Low(1), Average(2), High(3);

    companion object {
        fun from(priceRange: String?): ShopPriceLevel? = when (priceRange) {
            "$" -> Low
            "$$" -> Average
            "$$$", "$$$$" -> High
            else -> null
        }
    }
}
