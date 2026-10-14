package com.coffeepeek.feature.shop.domain.usecase

import com.coffeepeek.feature.shop.domain.model.MAX_SHOP_DRINK_PHOTO_BYTES
import com.coffeepeek.feature.shop.domain.model.MAX_SHOP_DRINK_PHOTO_EDGE
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto

/** Header/size guard only. The platform encoder and server validate pixels and complete PNG content. */
fun isValidShopDrinkPhoto(photo: ShopCheckInPhoto): Boolean {
    if (photo.contentType != "image/png" || photo.fileName.isBlank() ||
        photo.bytes.size !in 33..MAX_SHOP_DRINK_PHOTO_BYTES) return false
    val bytes = photo.bytes
    val signature = listOf(137, 80, 78, 71, 13, 10, 26, 10)
    if (signature.indices.any { (bytes[it].toInt() and 255) != signature[it] }) return false
    if (bytes.readUnsignedInt(8) != 13L || (12..15).map { bytes[it].toInt().toChar() }.joinToString("") != "IHDR") return false
    return bytes.readUnsignedInt(16) in 1L..MAX_SHOP_DRINK_PHOTO_EDGE.toLong() &&
        bytes.readUnsignedInt(20) in 1L..MAX_SHOP_DRINK_PHOTO_EDGE.toLong()
}

private fun ByteArray.readUnsignedInt(offset: Int): Long = (offset until offset + 4)
    .fold(0L) { value, index -> (value shl 8) or (this[index].toLong() and 255L) }
