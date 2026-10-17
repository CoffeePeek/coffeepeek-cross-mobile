package com.coffeepeek.feature.homecheckin.impl.ui.data

import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationUpload

/** Adapter to the existing platform extractor; candidates are transparent PNGs, not original photos. */
fun interface HomeCheckInDrinkPhotoProcessor {
    suspend fun process(source: HomePreparationUpload): Result<List<HomePreparationUpload>>
}
