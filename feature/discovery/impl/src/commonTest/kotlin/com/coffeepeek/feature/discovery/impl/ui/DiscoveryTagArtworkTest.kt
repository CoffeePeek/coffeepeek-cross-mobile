package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.feature.discovery.impl.ui.compose.component.discoveryTagArtwork
import kotlin.test.Test
import kotlin.test.assertEquals

class DiscoveryTagArtworkTest {
    @Test
    fun normalizedAmenitySlugsRetainTheirSpecificArtwork() {
        assertEquals(CoffeePeekIcons.PawPrint, discoveryTagArtwork("pet_friendly"))
        assertEquals(CoffeePeekIcons.PawPrint, discoveryTagArtwork("pet-friendly"))
        assertEquals(CoffeePeekIcons.Laptop, discoveryTagArtwork("laptop_friendly"))
        assertEquals(CoffeePeekIcons.Footprints, discoveryTagArtwork("to_go"))
        assertEquals(CoffeePeekIcons.ListStar, discoveryTagArtwork("unknown"))
    }
}
