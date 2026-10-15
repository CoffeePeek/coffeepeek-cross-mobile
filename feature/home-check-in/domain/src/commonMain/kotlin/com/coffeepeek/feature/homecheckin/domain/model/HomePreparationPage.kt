package com.coffeepeek.feature.homecheckin.domain.model

data class HomePreparationPage(
    val items: List<HomePreparation>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
) {
    init {
        require(totalCount >= 0 && page in 1..10000 && pageSize in 1..100)
        require(items.size <= pageSize)
    }

    val hasMore: Boolean get() = page.toLong() * pageSize < totalCount
}

data class HomePreparationDay(val preparedOn: String, val brewCount: Int) {
    init {
        HomePreparationDateRange(preparedOn, preparedOn)
        require(brewCount >= 0)
    }
}
