package com.coffeepeek.api.model.response.shop

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class CoffeeShopDetailsDtoTest {

    @Test
    fun decodesUserCheckInsFromShopDetails() {
        val details = Json.decodeFromString<CoffeeShopDetailsDto>(
            """
                {
                  "address": {"slug":"shop-1","canonicalPath":"/coffee-shops/shop-1","revision":1,"isAlias":false},
                  "name": "CoffeePeek",
                  "userCheckIns": [
                    {
                      "id": "check-in-1",
                      "shop": {"slug":"shop-1","canonicalPath":"/coffee-shops/shop-1","revision":1,"isAlias":false},
                      "note": "Отличный фильтр",
                      "createdAt": "2026-09-08T10:00:00Z",
                      "visitedAt": "2026-09-08T09:30:00Z",
                      "photos": [{ "fullUrl": "https://cdn.example/check-in.jpg" }],
                      "rating": { "place": 4, "service": 5, "coffee": 3 }
                    }
                  ]
                }
            """.trimIndent(),
        )

        assertEquals(1, details.userCheckIns.size)
        assertEquals("2026-09-08T09:30:00Z", details.userCheckIns.single().visitedAt)
        assertEquals(
            "https://cdn.example/check-in.jpg",
            details.userCheckIns.single().photos.single().url,
        )
        assertEquals(4, details.userCheckIns.single().rating.place)
        assertEquals(5, details.userCheckIns.single().rating.service)
        assertEquals(3, details.userCheckIns.single().rating.coffee)
    }
}
