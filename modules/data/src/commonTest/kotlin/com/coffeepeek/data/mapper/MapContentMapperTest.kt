package com.coffeepeek.data.mapper

import com.coffeepeek.api.model.response.shop.GetShopsInBoundsResponseDto
import com.coffeepeek.domain.model.MapBounds
import com.coffeepeek.domain.model.MapCluster
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapContentMapperTest {
    @Test fun retainedIosReadsServerClusterBoundsAndExactCounts() {
        val response = Json.decodeFromString<GetShopsInBoundsResponseDto>(
            """{"Clusters":[{"Latitude":1,"Longitude":1,"Count":120,"Bounds":{"MinLatitude":0,"MinLongitude":0,"MaxLatitude":2,"MaxLongitude":2}}]}""",
        )
        val content = response.toMapContent(null)
        assertTrue(content.shops.isEmpty())
        assertEquals(MapCluster("1.0:1.0:120", 1.0, 1.0, 120, MapBounds(0.0, 0.0, 2.0, 2.0)), content.clusters.single())
    }

    @Test fun invalidClustersCannotReachTheNativeRenderer() {
        val response = Json.decodeFromString<GetShopsInBoundsResponseDto>(
            """{"clusters":[{"latitude":1,"longitude":1,"count":0,"bounds":{"minLatitude":0,"minLongitude":0,"maxLatitude":2,"maxLongitude":2}}]}""",
        )
        assertTrue(response.toMapContent(null).clusters.isEmpty())
    }
}
