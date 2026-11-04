package com.coffeepeek.feature.discovery.impl

import com.coffeepeek.feature.discovery.domain.model.PriceLevel
import com.coffeepeek.feature.discovery.impl.search.priceFilterStop
import com.coffeepeek.feature.discovery.impl.search.priceFilterStops
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PriceFilterScaleTest {
    @Test fun everyCurrentSearchBandRoundTripsThroughItsOwnStop() {
        assertNull(priceFilterStops[0])
        assertEquals(listOf(PriceLevel.Cheap, PriceLevel.Moderate, PriceLevel.Expensive), priceFilterStops.filterNotNull())
        priceFilterStops.forEachIndexed { index, price -> assertEquals(index, priceFilterStop(price)) }
    }

    @Test fun legacyLuxuryDisplaysAndReappliesAsTheSupportedHighestSearchBand() {
        assertEquals(3, priceFilterStop(PriceLevel.Luxury))
        assertEquals(PriceLevel.Expensive, priceFilterStops[priceFilterStop(PriceLevel.Luxury)])
        assertEquals(priceFilterStop(PriceLevel.Expensive), priceFilterStop(PriceLevel.Luxury))
    }
}
