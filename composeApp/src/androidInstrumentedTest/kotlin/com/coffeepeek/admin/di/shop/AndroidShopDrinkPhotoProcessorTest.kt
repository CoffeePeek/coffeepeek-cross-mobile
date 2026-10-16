package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.feature.checkin.domain.DrinkPhotoExtractor
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.*

/** The model itself is exercised by main's DrinkPhotoExtractorAndroidTest. */
class AndroidShopDrinkPhotoProcessorTest {
    @Test fun delegatesBytesAndPreservesCandidateOrderAndNames() = runBlocking {
        val source = ShopCheckInPhoto(byteArrayOf(1, 2), "source.jpg")
        val processor = AndroidShopDrinkPhotoProcessor(DrinkPhotoExtractor {
            assertContentEquals(source.bytes, it.bytes)
            assertEquals("source.jpg", it.fileName)
            listOf(PendingPhotoUpload("cup.png", "image/png", byteArrayOf(3)))
        })
        val result = processor.process(source).getOrThrow().single()
        assertEquals("cup.png", result.fileName)
        assertContentEquals(byteArrayOf(3), result.bytes)
        assertContentEquals(byteArrayOf(1, 2), source.bytes)
    }

    @Test fun emptyCandidatesAndFailureRemainExplicit() = runBlocking {
        val source = ShopCheckInPhoto(byteArrayOf(1), "source.jpg")
        assertTrue(AndroidShopDrinkPhotoProcessor(DrinkPhotoExtractor { emptyList() }).process(source).getOrThrow().isEmpty())
        assertTrue(AndroidShopDrinkPhotoProcessor(DrinkPhotoExtractor { error("Invalid fixture") }).process(source).isFailure)
    }

    @Test fun cancellationIsNeverAResultFailure() = runBlocking<Unit> {
        assertFailsWith<CancellationException> {
            AndroidShopDrinkPhotoProcessor(DrinkPhotoExtractor { throw CancellationException("Cancelled") })
                .process(ShopCheckInPhoto(byteArrayOf(1), "source.jpg"))
        }
    }
}
