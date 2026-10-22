package com.coffeepeek.admin.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppReviewTest {
    @Test
    fun storeBuildsOpenTheirOwnAppPage() {
        assertEquals("https://apps.apple.com/app/id6821851267?action=write-review", appReviewUrl("ios", null))
        assertEquals("https://play.google.com/store/apps/details?id=com.coffeepeek", appReviewUrl("android", "play"))
    }

    @Test
    fun directApkAndUnknownDistributionsExposeNoReviewLink() {
        assertNull(appReviewUrl("android", "apk"))
        assertNull(appReviewUrl("android", null))
        assertNull(appReviewUrl("unknown", "play"))
    }
}
