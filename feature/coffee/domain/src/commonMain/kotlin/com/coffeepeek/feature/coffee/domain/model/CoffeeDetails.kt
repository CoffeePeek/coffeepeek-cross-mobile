package com.coffeepeek.feature.coffee.domain.model

data class CoffeeDetails(
    val coffee: Coffee,
    val canonicalPath: String,
    val description: String?,
    val photos: List<String>,
    val roasterSlug: String?,
    val productKind: String?,
    val productForm: String?,
    val roastLevel: String?,
    val acidity: String?,
    val tasteDescriptors: List<String>,
    val checkedAtUtc: String?,
)
