package com.coffeepeek.feature.homecheckin.data.mapper

import com.coffeepeek.feature.homecheckin.data.backend.HomeDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class HomePreparationPhotosTest {
    private fun entry(fields: String) = Json { ignoreUnknownKeys = true }.decodeFromString<HomeDto>(
        """{"id":"home","preparedAtUtc":"2026-10-19T08:00:00Z","preparedOn":"2026-10-19",$fields}"""
    ).toDomain("https://fixture.invalid")

    private fun variant(purpose: String, path: String, type: String = "image/png", width: Int = 100) =
        """{"purpose":"$purpose","url":"$path","contentType":"$type","width":$width,"height":100,"sizeBytes":1000}"""

    @Test fun absentManifestsAndLegacyCutoutStringsKeepIssuedSignedOriginals() {
        val item = entry(""""photos":[{"id":"p","fullUrl":"/original?signature=fixture"}],"drinkPhoto":"/cutout"""")
        val photo = item.photos.single()
        assertEquals("https://fixture.invalid/original?signature=fixture", photo.url)
        assertEquals(photo.url, photo.thumbnailUrl)
        assertEquals(photo.url, photo.detailUrl)
        assertEquals(photo.url, photo.calendarUrl)
    }

    @Test fun explicitEmptyAndUnsupportedManifestsNeverLoadOriginalsInline() {
        val item = entry(""""photos":[{"url":"/original","variants":[]},
            {"url":"/second","variants":[${variant("feed", "/unsupported", "image/avif")}]}],
            "drinkPhoto":{"url":"/cutout","variants":[]}""")
        assertEquals(listOf("", ""), item.photos.map { it.thumbnailUrl })
        assertEquals(listOf("", ""), item.photos.map { it.detailUrl })
        assertEquals(listOf("", ""), item.photos.map { it.calendarUrl })
    }

    @Test fun sortedAlbumAndObjectCutoutPreserveEachRepresentationWithoutInferringOriginals() {
        val item = entry(""""photos":[
            {"id":"b","url":"/b","sortIndex":2,"variants":[${variant("feed", "/feed-b")}]},
            {"id":"invalid","url":"javascript:invalid","variants":[${variant("feed", "/ignored")}]},
            {"id":"a","url":"/a","sortIndex":1,"variants":[${variant("feed", "/jpeg", "image/jpeg")},
                ${variant("feed", "/feed-a")},${variant("detail", "/detail-a")},${variant("thumb", "/thumb-a")}]}
            ],"drinkPhoto":{"url":"/cutout","storageKey":"not-a-url","variants":[
                ${variant("thumb", "/cutout-thumb")},${variant("detail", "/cutout-detail")}]}""")
        assertEquals(listOf("a", "b"), item.photos.map { it.id })
        assertEquals(listOf("https://fixture.invalid/a", "https://fixture.invalid/b"), item.photos.map { it.url })
        assertEquals(listOf("https://fixture.invalid/feed-a", "https://fixture.invalid/feed-b"), item.photos.map { it.thumbnailUrl })
        assertEquals(listOf("https://fixture.invalid/detail-a", ""), item.photos.map { it.detailUrl })
        assertEquals(listOf("https://fixture.invalid/thumb-a", ""), item.photos.map { it.calendarUrl })
    }

    @Test fun invalidGeometryPurposeAndUnsafeUrlsCannotDisplaceReadyVariants() {
        val item = entry(""""photos":[{"url":"/original","variants":[
            ${variant("feed", "/zero-width", width = 0)},${variant("feed", "//foreign.invalid/image")},
            ${variant("thumb", "/wrong-purpose")},${variant("feed", "/ready", "image/webp")}]}]""")
        assertEquals("https://fixture.invalid/ready", item.photos.single().thumbnailUrl)
        assertEquals("", item.photos.single().detailUrl)
    }
}
