package com.coffeepeek.api.model.response.shop

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoffeeShopDetailsDtoTest {

    @Test
    fun decodesCurrentPublicCheckInsWithoutLegacyReviews() {
        val details = Json.decodeFromString<CoffeeShopDetailsDto>(
            """{
                "address":{"slug":"shop","canonicalPath":"/coffee-shops/shop","revision":1,"isAlias":false},
                "name":"Кофейня", "checkInCount":"12",
                "checkIns":[{
                    "id":"public-visit","shop":null,"author":null,"username":"Анна","shopName":"Кофейня",
                    "text":"Хороший капучино","createdAtUtc":"2026-10-08T12:00:00Z","visitedAt":"2026-10-01T12:00:00Z",
                    "visibility":"Public","moderationState":"Approved","rating":{"place":4,"service":5,"coffee":5},
                    "helpfulCount":"3","isHelpfulByCurrentUser":true,"drinkSlug":"cappuccino","drinkNameRu":"Капучино",
                    "photos":[{"id":"photo","url":"/api/v1/check-ins/public-visit/photos/photo","sortIndex":0}]
                }],
                "userCheckIns":[{"id":"own-private-visit","text":"Личная заметка","visibility":"Private"}]
            }""",
        )
        assertEquals(12, details.checkInCount)
        assertTrue(details.reviews.isEmpty())
        val visit = details.checkIns.single()
        assertEquals("public-visit", visit.id)
        assertEquals("Хороший капучино", visit.text)
        assertEquals("2026-10-08T12:00:00Z", visit.createdAtUtc)
        assertEquals("2026-10-01T12:00:00Z", visit.visitedAt)
        assertEquals(5, visit.rating.coffee)
        assertEquals("Капучино", visit.drinkNameRu)
        assertEquals(3, visit.helpfulCount)
        assertTrue(visit.isHelpfulByCurrentUser)
        assertEquals("/api/v1/check-ins/public-visit/photos/photo", visit.photos.single().url)
        assertEquals("own-private-visit", details.userCheckIns.single().id)
    }

    @Test
    fun decodesCheckInCountInShopSummaryAndDefaultsMissingLists() {
        val summary = Json.decodeFromString<ShortShopDto>(
            """{"address":{"slug":"shop","canonicalPath":"/coffee-shops/shop","revision":1,"isAlias":false},"name":"Кофейня","checkInCount":7}""",
        )
        assertEquals(7, summary.checkInCount)
        val details = Json.decodeFromString<CoffeeShopDetailsDto>("""{"address":{"slug":"shop","canonicalPath":"/coffee-shops/shop","revision":1,"isAlias":false}}""")
        assertEquals(0, details.checkInCount)
        assertTrue(details.checkIns.isEmpty())
        assertTrue(details.userCheckIns.isEmpty())
    }

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
