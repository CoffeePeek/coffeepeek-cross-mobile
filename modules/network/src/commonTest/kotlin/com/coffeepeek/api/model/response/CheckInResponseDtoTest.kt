package com.coffeepeek.api.model.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CheckInResponseDtoTest {

    @Test
    fun decodesNullableFieldsAndStringNumbers() {
        val response = Json.decodeFromString<GetUserCheckInsResponseDto>(
            """
                {
                  "items": [
                    {
                      "id": "check-in-1",
                      "shop": {"slug":"shop-1","canonicalPath":"/coffee-shops/shop-1","revision":1,"isAlias":false},
                      "note": null,
                      "createdAt": "2026-09-16T12:27:27.922Z",
                      "visitedAt": "2026-09-16T12:27:27.922Z",
                      "reviewId": null,
                      "shopName": null,
                      "rating": {
                        "place": "4",
                        "service": "5",
                        "coffee": "3"
                      },
                      "photos": [
                        {
                          "id": "photo-1",
                          "fileName": null,
                          "storageKey": null,
                          "fullUrl": "https://cdn.example/check-in.jpg",
                          "sortIndex": "1",
                          "isPrimary": true
                        }
                      ]
                    }
                  ],
                  "totalItems": "1",
                  "totalPages": "1",
                  "currentPage": "1",
                  "pageSize": "20"
                }
            """.trimIndent(),
        )

        val checkIn = response.checkIns.single()
        assertNull(checkIn.note)
        assertNull(checkIn.shopName)
        assertEquals(4, checkIn.rating?.place)
        assertEquals(5, checkIn.rating?.service)
        assertEquals(3, checkIn.rating?.coffee)
        assertEquals(1, checkIn.photos.single().sortIndex)
        assertTrue(checkIn.photos.single().isPrimary)
        assertEquals(1, response.totalItems)
        assertEquals(1, response.totalPages)
        assertEquals(1, response.currentPage)
        assertEquals(20, response.pageSize)
    }
}
