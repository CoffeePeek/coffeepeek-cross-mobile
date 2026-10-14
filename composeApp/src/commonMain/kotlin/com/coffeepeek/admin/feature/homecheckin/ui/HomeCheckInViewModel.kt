package com.coffeepeek.admin.feature.homecheckin.ui

import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.admin.feature.catalog.api.BrewMethodCatalog
import com.coffeepeek.admin.feature.catalog.api.BrewMethodOption
import com.coffeepeek.admin.feature.coffee.api.CoffeePicker
import com.coffeepeek.admin.feature.coffee.api.CoffeePickerItem
import com.coffeepeek.admin.utils.currentEpochMillis
import com.coffeepeek.admin.utils.utcIsoToLocalDate
import com.coffeepeek.feature.homecheckin.domain.HomeBeanStats
import com.coffeepeek.feature.homecheckin.domain.HomeCalendarDay
import com.coffeepeek.feature.homecheckin.domain.HomeCheckIn
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInInput
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInVisibility
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInRepository
import com.coffeepeek.feature.homecheckin.domain.HomeMethodStats
import com.coffeepeek.feature.homecheckin.domain.homeCheckInFieldErrors
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInField
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

internal data class HomeMonth(val year: Int, val month: Int) {
    fun change(offset: Int): HomeMonth {
        val total = year * 12 + month - 1 + offset
        return HomeMonth(total.floorDiv(12), total.mod(12) + 1)
    }
    fun date(day: Int) = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    val from: String get() = date(1)
    val to: String get() = date(daysInMonth(year, month))
    val mondayOffset: Int get() {
        val adjustedYear = if (month < 3) year - 1 else year
        val offsets = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
        val sundayFirst = (adjustedYear + adjustedYear / 4 - adjustedYear / 100 +
            adjustedYear / 400 + offsets[month - 1] + 1) % 7
        return (sundayFirst + 6) % 7
    }
}

