package com.coffeepeek.feature.discovery.impl.tab

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DiscoveryBackTargetTest {
    @Test fun roastersOpenedFromHomeConsumeBackEvenWithAnEmptyQuery() {
        assertEquals(DiscoveryBackTarget.Roasters,
            discoveryBackTarget(showRoasters = true, mapExpanded = false, showDiscovery = true))
    }

    @Test fun aQueryInTheRoasterOverlayDoesNotTurnBackIntoShopSearchCancellation() {
        assertEquals(DiscoveryBackTarget.Roasters,
            discoveryBackTarget(showRoasters = true, mapExpanded = false, showDiscovery = false))
    }

    @Test fun roastersTakePriorityOverUnderlyingMapAndSearchState() {
        assertEquals(DiscoveryBackTarget.Roasters,
            discoveryBackTarget(showRoasters = true, mapExpanded = true, showDiscovery = false))
    }

    @Test fun backCollapsesTheMapBeforeCancellingTheSearchBehindIt() {
        assertEquals(DiscoveryBackTarget.Map,
            discoveryBackTarget(showRoasters = false, mapExpanded = true, showDiscovery = false))
        assertEquals(DiscoveryBackTarget.Search,
            discoveryBackTarget(showRoasters = false, mapExpanded = false, showDiscovery = false))
    }

    @Test fun aMapOpenedFromHomeStillConsumesBack() {
        assertEquals(DiscoveryBackTarget.Map,
            discoveryBackTarget(showRoasters = false, mapExpanded = true, showDiscovery = true))
    }

    @Test fun homeWithoutAnOverlayLeavesBackToTheRootNavigation() {
        assertNull(discoveryBackTarget(showRoasters = false, mapExpanded = false, showDiscovery = true))
    }
}
