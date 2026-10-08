package com.coffeepeek.feature.shop.data.mapper

import com.coffeepeek.feature.shop.data.backend.ShopCatalogItemDto
import com.coffeepeek.feature.shop.data.backend.ShopDetailsDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShopFeatureMapperTest {
    @Test fun fallsBackToTagsWhenShopTagsContainNoUsableNames() {
        val details = ShopDetailsDto(
            brewMethods = listOf(ShopCatalogItemDto(name = "V60", slug = "v60")),
            shopTags = Json.parseToJsonElement("""[{"id":"empty","name":" "}]"""),
            tags = Json.parseToJsonElement("""["Wi-Fi",{"title":"Можно с животными","slug":"pets"},"v60"]"""),
        )

        val features = details.toFeatures()
        assertEquals(listOf("V60", "Wi-Fi", "Можно с животными"), features.map { it.name })
        assertEquals("v60", features.first().slug)
        assertTrue(features.first().isBrewMethod)
        assertEquals("pets", features.last().slug)
        assertFalse(features.last().isBrewMethod)
    }
}
