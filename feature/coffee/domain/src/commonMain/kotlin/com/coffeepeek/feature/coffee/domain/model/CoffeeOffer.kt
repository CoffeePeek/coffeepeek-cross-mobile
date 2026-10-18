package com.coffeepeek.feature.coffee.domain.model

data class CoffeeOffer(
    val price: Double?,
    val currency: String?,
    val weightGrams: Int?,
    val key: String? = null,
    val availability: CoffeeAvailability = CoffeeAvailability.Unknown,
    val sellerName: String? = null,
    val sourceUrl: String? = null,
    val grind: String? = null,
    val brewPurpose: String? = null,
    val availabilityScope: String? = null,
    val checkedAtUtc: String? = null,
)
