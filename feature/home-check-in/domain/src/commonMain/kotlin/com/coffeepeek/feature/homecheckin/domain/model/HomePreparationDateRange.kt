package com.coffeepeek.feature.homecheckin.domain.model

/** Civil-day selection. Transport must preserve the existing Home from/to convention. */
data class HomePreparationDateRange(val from: String, val to: String) {
    init {
        require(isCivilDate(from) && isCivilDate(to) && from <= to)
    }
}

private fun isCivilDate(value: String): Boolean {
    if (value.length != 10 || value[4] != '-' || value[7] != '-') return false
    if (value.indices.any { it != 4 && it != 7 && value[it] !in '0'..'9' }) return false
    val year = value.substring(0, 4).toInt()
    val month = value.substring(5, 7).toInt()
    val day = value.substring(8, 10).toInt()
    if (year !in 1..9999 || month !in 1..12) return false
    val days = when (month) {
        2 -> if (year % 400 == 0 || year % 4 == 0 && year % 100 != 0) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
    return day in 1..days
}
