@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.shop

import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import com.coffeepeek.feature.shop.domain.usecase.ShopCheckInValidationError
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInSubmitFailure
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage

/** Picker output is already prepared by the shared photo pipeline; only native presentation is decoded here. */
internal fun ShopCheckInPhoto.nativeImage(): UIImage? {
    if (bytes.isEmpty()) return null
    val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    return UIImage.imageWithData(data)
}

internal fun ShopCheckInValidationError.shopMessageKey() = when (this) {
    ShopCheckInValidationError.InvalidShop -> "shop_checkin_form_invalid_shop"
    ShopCheckInValidationError.InvalidVisitDate -> "shop_checkin_form_invalid_date"
    ShopCheckInValidationError.InvalidDrink -> "shop_checkin_form_invalid_drink"
    ShopCheckInValidationError.InvalidNote -> "shop_checkin_form_invalid_note"
    ShopCheckInValidationError.InvalidRating -> "shop_checkin_form_invalid_rating"
    ShopCheckInValidationError.TooManyPhotos -> "shop_checkin_form_invalid_photos"
    ShopCheckInValidationError.InvalidPhoto -> "shop_checkin_form_invalid_photo"
}

internal fun ShopCheckInSubmitFailure.shopMessageKey() = when (status) {
    401 -> "shop_checkin_error_unauthorized"
    403 -> "shop_checkin_error_forbidden"
    404 -> "shop_checkin_error_shop_gone"
    413 -> "shop_checkin_error_photos_too_large"
    429 -> "shop_checkin_error_too_many_requests"
    else -> "shop_checkin_form_submit_error"
}
