package com.coffeepeek.feature.checkinhistory.domain.model

/**
 * Explicit instant bounds for an owner-history query. Composition derives them
 * from the local month and its time zone; domain does not guess a UTC offset.
 * The caller supplies validated ISO timestamps, with an exclusive upper bound.
 */
data class HistoryPeriod(val fromInclusive: String, val toExclusive: String) {
    init {
        require(fromInclusive.isNotBlank() && toExclusive.isNotBlank())
        require(fromInclusive != toExclusive)
    }
}
