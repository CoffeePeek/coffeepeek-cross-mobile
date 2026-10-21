package com.coffeepeek.feature.homecheckin.domain.model

/** Original photo bytes; signed URLs and storage keys are data-layer details. */
data class HomePreparationUpload(val fileName: String, val contentType: String, val bytes: ByteArray)
