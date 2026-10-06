package com.coffeepeek.api.model

import kotlinx.serialization.Serializable

@Serializable
data class PublicAddressDto(
    val slug: String,
    val canonicalPath: String,
    val revision: Int,
    val isAlias: Boolean,
)
