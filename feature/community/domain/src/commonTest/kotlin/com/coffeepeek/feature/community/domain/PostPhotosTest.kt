package com.coffeepeek.feature.community.domain

import com.coffeepeek.feature.community.domain.model.PostPhotos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PostPhotosTest {
    private val photos = PostPhotos(
        originals = listOf("/o/0", "/o/1", ""),
        thumbnails = listOf("/t/0", "", ""),
        details = listOf("/d/0", "", ""),
    )

    @Test fun anInlinePhotoUpgradesToItsDetailVariantNotTheFullOriginal() = assertEquals("/d/0", photos.inlineSource(0))

    @Test fun aMissingDetailFallsBackToTheOriginal() = assertEquals("/o/1", photos.inlineSource(1))

    @Test fun nothingUsableOrOutOfRangeGivesNull() {
        assertNull(photos.inlineSource(2))
        assertNull(photos.inlineSource(7))
    }
}
