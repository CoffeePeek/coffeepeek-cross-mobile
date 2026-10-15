package com.coffeepeek.feature.checkinhistory.impl.ui.compose.model

import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn

internal data class HistoryCalendarState(
    val month: HistoryMonth,
    val checkInsByDate: Map<String, List<PersonalCheckIn>> = emptyMap(),
    val selectedDate: String? = null,
    val hasLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
) {
    val selectedCheckIns: List<PersonalCheckIn> get() = checkInsByDate[selectedDate].orEmpty()
    val visitCount: Int get() = checkInsByDate.values.sumOf { it.size }
    val shopCount: Int get() = checkInsByDate.values.flatten().mapNotNull { it.shop?.slug }.distinct().size
}
