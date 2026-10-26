package com.coffeepeek.core.media.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProtectedPhotoRequestTest {
    @Test
    fun galleryAndDrinkVariantsUseProtectedPathAndKeepOriginalAsExplicitAction() {
        val original = "https://gateway.example/api/v1/check-ins/visit/photos/photo"
        val variant = "$original/variants/source/1/detail.png"
        assertTrue(isProtectedCheckInPhoto(original))
        assertTrue(isProtectedCheckInPhoto(variant))
        assertEquals(original, originalCheckInPhoto(variant))
        assertNull(originalCheckInPhoto(original))
        assertFalse(isProtectedCheckInPhoto("https://cdn.example/gallery/photo.jpg"))
        assertTrue(isImmutablePhotoVariant(variant))
        assertFalse(isImmutablePhotoVariant(original))
        assertFalse(isImmutablePhotoVariant("$original/variants/pending"))
        assertFalse(isImmutablePhotoVariant("$original/variants/source/0/feed.png"))
        assertFalse(isImmutablePhotoVariant("$original/variants/source/1/original.png"))
    }
}
