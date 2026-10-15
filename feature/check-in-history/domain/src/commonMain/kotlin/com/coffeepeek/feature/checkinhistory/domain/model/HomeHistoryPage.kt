package com.coffeepeek.feature.checkinhistory.domain.model

data class HomeHistoryPage(
    val items: List<HomeHistoryEntry>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
) {
    init {
        require(totalCount >= 0 && page in 1..10000 && pageSize in 1..100 && items.size <= pageSize)
    }

    val hasMore: Boolean get() = page.toLong() * pageSize < totalCount
}

data class HomeHistoryDay(val preparedOn: String, val brewCount: Int) {
    init {
        require(HistoryMonth.fromDate(preparedOn) != null && brewCount >= 0)
    }
}
