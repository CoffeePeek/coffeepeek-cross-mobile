package com.coffeepeek.feature.checkinhistory.domain.model

/** A local civil month. Query instants must be supplied by a zone-aware caller. */
data class HistoryMonth(val year: Int, val month: Int) {
    init {
        require(year in 1..9999)
        require(month in 1..12)
    }

    val daysInMonth: Int
        get() = when (month) {
            2 -> if (year % 400 == 0 || year % 4 == 0 && year % 100 != 0) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }

    fun plusMonths(offset: Int): HistoryMonth {
        val absolute = year.toLong() * 12 + month - 1 + offset.toLong()
        val nextYear = absolute.floorDiv(12)
        require(nextYear in 1L..9999L)
        return HistoryMonth(nextYear.toInt(), absolute.mod(12) + 1)
    }

    fun date(day: Int): String {
        require(day in 1..daysInMonth)
        return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }

    fun containsDate(value: String): Boolean = fromDate(value) == this

    companion object {
        /** Strict ISO local date, not a timestamp or a partially parsed date. */
        fun fromDate(value: String): HistoryMonth? {
            if (value.length != 10 || value[4] != '-' || value[7] != '-') return null
            if (value.indices.any { it != 4 && it != 7 && value[it] !in '0'..'9' }) return null
            val year = value.substring(0, 4).toInt()
            val month = value.substring(5, 7).toInt()
            val day = value.substring(8, 10).toInt()
            if (year !in 1..9999 || month !in 1..12) return null
            return HistoryMonth(year, month).takeIf { day in 1..it.daysInMonth }
        }
    }
}