internal fun daysInMonth(year: Int, month: Int) = when (month) {
    2 -> if (year % 400 == 0 || year % 4 == 0 && year % 100 != 0) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

internal data class HomeCheckInUiState(
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val items: List<HomeCheckIn> = emptyList(),
    val page: Int = 1,
    val hasMore: Boolean = false,
    val month: HomeMonth,
    val calendar: List<HomeCalendarDay> = emptyList(),
    val calendarLoading: Boolean = false,
    val calendarError: String? = null,
    val selectedDate: String? = null,
    val beanStats: List<HomeBeanStats> = emptyList(),
    val methodStats: List<HomeMethodStats> = emptyList(),
    val selected: HomeCheckIn? = null,
    val detailLoading: Boolean = false,
    val editing: Boolean = false,
    val draft: HomeCheckInInput,
    val saving: Boolean = false,
    val formError: String? = null,
    val fieldErrors: Map<HomeCheckInField, String> = emptyMap(),
    val saveNotice: String? = null,
    val deleteConfirmation: Boolean = false,
    val catalogQuery: String = "",
    val catalogSelectionName: String? = null,
    val catalogItems: List<CoffeePickerItem> = emptyList(),
    val catalogLoading: Boolean = false,
    val catalogError: String? = null,
    val brewMethods: List<BrewMethodOption> = emptyList(),
    val brewMethodsLoading: Boolean = false,
    val brewMethodsError: Boolean = false,
)

internal class HomeCheckInViewModel(
    private val repository: HomeCheckInRepository,
    private val coffeePicker: CoffeePicker,
    private val brewMethodCatalog: BrewMethodCatalog,
) : BaseViewModel() {
    private val now = currentHomePreparedAt()
    private val today = utcIsoToLocalDate(now)
    private val todayMonth = HomeMonth(today.substring(0, 4).toInt(), today.substring(5, 7).toInt())
    private val _state = MutableStateFlow(HomeCheckInUiState(
        month = todayMonth,
        draft = HomeCheckInInput(preparedAt = now, preparedOn = today, tasteScore = 5),
    ))
    val state = _state.asStateFlow()
    private var historyJob: Job? = null
    private var calendarJob: Job? = null
    private var catalogJob: Job? = null
    private var methodsJob: Job? = null
    private var statsRequested = false

    init { refresh() }

    fun refresh() {
        loadHistory(reset = true)
        loadCalendar()
        if (statsRequested) loadStats()
    }

    fun loadMore() = loadHistory(reset = false)

    private fun loadHistory(reset: Boolean) {
        if (!reset && (!_state.value.hasMore || _state.value.loadingMore)) return
        if (reset) historyJob?.cancel()
        val page = if (reset) 1 else _state.value.page + 1
        val selectedDate = _state.value.selectedDate
        _state.update { it.copy(loading = reset, loadingMore = !reset, error = null) }
        historyJob = workScope.launch {
            repository.history(page = page, from = selectedDate, to = selectedDate)
                .onSuccess { result ->
                    _state.update { old -> if (old.selectedDate != selectedDate) old else old.copy(
                        loading = false, loadingMore = false, page = result.page,
                        hasMore = result.hasMore,
                        items = if (reset) result.items else (old.items + result.items).distinctBy { it.id },
                    ) }
                }
                .onFailure { error -> _state.update {
                    if (it.selectedDate != selectedDate) it
                    else it.copy(loading = false, loadingMore = false, error = error.message)
                } }
        }
    }

    fun changeMonth(offset: Int) {
        val next = _state.value.month.change(offset)
        if (next.year * 12 + next.month > todayMonth.year * 12 + todayMonth.month) return
        _state.update { it.copy(month = next, selectedDate = null, calendar = emptyList()) }
        loadCalendar()
        loadHistory(reset = true)
    }

    fun selectDate(date: String?) {
        _state.update { it.copy(selectedDate = if (it.selectedDate == date) null else date) }
        loadHistory(reset = true)
    }

    fun loadCalendar() {
        val month = _state.value.month
        calendarJob?.cancel()
        _state.update { it.copy(calendarLoading = true, calendarError = null) }
        calendarJob = workScope.launch {
            repository.calendar(month.from, month.to)
                .onSuccess { days -> _state.update { if (it.month == month) it.copy(calendar = days, calendarLoading = false) else it } }
                .onFailure { error -> _state.update { if (it.month == month) it.copy(calendarError = error.message ?: "Не удалось загрузить календарь", calendarLoading = false) else it } }
        }
    }

    fun loadStats() {
        statsRequested = true
        workScope.launch {
            repository.beanStats()
                .onSuccess { stats -> _state.update { it.copy(beanStats = stats) } }
                .onFailure { error -> _state.update { it.copy(error = error.message) } }
            repository.methodStats()
                .onSuccess { stats -> _state.update { it.copy(methodStats = stats) } }
                .onFailure { error -> _state.update { it.copy(error = error.message) } }
        }
    }

    fun open(id: String, editOnLoad: Boolean = false) {
        _state.update { it.copy(detailLoading = true, error = null, saveNotice = null) }
        workScope.launch {
            repository.detail(id)
                .onSuccess { entry ->
                    _state.update { it.copy(selected = entry, detailLoading = false) }
                    if (editOnLoad) edit()
                }
                .onFailure { error -> _state.update { it.copy(detailLoading = false, error = error.message) } }
        }
    }

    fun closeDetail() = _state.update { it.copy(selected = null, editing = false, saveNotice = null) }

    fun create() {
        catalogJob?.cancel()
        val preparedAt = currentHomePreparedAt()
        _state.update { it.copy(
            editing = true, selected = null, formError = null, fieldErrors = emptyMap(), saveNotice = null,
            catalogQuery = "", catalogItems = emptyList(), catalogSelectionName = null,
            catalogLoading = false, catalogError = null,
            draft = HomeCheckInInput(preparedAt = preparedAt, preparedOn = utcIsoToLocalDate(preparedAt), tasteScore = 5),
        ) }
        loadBrewMethods()
    }

    fun edit() {
        val entry = _state.value.selected ?: return
        catalogJob?.cancel()
        _state.update { it.copy(
            editing = true, formError = null, fieldErrors = emptyMap(), saveNotice = null,
            catalogQuery = "", catalogItems = emptyList(), catalogSelectionName = entry.beanName,
            catalogLoading = false, catalogError = null,
            draft = HomeCheckInInput(
                preparedAt = entry.preparedAtUtc, preparedOn = entry.preparedOn,
                brewMethodCode = entry.brewMethodCode,
                coffeeProductId = entry.coffeeProductId,
                customBeanName = if (entry.coffeeProductId == null) entry.beanName else null,
                doseGrams = entry.doseGrams, brewDurationSeconds = entry.brewDurationSeconds,
                tasteScore = entry.tasteScore, text = entry.text,
                drinkSlug = entry.drinkSlug ?: "other".takeIf { !entry.customDrinkName.isNullOrBlank() || !entry.drinkName.isNullOrBlank() },
                customDrinkName = entry.customDrinkName ?: entry.drinkName.takeIf { entry.drinkSlug == null },
                visibility = entry.visibility,
            ),
        ) }
        loadBrewMethods()
    }

    fun updateDraft(change: (HomeCheckInInput) -> HomeCheckInInput) {
        if (_state.value.saving) return
        _state.update { it.withDraft(change(it.draft)) }
    }

    fun loadBrewMethods() {
        if (_state.value.brewMethods.isNotEmpty()) return
        methodsJob?.cancel()
        _state.update { it.copy(brewMethodsLoading = true, brewMethodsError = false) }
        methodsJob = workScope.launch {
            val result = brewMethodCatalog.load()
            coroutineContext.ensureActive()
            result.onSuccess { methods -> _state.update { it.copy(brewMethods = methods, brewMethodsLoading = false) } }
                .onFailure { _state.update { it.copy(brewMethodsLoading = false, brewMethodsError = true) } }
        }
    }

    fun setCatalogQuery(query: String) {
        _state.update { it.copy(catalogQuery = query.take(100), catalogError = null) }
        searchCatalog(debounce = true)
    }

    fun searchCatalog() = searchCatalog(debounce = false)

    private fun searchCatalog(debounce: Boolean) {
        catalogJob?.cancel()
        val query = _state.value.catalogQuery.trim()
        _state.update { it.copy(catalogLoading = true, catalogError = null, catalogItems = emptyList()) }
        catalogJob = workScope.launch {
            if (debounce) delay(300)
            val result = coffeePicker.search(query)
            coroutineContext.ensureActive()
            result
                .onSuccess { items -> _state.update { it.copy(catalogItems = items, catalogLoading = false) } }
                .onFailure { error -> _state.update { it.copy(catalogError = error.message, catalogLoading = false) } }
        }
    }

    fun selectCatalogProduct(product: CoffeePickerItem) {
        catalogJob?.cancel()
        _state.update { it.withDraft(it.draft.copy(coffeeProductSlug = product.slug,
            coffeeProductId = null, customBeanName = null)).copy(
            catalogLoading = false, catalogError = null, catalogSelectionName = product.name,
        ) }
    }

    fun closeEditor() = _state.update { it.copy(editing = false, formError = null, fieldErrors = emptyMap()) }

    fun save() {
        val snapshot = _state.value
        if (snapshot.saving) return
        val errors = homeCheckInFieldErrors(snapshot.draft, editing = snapshot.selected != null)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(formError = null, fieldErrors = errors) }
            return
        }
        _state.update { it.copy(saving = true, formError = null, fieldErrors = emptyMap()) }
        workScope.launch {
            val result = snapshot.selected?.let { repository.update(it.id, snapshot.draft) }
                ?: repository.create(snapshot.draft)
            result.onSuccess { entry ->
                _state.update { it.copy(saving = false, editing = false, selected = entry,
                    saveNotice = homeVisibilitySaveNotice(snapshot.draft.visibility, entry.visibility)) }
                refresh()
            }.onFailure { error ->
                val failure = error as? HomeCheckInFailure
                _state.update { it.copy(saving = false,
                    fieldErrors = failure?.fieldErrors.orEmpty(),
                    formError = if (failure?.fieldErrors?.isNotEmpty() == true) null
                        else failure?.message ?: "Не удалось сохранить чекин. Попробуйте ещё раз.",
                ) }
            }
        }
    }

    fun confirmDelete() = _state.update { it.copy(deleteConfirmation = true) }
    fun cancelDelete() = _state.update { it.copy(deleteConfirmation = false) }
    fun delete() {
        val id = _state.value.selected?.id ?: return
        _state.update { it.copy(saving = true, deleteConfirmation = false) }
        workScope.launch {
            repository.delete(id).onSuccess {
                _state.update { it.copy(selected = null, saving = false) }
                refresh()
            }.onFailure { error -> _state.update { it.copy(saving = false, error = error.message) } }
        }
    }
}

/** Android's Instant.now() can emit 9 fractional digits; the server accepts at most 7. */
@OptIn(kotlin.time.ExperimentalTime::class)
private fun currentHomePreparedAt(): String = kotlin.time.Instant.fromEpochMilliseconds(currentEpochMillis()).toString()

internal fun homeVisibilitySaveNotice(
    requested: HomeCheckInVisibility?,
    returned: HomeCheckInVisibility?,
): String? = if (requested != null && requested != returned)
    "Чекин сохранён, но сервер не подтвердил выбранную видимость. Её можно повторно изменить через редактирование этой записи."
else null
