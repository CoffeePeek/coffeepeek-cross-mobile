package com.coffeepeek.feature.shop.impl.ui.data

import com.coffeepeek.feature.shop.domain.model.ShopMenuItem
import kotlin.test.Test
import kotlin.test.assertEquals

/** Pure presentation fixtures; no backend, locale clock, or production DI. */
class ShopMenuPresentationTest {
    @Test fun visibleGroupsMatchTheCurrentEspressoFilterThenUnknownOrder() {
        val groups = groupedPresentMenuItems(listOf(drink("tea", "Tea"), drink("filter", "Filter"),
            drink("latte", "Espresso"), drink("sold-out", "Espresso", "Absent"),
            drink("cocoa", "Cocoa"), drink("espresso", "Espresso", "present")))
        assertEquals(listOf(listOf("latte", "espresso"), listOf("filter"), listOf("tea"), listOf("cocoa")),
            groups.map { group -> group.map { it.slug } })
    }

    @Test fun unavailableItemsDoNotCreateAnEmptyGroup() {
        assertEquals(emptyList(), groupedPresentMenuItems(listOf(drink("hidden", "Espresso", "Absent"))))
    }

    @Test fun priceKeepsTwoDecimalsAndTheLegacyRoundingRule() {
        assertEquals("5,50", formatShopMenuAmount(5.5))
        assertEquals("6,00", formatShopMenuAmount(6.0))
        assertEquals("0,00", formatShopMenuAmount(0.0))
        assertEquals("1,12", formatShopMenuAmount(1.125))
        assertEquals("-0,50", formatShopMenuAmount(-0.5))
    }

    @Test fun previewDateIsReadableAndDoesNotPretendToConvertTheTimezone() {
        assertEquals("02.10.2026", formatShopMenuDate("2026-10-02T23:00:00Z"))
        assertEquals("02.10.2026", formatShopMenuDate("2026-10-02"))
        assertEquals("unavailable", formatShopMenuDate("unavailable"))
    }

    private fun drink(slug: String, category: String, availability: String = "Present") =
        ShopMenuItem(slug, slug, slug, category, availability, 5.5, "BYN", null)
}
