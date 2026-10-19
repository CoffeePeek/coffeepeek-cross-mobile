package com.coffeepeek.feature.shop.data.mapper

import com.coffeepeek.feature.shop.data.backend.ShopCheckInDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ShopCheckInPhotoDeliveryTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val files = ShopFileUrlResolver("https://api.example")

    @Test fun variantsKeepServerOriginalsAndOrderedAlbumIndices() {
        val visit = decode("""{
          "id":"visit","authorId":"opaque-id","author":{"slug":"public-slug"},
          "photos":[
            {"url":"/original-b?signature=issued","sortIndex":2,"variants":[]},
            {"url":"/original-a","sortIndex":1,"variants":[
              {"purpose":"feed","url":"/feed-a","contentType":"image/jpeg","width":600,"height":400,"sizeBytes":12},
              {"purpose":"detail","url":"/detail-a","contentType":"image/jpeg","width":1200,"height":800,"sizeBytes":24}
            ]},
            {"url":"","sortIndex":0,"variants":[]}
          ]
        }""")
        assertEquals("opaque-id", visit.authorId)
        assertEquals("public-slug", visit.authorSlug)
        assertEquals(listOf("https://api.example/original-a", "https://api.example/original-b?signature=issued"), visit.photoUrls)
        assertEquals(listOf("https://api.example/feed-a", ""), visit.photoThumbnailUrls)
        // Matches main: full-screen can fall back to the explicitly issued original, never an invented URL.
        assertEquals(listOf("https://api.example/detail-a", "https://api.example/original-b?signature=issued"), visit.photoDetailUrls)
    }

    @Test fun missingManifestKeepsLegacyFallbackButEmptyManifestDoesNot() {
        val visit = decode("""{"id":"visit","photos":[
          {"url":"/legacy"}, {"url":"/empty","variants":[]}
        ],"drinkPhotoUrl":"/cup","drinkPhotoVariants":[]}""")
        assertEquals(listOf("https://api.example/legacy", ""), visit.photoThumbnailUrls)
        assertEquals("https://api.example/cup", visit.drinkPhotoUrl)
        assertEquals("", visit.drinkPhotoThumbnailUrl)
    }

    @Test fun cutoutPrefersPngAndRejectsInvalidOrUnsupportedVariants() {
        val visit = decode("""{"id":"visit","drinkPhotoUrl":"/cup","drinkPhotoVariants":[
          {"purpose":"thumb","url":"/jpeg","contentType":"image/jpeg","width":100,"height":100,"sizeBytes":10},
          {"purpose":"thumb","url":"/png","contentType":"image/png","width":100,"height":100,"sizeBytes":20},
          {"purpose":"thumb","url":"/invalid","contentType":"image/png","width":0,"height":100,"sizeBytes":20}
        ],"photos":[{"url":"/original","variants":[
          {"purpose":"feed","url":"/avif","contentType":"image/avif","width":100,"height":100,"sizeBytes":10}
        ]}]}""")
        assertEquals("https://api.example/png", visit.drinkPhotoThumbnailUrl)
        assertEquals(listOf(""), visit.photoThumbnailUrls)
    }

    private fun decode(value: String) = assertNotNull(json.decodeFromString<ShopCheckInDto>(value).toDomain(files))
}
