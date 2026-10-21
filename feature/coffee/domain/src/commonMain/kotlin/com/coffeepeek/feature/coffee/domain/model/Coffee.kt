package com.coffeepeek.feature.coffee.domain.model

data class Coffee(
    val slug: String,
    val name: String,
    val photoUrl: String?,
    val roasterName: String,
    val roasterPhotoUrl: String?,
    val countries: List<String>,
    val tasteCodes: List<String>,
    val offers: List<CoffeeOffer>,
)
