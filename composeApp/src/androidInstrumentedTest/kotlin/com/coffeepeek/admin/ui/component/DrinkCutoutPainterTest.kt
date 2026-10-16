package com.coffeepeek.admin.ui.component

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrinkCutoutPainterTest {
    @Test
    fun outlineFollowsTheObjectAndKeepsTheBackgroundTransparent() {
        val original = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(original).drawCircle(10f, 10f, 6f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLUE })
        val result = render(original)
        assertEquals(android.graphics.Color.BLUE, result.getPixel(30, 30))
        val edge = result.getPixel(12, 30)
        assertEquals(255, android.graphics.Color.alpha(edge))
        // The antialiased blue object can overlap the inner edge by a fraction of a pixel.
        assertTrue(android.graphics.Color.red(edge) >= 245 && android.graphics.Color.green(edge) >= 245 &&
            android.graphics.Color.blue(edge) >= 245)
        assertEquals(0, android.graphics.Color.alpha(result.getPixel(0, 0)))
        assertEquals(0, android.graphics.Color.alpha(result.getPixel(5, 30)))
        assertEquals("Rendering must not change the original", android.graphics.Color.BLUE, original.getPixel(10, 10))
        original.recycle()
        result.recycle()
    }

    @Test
    fun tightlyCroppedObjectStillHasAnOutlineInsideItsBounds() {
        val original = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.BLUE)
        }
        val result = render(original)
        assertEquals(android.graphics.Color.BLUE, result.getPixel(30, 30))
        assertEquals(android.graphics.Color.WHITE, result.getPixel(1, 30))
        assertTrue(android.graphics.Color.alpha(result.getPixel(0, 0)) < 255)
        original.recycle()
        result.recycle()
    }

    private fun render(original: Bitmap): Bitmap {
        val target = ImageBitmap(60, 60)
        val painter = DrinkCutoutPainter(BitmapPainter(original.asImageBitmap()), 3f)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(target), Size(60f, 60f)) {
            with(painter) { draw(size) }
        }
        return target.asAndroidBitmap()
    }
}
