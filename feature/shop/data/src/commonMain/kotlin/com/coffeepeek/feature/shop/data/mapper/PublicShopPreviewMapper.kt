package com.coffeepeek.feature.shop.data.mapper

import com.coffeepeek.feature.shop.api.PublicShopCoordinates
import com.coffeepeek.feature.shop.api.PublicShopOpeningHours
import com.coffeepeek.feature.shop.api.PublicShopPreview
import com.coffeepeek.feature.shop.data.backend.ShopDetailsDto
import com.coffeepeek.feature.shop.domain.model.ShopSchedule

internal fun ShopDetailsDto.toPublicPreview(utcOffsetMinutes: Int): PublicShopPreview {
    val cover = photos.firstOrNull()?.let { it.urls?.card?.takeIf(String::isNotBlank) ?: it.fullUrl?.takeIf(String::isNotBlank) }
    val detail = photos.firstNotNullOfOrNull { it.urls?.detail?.takeIf(String::isNotBlank) ?: it.fullUrl?.takeIf(String::isNotBlank) }
    val latitude = location?.latitude
    val longitude = location?.longitude
    return PublicShopPreview(
        coverPhotoUrl = cover ?: detail,
        detailPhotoUrl = detail ?: cover,
        rating = rating.takeIf { it > 0 },
        reviewCount = checkInCount.toFlexibleInt(),
        isOpen = isOpen,
        hours = schedules.orEmpty().toLocalSchedules(utcOffsetMinutes).previewHours(),
        coordinates = if (latitude != null && longitude != null) PublicShopCoordinates(latitude, longitude) else null,
    )
}

private fun List<ShopSchedule>.previewHours(): PublicShopOpeningHours? {
    val schedule = firstOrNull { !it.isClosed && it.intervals.isNotEmpty() } ?: firstOrNull() ?: return null
    if (schedule.isClosed) return PublicShopOpeningHours(isClosed = true)
    val interval = schedule.intervals.firstOrNull() ?: return null
    return PublicShopOpeningHours(false, interval.openTime, interval.closeTime)
}
