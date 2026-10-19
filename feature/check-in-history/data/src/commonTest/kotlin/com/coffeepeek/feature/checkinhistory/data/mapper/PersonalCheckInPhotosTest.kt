package com.coffeepeek.feature.checkinhistory.data.mapper

import com.coffeepeek.feature.checkinhistory.data.backend.HistoryCheckInDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class PersonalCheckInPhotosTest {
    private fun entry(fields: String) = Json { ignoreUnknownKeys = true }
        .decodeFromString<HistoryCheckInDto>("""{"id":"visit",$fields}""").toDomain("https://fixture.test")

    private fun variant(purpose: String, path: String, type: String = "image/png", width: Int = 100) =
        """{"purpose":"$purpose","url":"$path","contentType":"$type","width":$width,"height":100,"sizeBytes":1000}"""

    @Test fun absentManifestPreservesLegacyIssuedUrlsAndSignedQueries() {
        val item = entry(""""photos":[{"url":"/original?signature=fixture"}],"drinkPhotoUrl":"/cutout"""")
        assertEquals(listOf("https://fixture.test/original?signature=fixture"), item.photoUrls)
        assertEquals(item.photoUrls, item.photoThumbnailUrls)
        assertEquals(item.photoUrls, item.photoDetailUrls)
        assertEquals(item.photoUrls, item.photoCalendarUrls)
    }

    @Test fun explicitEmptyOrUnsupportedManifestNeverFallsBackInlineToOriginals() {
        val item = entry(""""photos":[{"url":"/original","variants":[]},
            {"url":"/second","variants":[${variant("feed", "/unsupported", "image/avif")}]}],
            "drinkPhotoUrl":"/cutout","drinkPhotoVariants":[]""")
        assertEquals(listOf("https://fixture.test/original", "https://fixture.test/second"), item.photoUrls)
        assertEquals(listOf("", ""), item.photoThumbnailUrls)
        assertEquals(listOf("", ""), item.photoDetailUrls)
        assertEquals(listOf("", ""), item.photoCalendarUrls)
    }

    @Test fun purposeVariantsStayAlignedAfterSortingAndRejectingInvalidOriginals() {
        val item = entry(""""photos":[
            {"url":"/original-b","sortIndex":2,"variants":[${variant("feed", "/feed-b")},${variant("detail", "/detail-b")},${variant("thumb", "/thumb-b")}]},
            {"url":"javascript:invalid","sortIndex":0,"variants":[${variant("feed", "/must-not-shift")}]},
            {"url":"/original-a","sortIndex":"1","variants":[${variant("feed", "/jpeg", "image/jpeg")},${variant("feed", "/feed-a")},${variant("detail", "/detail-a")},${variant("thumb", "/thumb-a")}]}
            ],"drinkPhotoUrl":"/cutout","drinkPhotoVariants":[${variant("thumb", "/cutout-thumb")}]""")
        assertEquals(listOf("https://fixture.test/original-a", "https://fixture.test/original-b"), item.photoUrls)
        assertEquals(listOf("https://fixture.test/feed-a", "https://fixture.test/feed-b"), item.photoThumbnailUrls)
        assertEquals(listOf("https://fixture.test/detail-a", "https://fixture.test/detail-b"), item.photoDetailUrls)
        assertEquals(listOf("https://fixture.test/thumb-a", "https://fixture.test/thumb-b"), item.photoCalendarUrls)
    }

    @Test fun invalidGeometryAndUnsafeVariantUrlsCannotDisplaceAReadySupportedVariant() {
        val item = entry(""""photos":[{"url":"/original","variants":[
            ${variant("feed", "/zero-width", width = 0)},${variant("feed", "//foreign.test/image")},
            ${variant("thumb", "/wrong-purpose")},${variant("feed", "/ready", "image/webp")}
            ]}]""")
        assertEquals(listOf("https://fixture.test/ready"), item.photoThumbnailUrls)
        assertEquals(listOf(""), item.photoDetailUrls)
    }
}
