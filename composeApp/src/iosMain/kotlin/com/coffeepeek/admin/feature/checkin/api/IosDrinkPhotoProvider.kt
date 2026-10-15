package com.coffeepeek.admin.feature.checkin.api

import platform.Foundation.NSData

// Vision's iOS 17 classes stay in Swift to avoid strong linking on iOS 15–16.
interface IosDrinkPhotoProvider {
    fun extract(photo: NSData): IosDrinkPhotoResult
}

class IosDrinkPhotoResult(val photos: List<NSData>, val error: String?)

object IosDrinkPhotoRegistry {
    var provider: IosDrinkPhotoProvider? = null
}
