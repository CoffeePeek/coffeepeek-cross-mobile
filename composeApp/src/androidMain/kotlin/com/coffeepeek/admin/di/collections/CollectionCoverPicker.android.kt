package com.coffeepeek.admin.di.collections

import android.graphics.Bitmap
import com.coffeepeek.core.media.AndroidImageCodec
import com.coffeepeek.core.media.AndroidImageCodec.changeSize
import java.io.ByteArrayOutputStream

internal actual suspend fun prepareCollectionCover(bytes: ByteArray): ByteArray {
    val decoded = AndroidImageCodec.decodeOrientedBitmap(bytes, decodeMaxSide = 2560)
    val sized = decoded.changeSize(1280)
    try {
        for (quality in 90 downTo 40 step 10) {
            val stream = ByteArrayOutputStream()
            check(sized.compress(Bitmap.CompressFormat.JPEG, quality, stream))
            val encoded = stream.toByteArray()
            if (encoded.size <= 1024 * 1024) return encoded
        }
        error("Collection cover exceeds its byte budget")
    } finally {
        if (sized !== decoded) sized.recycle()
        decoded.recycle()
    }
}
