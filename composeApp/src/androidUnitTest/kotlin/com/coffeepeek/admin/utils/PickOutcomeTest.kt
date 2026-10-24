package com.coffeepeek.admin.utils

import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PickOutcomeTest {
    private fun image(name: String) = PickedImage(byteArrayOf(1), name)

    @Test fun everyPreparedPhotoIsKeptAndNothingIsCountedAsFailed() {
        val outcome = pickOutcome(listOf("a", "b"), limit = 5) { image(it) }
        assertEquals(listOf("a", "b"), outcome.images.map { it.fileName })
        assertEquals(0, outcome.failed)
    }

    @Test fun unreadableAndFailingPhotosAreCountedWhileTheRestSurvive() {
        val outcome = pickOutcome(listOf("a", "unreadable", "boom", "d"), limit = 5) { item ->
            when (item) { "unreadable" -> null; "boom" -> error("decode failed"); else -> image(item) }
        }
        assertEquals(listOf("a", "d"), outcome.images.map { it.fileName })
        assertEquals(2, outcome.failed)
    }

    @Test fun onlyTheChosenLimitIsPreparedAndCounted() {
        val outcome = pickOutcome(listOf("a", "b", "c"), limit = 2) { if (it == "b") null else image(it) }
        assertEquals(1, outcome.failed)
        assertEquals(listOf("a"), outcome.images.map { it.fileName })
    }

    @Test fun cancellationIsNotSwallowedAsAFailedPhoto() {
        assertFailsWith<CancellationException> { pickOutcome(listOf("a"), 1) { throw CancellationException("stop") } }
    }
}
