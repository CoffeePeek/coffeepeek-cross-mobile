package com.coffeepeek.admin.di.shop

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.coffeepeek.feature.shop.domain.model.MAX_SHOP_DRINK_PHOTO_BYTES
import com.coffeepeek.feature.shop.domain.model.MAX_SHOP_DRINK_PHOTO_EDGE
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import com.coffeepeek.feature.shop.domain.usecase.isValidShopDrinkPhoto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Native PNG encoding guards only, not a claim about ML Kit model availability or cup detection quality. */
class AndroidShopDrinkPhotoProcessorTest {
    @Test fun displayDecoderAndReencodingRetainTransparentDrinkPixels() {
        val source = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.TRANSPARENT)
            setPixel(16, 16, Color.RED)
        }
        val encoded = java.io.ByteArrayOutputStream().also { source.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        val displayed = com.coffeepeek.admin.utils.BitmapUtil.load(encoded)
        val bytes = with(com.coffeepeek.admin.utils.BitmapUtil) { displayed.toByteArray() }
        val decoded = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
        try {
            assertEquals(0, Color.alpha(decoded.getPixel(0, 0)))
            assertEquals(Color.RED, decoded.getPixel(16, 16))
        } finally { source.recycle(); displayed.recycle(); decoded.recycle() }
    }

    @Test fun nativeEncoderKeepsVisiblePixelsTransparencyAndSizeWithoutChangingTheSource() {
        val source = Bitmap.createBitmap(1024, 512, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        try {
            val result = requireNotNull(source.toShopDrinkPhoto(2))
            assertEquals("drink_2.png", result.fileName)
            assertEquals("image/png", result.contentType)
            assertTrue(isValidShopDrinkPhoto(result))
            assertTrue(result.bytes.size <= MAX_SHOP_DRINK_PHOTO_BYTES)
            val decoded = requireNotNull(BitmapFactory.decodeByteArray(result.bytes, 0, result.bytes.size))
            try {
                assertEquals(MAX_SHOP_DRINK_PHOTO_EDGE, decoded.width)
                assertTrue(decoded.height in 1..MAX_SHOP_DRINK_PHOTO_EDGE)
                assertEquals(0, Color.alpha(decoded.getPixel(0, 0)))
                assertEquals(Color.RED, decoded.getPixel(decoded.width / 2, decoded.height / 2))
            } finally { decoded.recycle() }
            assertFalse(source.isRecycled)
            assertEquals(1024, source.width)
            assertEquals(512, source.height)
            assertEquals(Color.RED, source.getPixel(0, 0))
        } finally { source.recycle() }
    }

    @Test fun entirelyTransparentBitmapIsNotAUsableForegroundObject() {
        val source = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.TRANSPARENT) }
        try {
            assertNull(source.toShopDrinkPhoto(0))
            assertFalse(source.isRecycled)
        } finally { source.recycle() }
    }

    @Test fun damagedOriginalFailsBeforeRequestingTheSegmentationModel() = runBlocking {
        val original = byteArrayOf(1, 2, 3, 4)
        val result = AndroidShopDrinkPhotoProcessor().process(ShopCheckInPhoto(original, "broken.jpg"))
        assertTrue(result.isFailure)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), original)
    }
}
