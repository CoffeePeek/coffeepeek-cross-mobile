package com.coffeepeek.admin.di.contributions

import com.coffeepeek.admin.utils.PickedImage
import kotlin.test.*

class ContributionPhotoMappingTest {
    @Test fun platformConversionCapsEveryFormLimitWithoutSharingMutableBytes() {
        val photo = PickedImage(byteArrayOf(1, 2), "source.jpg", "image/jpeg")
        assertTrue(listOf(photo).toContributionPhotos(-1).isEmpty())
        val photos = List(12) { photo }.toContributionPhotos(12)
        assertEquals(10, photos.size)
        assertEquals(4, List(12) { photo }.toContributionPhotos(4).size)
        assertEquals("source.jpg", photos.first().fileName)
        photo.bytes[0] = 9
        assertContentEquals(byteArrayOf(1, 2), photos.first().bytes)
        photos.first().bytes[1] = 9
        assertContentEquals(byteArrayOf(1, 2), photos[1].bytes)
    }
}
