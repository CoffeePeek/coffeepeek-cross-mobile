package com.coffeepeek.api.model.response

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ConsumedDrinkOptionDto(
    val slug: String,
    val nameRu: String = "",
    val nameEn: String = "",
    val category: JsonElement? = null,
)
