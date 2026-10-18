package com.coffeepeek.data.mapper

import com.coffeepeek.api.model.response.CheckInDto
import com.coffeepeek.data.util.FileUrlResolver
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CheckInPhotoVariantsTest {
    @Test
    fun readyVariantsReachFeedViewerCalendarAndCutoutWithoutChangingOriginalsOrOrder() {
        val original = "/api/v1/check-ins/visit/photos/first"
        fun variants() = listOf("thumb", "feed", "detail").joinToString(",") { purpose ->
            """{"purpose":"$purpose","contentType":"image/png","width":320,"height":240,"sizeBytes":200,"url":"$original/variants/source/1/$purpose.png"}"""
        }
        val dto = Json.decodeFromString<CheckInDto>("""{"id":"visit","photos":[
            {"id":"second","sortIndex":1,"url":"/second"},
            {"id":"first","sortIndex":0,"url":"$original","variants":[${variants()}]}
        ],"drinkPhotoUrl":"$original","drinkPhotoVariants":[${variants()}]}""")
        val model = dto.toDomain(FileUrlResolver("https://gateway.example/"))
        assertEquals("https://gateway.example$original", model.photoUrls[0])
        assertTrue(model.photoThumbnailUrls[0].endsWith("feed.png"))
        assertTrue(model.photoDetailUrls!![0].endsWith("detail.png"))
        assertTrue(model.photoCalendarUrls!![0].endsWith("thumb.png"))
        assertTrue(model.drinkPhotoThumbnailUrl!!.endsWith("thumb.png"))
        assertEquals("https://gateway.example/second", model.photoThumbnailUrls[1])
    }

    @Test
    fun explicitPendingOrUnknownManifestKeepsGallerySlotWithoutFetchingOriginal() {
        for (variants in listOf("[]", """[{"purpose":"unknown","contentType":"image/jpeg","width":100,"height":100,"sizeBytes":1,"url":"/unknown"}]""")) {
            val dto = Json.decodeFromString<CheckInDto>("""{"id":"visit","photos":[{"url":"/original","variants":$variants}]}""")
            val model = dto.toDomain(FileUrlResolver("https://gateway.example/"))
            assertEquals(listOf("https://gateway.example/original"), model.photoUrls)
            assertEquals(listOf(""), model.photoThumbnailUrls)
            // Only an explicit viewer action may open this original; the list remains a placeholder.
            assertEquals(model.photoUrls, model.photoDetailUrls)
        }
    }
}
