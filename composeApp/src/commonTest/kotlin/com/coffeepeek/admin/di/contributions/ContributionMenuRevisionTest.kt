package com.coffeepeek.admin.di.contributions

import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.model.ShopMenu
import kotlin.test.Test
import kotlin.test.assertEquals

class ContributionMenuRevisionTest {
    private val details = CoffeeShopDetails(
        shop = CoffeeShop("cafe", "Cafe", rating = null, cityName = null, priceRange = null, photoUrl = null),
    )

    @Test fun snapshotKeepsThePublishedMenuRevisionWithoutInventingOne() {
        for (revision in listOf(null, 0L, 17L, -1L)) {
            val snapshot = details.copy(menu = ShopMenu(inputRevision = revision)).toSnapshot("cafe")
            assertEquals(revision, snapshot.menuInputRevision)
        }
    }

    @Test fun aShopWithoutAMenuHasRevisionZero() {
        assertEquals(0L, details.toSnapshot("cafe").menuInputRevision)
    }
}
