package com.coffeepeek.admin.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coffeepeek.admin.utils.BitmapUtil.toByteArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DisplayPhotoTransparencyTest {
    @Test
    fun downloadedPngKeepsTransparencyAndOriginalColorAfterDisplayPreparation() {
        val source = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        Canvas(source).drawCircle(20f, 20f, 10f, Paint().apply { color = Color.MAGENTA })
        val original = source.toByteArray(Bitmap.CompressFormat.PNG)
        val displayBytes = BitmapUtil.prepareForDisplay(original)
        val displayed = BitmapFactory.decodeByteArray(displayBytes, 0, displayBytes.size)
        assertTrue(displayed.hasAlpha())
        assertEquals(0, Color.alpha(displayed.getPixel(0, 0)))
        assertEquals(Color.MAGENTA, displayed.getPixel(20, 20))
        assertEquals(Color.MAGENTA, source.getPixel(20, 20))
        source.recycle()
        displayed.recycle()
    }

    @Test
    fun largeTransparentDownloadIsBoundedWithoutFlatteningItsBackground() {
        val source = Bitmap.createBitmap(2000, 100, Bitmap.Config.ARGB_8888)
        Canvas(source).drawCircle(1000f, 50f, 40f, Paint().apply { color = Color.BLUE })
        val bytes = BitmapUtil.prepareForDisplay(source.toByteArray(Bitmap.CompressFormat.PNG))
        val displayed = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertTrue(displayed.width <= BitmapUtil.DISPLAY_MAX_SIDE)
        assertEquals(0, Color.alpha(displayed.getPixel(0, 0)))
        assertEquals(Color.BLUE, displayed.getPixel(displayed.width / 2, displayed.height / 2))
        source.recycle()
        displayed.recycle()
    }
}
