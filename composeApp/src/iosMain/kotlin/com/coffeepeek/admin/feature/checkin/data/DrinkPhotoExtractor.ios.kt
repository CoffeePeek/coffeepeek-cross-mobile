@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.coffeepeek.admin.feature.checkin.data

import com.coffeepeek.admin.feature.checkin.api.IosDrinkPhotoRegistry
import com.coffeepeek.admin.feature.checkin.domain.DrinkPhotoExtractor
import com.coffeepeek.domain.model.PendingPhotoUpload
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy

internal actual fun createDrinkPhotoExtractor(): DrinkPhotoExtractor = DrinkPhotoExtractor { photo ->
    withContext(Dispatchers.Default) {
        val provider = IosDrinkPhotoRegistry.provider ?: error("Выделение чашки недоступно")
        require(photo.bytes.isNotEmpty()) { "Фото пустое" }
        autoreleasepool {
            val data = photo.bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = photo.bytes.size.toULong()) }
            val result = provider.extract(data)
            ensureActive()
            result.error?.let { error(it) }
            result.photos.mapIndexed { index, png ->
                require(png.length in 1UL..4_194_304UL) { "Недопустимый размер изображения чашки" }
                val bytes = ByteArray(png.length.toInt()).also { output ->
                    output.usePinned { memcpy(it.addressOf(0), png.bytes, png.length) }
                }
                PendingPhotoUpload("drink_$index.png", "image/png", bytes)
            }
        }
    }
}
