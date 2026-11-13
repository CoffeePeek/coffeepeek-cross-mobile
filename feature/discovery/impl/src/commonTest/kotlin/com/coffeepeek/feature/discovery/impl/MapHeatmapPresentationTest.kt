package com.coffeepeek.feature.discovery.impl

import com.coffeepeek.feature.discovery.domain.model.CheckInHeatmap
import com.coffeepeek.feature.discovery.domain.model.CheckInHeatPoint
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.impl.map.MapState
import com.coffeepeek.feature.discovery.impl.map.canvasHeatmap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapHeatmapPresentationTest {
    private val cold = CheckInHeatPoint("cold", Coordinates(53.9, 27.56), 4)
    private val hot = cold.copy(shopId = "hot", checkInCount = 40)

    @Test fun intensityTracksVisitsAndDoesNotRenormalizeWhenVisibleShopsChange() {
        val state = MapState(showHeatmap = true, heatmap = CheckInHeatmap(listOf(cold, hot)))
        val points = state.canvasHeatmap(false)
        assertEquals(10.0, points[1].weight / points[0].weight, absoluteTolerance = 1e-9)
        assertEquals(points[0], state.copy(heatmap = CheckInHeatmap(listOf(cold))).canvasHeatmap(false).single())
    }

    @Test fun fifteenVisitsReachTheTopOfThePaletteForAnIsolatedShop() {
        val state = MapState(showHeatmap = true, heatmap = CheckInHeatmap(listOf(cold.copy(checkInCount = 15))))
        val peakDensity = state.canvasHeatmap(false).single().weight / kotlin.math.sqrt(2 * kotlin.math.PI)
        assertTrue(peakDensity in 1.0..1.05)
    }

    @Test fun previewDisabledLayerAndZeroVisitsDoNotRenderHeat() {
        val state = MapState(showHeatmap = true, heatmap = CheckInHeatmap(listOf(cold, hot.copy(checkInCount = 0))))
        assertEquals(1, state.canvasHeatmap(false).size)
        assertTrue(state.canvasHeatmap(true).isEmpty())
        assertTrue(state.copy(showHeatmap = false).canvasHeatmap(false).isEmpty())
    }
}
