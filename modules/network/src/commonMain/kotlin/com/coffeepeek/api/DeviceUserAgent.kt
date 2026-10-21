package com.coffeepeek.api

internal expect fun deviceUserAgent(): String

internal fun formatDeviceUserAgent(model: String, system: String): String =
    "CoffeePeek (${model.headerPart(160)}; ${system.headerPart(64)})"

private fun String.headerPart(limit: Int): String =
    map { if (it in ' '..'~' && it !in "();\\") it else ' ' }
        .joinToString("").trim().take(limit).ifBlank { "Unknown" }

internal fun androidDeviceModel(manufacturer: String, model: String): String =
    if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model".trim()

// Hardware identifiers and product names: https://github.com/devicekit/DeviceKit/blob/master/Source/Device.generated.swift
internal fun appleDeviceModel(identifier: String): String = when (identifier) {
    "iPhone8,1" -> "iPhone 6s"
    "iPhone8,2" -> "iPhone 6s Plus"
    "iPhone8,4" -> "iPhone SE"
    "iPhone9,1", "iPhone9,3" -> "iPhone 7"
    "iPhone9,2", "iPhone9,4" -> "iPhone 7 Plus"
    "iPhone10,1", "iPhone10,4" -> "iPhone 8"
    "iPhone10,2", "iPhone10,5" -> "iPhone 8 Plus"
    "iPhone10,3", "iPhone10,6" -> "iPhone X"
    "iPhone11,2" -> "iPhone XS"
    "iPhone11,4", "iPhone11,6" -> "iPhone XS Max"
    "iPhone11,8" -> "iPhone XR"
    "iPhone12,1" -> "iPhone 11"
    "iPhone12,3" -> "iPhone 11 Pro"
    "iPhone12,5" -> "iPhone 11 Pro Max"
    "iPhone12,8" -> "iPhone SE 2"
    "iPhone13,1" -> "iPhone 12 mini"
    "iPhone13,2" -> "iPhone 12"
    "iPhone13,3" -> "iPhone 12 Pro"
    "iPhone13,4" -> "iPhone 12 Pro Max"
    "iPhone14,2" -> "iPhone 13 Pro"
    "iPhone14,3" -> "iPhone 13 Pro Max"
    "iPhone14,4" -> "iPhone 13 mini"
    "iPhone14,5" -> "iPhone 13"
    "iPhone14,6" -> "iPhone SE 3"
    "iPhone14,7" -> "iPhone 14"
    "iPhone14,8" -> "iPhone 14 Plus"
    "iPhone15,2" -> "iPhone 14 Pro"
    "iPhone15,3" -> "iPhone 14 Pro Max"
    "iPhone15,4" -> "iPhone 15"
    "iPhone15,5" -> "iPhone 15 Plus"
    "iPhone16,1" -> "iPhone 15 Pro"
    "iPhone16,2" -> "iPhone 15 Pro Max"
    "iPhone17,1" -> "iPhone 16 Pro"
    "iPhone17,2" -> "iPhone 16 Pro Max"
    "iPhone17,3" -> "iPhone 16"
    "iPhone17,4" -> "iPhone 16 Plus"
    "iPhone17,5" -> "iPhone 16e"
    "iPhone18,1" -> "iPhone 17 Pro"
    "iPhone18,2" -> "iPhone 17 Pro Max"
    "iPhone18,3" -> "iPhone 17"
    "iPhone18,4" -> "iPhone Air"
    "iPhone18,5" -> "iPhone 17e"
    "iPhone19,2" -> "iPhone 18 Pro"
    "iPhone19,3", "iPhone19,7" -> "iPhone 18 Pro Max"
    "iPod9,1" -> "iPod touch 7"
    "i386", "x86_64", "arm64" -> "iOS Simulator"
    else -> identifier
}
