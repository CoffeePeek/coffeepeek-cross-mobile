package com.coffeepeek.api.model.response

import kotlinx.serialization.json.Json
import kotlin.test.*

class CheckInResponseDtoTest {
    @Test
    fun decodesPersonalHistoryWithNullableAddressesAndModerationState() {
        val response = Json.decodeFromString<GetUserCheckInsResponseDto>(
            """{"items":[{
                "id":"check-in-1","shop":null,"author":null,"username":"Анна","shopName":"Кофейня",
                "text":"Отличный фильтр","createdAtUtc":"2026-10-07T12:27:27.922Z","visitedAt":"2026-10-07T09:00:00+03:00",
                "rating":{"place":"4","service":"5","coffee":"3"},"visibility":"Public",
                "moderationState":"Rejected","contentRevision":"2","rejectionReason":"Уточните текст",
                "drinkSlug":"other","customDrinkName":"Тоник","drinkNameRu":"Другое","drinkNameEn":"Other",
                "helpfulCount":"7","isHelpfulByCurrentUser":false,
                "photos":[{"id":"photo-1","fileName":"coffee.jpg","contentType":"image/jpeg","storageKey":"check-ins/key.jpg",
                    "sizeBytes":1200,"sortIndex":"1","url":"/api/v1/check-ins/check-in-1/photos/photo-1"}]
            }],"totalCount":"41"}"""
        )
        val checkIn = response.items.single()
        assertNull(checkIn.shop)
        assertNull(checkIn.author)
        assertEquals("Анна", checkIn.username)
        assertEquals("Отличный фильтр", checkIn.text)
        assertEquals("2026-10-07T12:27:27.922Z", checkIn.createdAtUtc)
        assertEquals(4, checkIn.rating!!.place)
        assertEquals("Rejected", checkIn.moderationState)
        assertEquals(2, checkIn.contentRevision)
        assertEquals("Уточните текст", checkIn.rejectionReason)
        assertEquals("Тоник", checkIn.customDrinkName)
        assertEquals(7, checkIn.helpfulCount)
        assertEquals("/api/v1/check-ins/check-in-1/photos/photo-1", checkIn.photos.single().url)
        assertEquals(41, response.totalCount)
    }
}
