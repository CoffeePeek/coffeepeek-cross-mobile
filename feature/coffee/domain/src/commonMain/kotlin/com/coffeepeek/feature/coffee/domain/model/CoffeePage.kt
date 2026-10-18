package com.coffeepeek.feature.coffee.domain.model

data class CoffeePage(
    val items: List<Coffee>,
    val currentPage: Int,
    val totalPages: Int,
    val totalCount: Int? = null,
)
