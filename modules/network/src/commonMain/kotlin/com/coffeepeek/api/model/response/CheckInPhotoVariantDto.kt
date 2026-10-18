package com.coffeepeek.api.model.response

import kotlinx.serialization.Serializable

@Serializable
data class CheckInPhotoVariantDto(
    val purpose: String = "",
    val format: String = "",
    val contentType: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0,
    val sourceVersion: String = "",
    val recipeVersion: Int = 0,
    val url: String = "",
)
