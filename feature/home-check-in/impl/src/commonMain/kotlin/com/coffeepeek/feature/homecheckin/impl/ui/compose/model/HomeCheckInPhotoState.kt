package com.coffeepeek.feature.homecheckin.impl.ui.compose.model

import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationUpload

internal data class HomeCheckInPhotoState(
    val selected: HomePreparationUpload? = null,
    val candidates: List<HomePreparationUpload> = emptyList(),
    val isProcessing: Boolean = false,
    val isSkipped: Boolean = false,
    val hasError: Boolean = false,
    val source: HomePreparationUpload? = null,
) {
    val canSubmit: Boolean get() = !isProcessing && (candidates.size <= 1 || selected != null || isSkipped)
}
