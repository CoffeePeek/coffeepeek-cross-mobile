package com.coffeepeek.feature.checkinhistory.domain.model

data class CheckInHistoryPage(
    val items: List<PersonalCheckIn>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
) {
    init {
        require(totalCount >= 0 && page >= 1 && pageSize in 1..100)
    }

    val totalPages: Int get() = ((totalCount.toLong() + pageSize - 1) / pageSize).toInt()
    val hasMore: Boolean get() = page.toLong() * pageSize < totalCount
}
