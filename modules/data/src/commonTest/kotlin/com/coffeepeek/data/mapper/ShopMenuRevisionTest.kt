package com.coffeepeek.data.mapper

import com.coffeepeek.api.model.response.shop.ShopMenuDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ShopMenuRevisionTest {
    @Test fun publishedMenuRevisionSurvivesDecodingAndDomainMapping() {
        for (revision in listOf(null, 0L, 17L, -1L, Long.MAX_VALUE)) {
            val dto = Json.decodeFromString<ShopMenuDto>("""{"inputRevision":$revision}""")
            val menu = with(ShopMapper) { dto.toDomain() }
            assertEquals(revision, menu.inputRevision)
        }
    }
}
