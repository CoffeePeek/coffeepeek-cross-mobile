package com.coffeepeek.feature.checkinhistory.impl.ui.data

import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryEntry
import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoStripSources

internal fun PersonalCheckIn.timelinePhotos() = CoffeePeekPhotoStripSources(photoUrls, photoThumbnailUrls, photoDetailUrls)

internal fun HomeHistoryEntry.timelinePhotos() = CoffeePeekPhotoStripSources(
    photos.map { it.url }, photos.map { it.thumbnailUrl ?: it.url }, photos.map { it.detailUrl ?: it.url },
)
