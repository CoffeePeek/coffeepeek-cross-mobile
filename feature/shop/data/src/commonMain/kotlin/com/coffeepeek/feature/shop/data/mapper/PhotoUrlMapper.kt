package com.coffeepeek.feature.shop.data.mapper

import com.coffeepeek.feature.shop.data.backend.ShopPhotoDto

internal fun ShopPhotoDto.fullScreenUrl(): String? =
    urls?.fullscreen?.takeIf(String::isNotBlank) ?: fullUrl?.takeIf(String::isNotBlank)

internal fun ShopPhotoDto.detailUrl(fallback: String): String =
    urls?.detail?.takeIf(String::isNotBlank) ?: fallback
