package com.coffeepeek.feature.shop.impl.ui.data

import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto

/** Platform-owned foreground extraction. Candidates are transparent PNGs, not classifications of cups. */
fun interface ShopDrinkPhotoProcessor {
    suspend fun process(source: ShopCheckInPhoto): Result<List<ShopCheckInPhoto>>
}
