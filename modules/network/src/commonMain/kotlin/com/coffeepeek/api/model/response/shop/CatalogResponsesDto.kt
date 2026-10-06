package com.coffeepeek.api.model.response.shop

import com.coffeepeek.api.model.PublicAddressDto
import kotlinx.serialization.Serializable

@Serializable
data class CityItemDto(
    val address: PublicAddressDto,
    val name: String,
)
