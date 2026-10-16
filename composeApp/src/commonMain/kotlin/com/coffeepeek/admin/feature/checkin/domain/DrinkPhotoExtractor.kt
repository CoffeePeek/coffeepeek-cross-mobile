package com.coffeepeek.admin.feature.checkin.domain

import com.coffeepeek.domain.model.PendingPhotoUpload

internal fun interface DrinkPhotoExtractor {
    /** Returns separate foreground objects as cropped transparent PNGs, or no candidates. */
    suspend fun extract(photo: PendingPhotoUpload): List<PendingPhotoUpload>
}
