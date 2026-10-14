package com.coffeepeek.admin.ui.screen.checkins

import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.admin.utils.currentUtcIsoDateTime
import com.coffeepeek.admin.utils.utcIsoToLocalDate
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.repository.CheckInRepository
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20
private const val CALENDAR_PAGE_SIZE = 100

enum class CheckInViewMode { Calendar, List }

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

class VisitedPlacesViewModel(
    private val checkInRepository: CheckInRepository,
    private val sessionRepository: SessionRepository,
) : BaseViewModel() {

    private val today get() = utcIsoToLocalDate(currentUtcIsoDateTime())
    private val currentMonth get() = calendarMonthFromIsoDate(today) ?: CalendarMonth(1970, 1)
    private val _state = MutableStateFlow(
        VisitedPlacesUiState(calendarMonth = currentMonth),
    )
    val state = _state.asStateFlow()
    private var calendarJob: Job? = null

    init { loadCalendarMonth(currentMonth) }

    fun changeMonth(offset: Int) {
        val target = _state.value.calendarMonth.plusMonths(offset)
        if (target.year * 12 + target.month > currentMonth.year * 12 + currentMonth.month) return
        _state.update {
            it.copy(
                calendarMonth = target,
                canGoNextMonth = target != currentMonth,
                calendarCheckIns = emptyMap(),
                selectedDate = null,
            )
        }
        loadCalendarMonth(target)
    }

    fun selectDate(date: String) {
        if (calendarMonthFromIsoDate(date) != _state.value.calendarMonth || date > today) return
        _state.update { it.copy(selectedDate = date) }
    }

    fun showToday() {
        val date = today
        val month = calendarMonthFromIsoDate(date) ?: return
        val changedMonth = _state.value.calendarMonth != month
        _state.update {
            it.copy(calendarMonth = month, selectedDate = date, canGoNextMonth = false,
                calendarCheckIns = if (changedMonth) emptyMap() else it.calendarCheckIns)
        }
        if (changedMonth || _state.value.calendarError != null) loadCalendarMonth(month)
    }

    fun refreshCalendar() = loadCalendarMonth(_state.value.calendarMonth)

    private fun loadCalendarMonth(month: CalendarMonth) {
        calendarJob?.cancel()
        calendarJob = workScope.launch {
            if (!sessionRepository.isLoggedIn()) {
                _state.update {
                    it.copy(calendarCheckIns = emptyMap(), isCalendarLoading = false, calendarError = null)
                }
                return@launch
            }
            _state.update { it.copy(isCalendarLoading = true, calendarError = null) }
            checkInRepository.getMyCheckIns(month.fromUtc, month.toUtc, CALENDAR_PAGE_SIZE)
                .onSuccess { checkIns ->
                    val grouped = checkIns.groupBy { checkIn ->
                        utcIsoToLocalDate(checkIn.visitedAt.ifBlank { checkIn.createdAt })
                    }
                    _state.update { state ->
                        if (state.calendarMonth != month) state else state.copy(
                            calendarCheckIns = grouped,
                            selectedDate = calendarSelection(month, today, state.selectedDate, grouped.keys),
                            isCalendarLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { state ->
                        if (state.calendarMonth != month) state else state.copy(
                            isCalendarLoading = false,
                            calendarError = error.message ?: "Ошибка загрузки",
                        )
                    }
                }
        }
    }

    fun load(reset: Boolean = false) {
        workScope.launch {
            // Logged-out users have no check-ins — show an empty list, not an error.
            if (!sessionRepository.isLoggedIn()) {
                _state.update {
                    it.copy(
                        checkIns = emptyList(),
                        isLoading = false,
                        isLoadingMore = false,
                        error = null,
                        hasMore = false,
                    )
                }
                return@launch
            }
            val page = if (reset) 1 else _state.value.currentPage + 1
            if (!reset && (!_state.value.hasMore || _state.value.isLoadingMore)) return@launch

            _state.update {
                it.copy(
                    isLoading = reset,
                    isLoadingMore = !reset,
                    error = null,
                )
            }

            checkInRepository.getMyCheckIns(page = page, pageSize = PAGE_SIZE)
                .onSuccess { result ->
                    _state.update { state ->
                        state.copy(
                            checkIns = if (reset) result.items else state.checkIns + result.items,
                            isLoading = false,
                            isLoadingMore = false,
                            currentPage = result.currentPage,
                            hasMore = result.currentPage < result.totalPages,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, isLoadingMore = false, error = e.message) }
                }
        }
    }

    fun loadMore() = load(reset = false)

    fun refresh() = load(reset = true)
}
