package com.coffeepeek.feature.shop.domain.model

data class ShopCoffeeDetails(
    val beans: List<String> = emptyList(),
    val roasters: List<ShopRoaster> = emptyList(),
    val equipment: List<String> = emptyList(),
)

data class ShopRoaster(
    val id: String,
    val name: String,
    val photoUrl: String?,
    val canonicalPath: String? = null,
)
