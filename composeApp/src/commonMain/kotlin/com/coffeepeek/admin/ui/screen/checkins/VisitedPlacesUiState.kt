package com.coffeepeek.admin.ui.screen.checkins

import com.coffeepeek.domain.model.CheckIn

data class VisitedPlacesUiState(
    val checkIns: List<CheckIn> = emptyList(),
    val calendarCheckIns: Map<String, List<CheckIn>> = emptyMap(),
    val calendarMonth: CalendarMonth = CalendarMonth(1970, 1),
    val canGoNextMonth: Boolean = false,
    val selectedDate: String? = null,
    val isLoading: Boolean = false,
    val isCalendarLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val calendarError: String? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = false,
)
