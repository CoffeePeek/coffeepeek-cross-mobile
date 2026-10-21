package com.coffeepeek.admin.feature.homecheckin.ui

import com.coffeepeek.admin.analytics.*

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
import kotlinx.coroutines.flow.updateAndGet
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
    val calendarItems: List<HomeCheckIn> = emptyList(),
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
    private val onCheckInCreated: () -> Unit = {},
) : BaseViewModel() {
    private val now = currentHomePreparedAt()
    private val today = utcIsoToLocalDate(now)
    private val todayMonth = HomeMonth(today.substring(0, 4).toInt(), today.substring(5, 7).toInt())
    private val _state = MutableStateFlow(HomeCheckInUiState(
        month = todayMonth,
        draft = HomeCheckInInput(preparedAt = now, preparedOn = today, tasteScore = 5),
    ))
    val state = _state.asStateFlow()
    private val historyRevision = MutableStateFlow(0L)
    private val calendarRevision = MutableStateFlow(0L)
    private val detailRevision = MutableStateFlow(0L)
    private var detailJob: Job? = null
    private var historyJob: Job? = null
    private var calendarJob: Job? = null
    private var catalogJob: Job? = null
    private var methodsJob: Job? = null
    private var statsRequested = false
    private var checkInAnalytics: CheckInTracker? = null
    private var creationAnalytics: CheckInTracker? = null
    fun creationTracker(): CheckInTracker = creationAnalytics ?: CheckInTracker(CheckInKind.Undecided, CheckInSource.Global)
        .also { it.step(CheckInStep.Type); creationAnalytics = it }
    fun creationPickerFinished() { creationAnalytics = null }

    init { refresh() }

    fun refresh() {
        loadHistory(reset = true)
        loadCalendar()
        if (statsRequested) loadStats()
    }

    fun loadMore() = loadHistory(reset = false)

    private fun loadHistory(reset: Boolean) {
        if (!reset && (!_state.value.hasMore || _state.value.loading || _state.value.loadingMore)) return
        if (reset) historyJob?.cancel()
        val revision = historyRevision.updateAndGet { it + 1 }
        val page = if (reset) 1 else _state.value.page + 1
        val selectedDate = _state.value.selectedDate
        _state.update { it.copy(loading = reset, loadingMore = !reset, error = null) }
        historyJob = workScope.launch {
            val response = repository.history(page = page, from = selectedDate, to = selectedDate)
            coroutineContext.ensureActive()
            response.onSuccess { result ->
                    _state.update { old -> if (historyRevision.value != revision || old.selectedDate != selectedDate) old else old.copy(
                        loading = false, loadingMore = false, page = result.page,
                        hasMore = result.hasMore,
                        items = if (reset) result.items else (old.items + result.items).distinctBy { it.id },
                    ) }
                }
                .onFailure { error -> _state.update {
                    if (historyRevision.value != revision || it.selectedDate != selectedDate) it
                    else it.copy(loading = false, loadingMore = false, error = error.message)
                } }
        }
    }

    fun changeMonth(offset: Int) {
        val next = _state.value.month.change(offset)
        if (next.year * 12 + next.month > todayMonth.year * 12 + todayMonth.month) return
        _state.update { it.copy(month = next, selectedDate = null, calendar = emptyList(), calendarItems = emptyList()) }
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
        val revision = calendarRevision.updateAndGet { it + 1 }
        _state.update { it.copy(calendarLoading = true, calendarError = null) }
        calendarJob = workScope.launch {
            try {
                val days = repository.calendar(month.from, month.to).getOrThrow()
                ensureActive()
                _state.update { if (calendarRevision.value == revision && it.month == month) it.copy(calendar = days) else it }
                // The summary route returns counts only. Fetch month photos separately so a
                // selected day or a partially loaded history cannot erase other day markers.
                val entries = mutableListOf<HomeCheckIn>()
                if (days.any { it.brewCount > 0 }) {
                    var page = 1
                    do {
                        val result = repository.history(page, 100, month.from, month.to).getOrThrow()
                        ensureActive()
                        entries += result.items
                        page++
                    } while (result.hasMore && result.items.isNotEmpty() && page <= 10000)
                }
                _state.update { if (calendarRevision.value == revision && it.month == month) it.copy(
                    calendarItems = entries.distinctBy(HomeCheckIn::id), calendarLoading = false,
                ) else it }
            } catch (error: Exception) {
                ensureActive()
                _state.update { if (calendarRevision.value == revision && it.month == month) it.copy(
                    calendarError = error.message ?: "Не удалось загрузить календарь", calendarLoading = false,
                ) else it }
            }
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
        detailJob?.cancel()
        val revision = detailRevision.updateAndGet { it + 1 }
        _state.update { it.copy(detailLoading = true, error = null, saveNotice = null) }
        detailJob = workScope.launch {
            val response = repository.detail(id)
            coroutineContext.ensureActive()
            response.onSuccess { entry ->
                _state.update { if (detailRevision.value == revision) it.copy(selected = entry, detailLoading = false) else it }
                if (editOnLoad && detailRevision.value == revision) edit()
            }.onFailure { error -> _state.update {
                if (detailRevision.value == revision) it.copy(detailLoading = false, error = error.message) else it
            } }
        }
    }

    private fun cancelDetail() {
        detailRevision.update { it + 1 }
        detailJob?.cancel()
    }

    fun closeDetail() {
        cancelDetail()
        _state.update { it.copy(selected = null, editing = false, detailLoading = false, saveNotice = null) }
    }

    fun create(tracker: CheckInTracker? = null) {
        checkInAnalytics = (tracker ?: CheckInTracker(CheckInKind.Home, CheckInSource.Direct)).also { it.step(CheckInStep.Editor) }
        cancelDetail()
        catalogJob?.cancel()
        val preparedAt = currentHomePreparedAt()
        _state.update { it.copy(
            editing = true, selected = null, detailLoading = false, formError = null, fieldErrors = emptyMap(), saveNotice = null,
            catalogQuery = "", catalogItems = emptyList(), catalogSelectionName = null,
            catalogLoading = false, catalogError = null,
            draft = HomeCheckInInput(preparedAt = preparedAt, preparedOn = utcIsoToLocalDate(preparedAt), tasteScore = 5),
        ) }
        loadBrewMethods()
    }

    fun edit() {
        val entry = _state.value.selected ?: return
        checkInAnalytics = CheckInTracker(CheckInKind.Home, CheckInSource.Diary, edit = true).also { it.step(CheckInStep.Editor) }
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
        val before = _state.value.draft
        val after = change(before)
        listOf("date" to (before.preparedAt != after.preparedAt || before.preparedOn != after.preparedOn),
            "brew_method" to (before.brewMethodCode != after.brewMethodCode), "bean" to (before.customBeanName != after.customBeanName || before.coffeeProductId != after.coffeeProductId || before.coffeeProductSlug != after.coffeeProductSlug),
            "dose" to (before.doseGrams != after.doseGrams), "brew_duration" to (before.brewDurationSeconds != after.brewDurationSeconds),
            "rating_taste" to (before.tasteScore != after.tasteScore), "note" to (before.text != after.text),
            "drink" to (before.drinkSlug != after.drinkSlug || before.customDrinkName != after.customDrinkName),
            "photos" to (before.photos != after.photos), "visibility" to (before.visibility != after.visibility))
            .filter { it.second }.forEach { checkInAnalytics?.field(it.first) }
        _state.update { it.withDraft(after) }
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
        checkInAnalytics?.step(CheckInStep.Bean)
        checkInAnalytics?.field("bean")
        _state.update { it.copy(catalogQuery = query.take(100), catalogError = null) }
        searchCatalog(debounce = true)
    }

    fun searchCatalog() { checkInAnalytics?.retry(CheckInStage.Bean); searchCatalog(debounce = false) }

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
                .onFailure { error -> checkInAnalytics?.failed(CheckInStage.Bean, safeAnalyticsError(error)); _state.update { it.copy(catalogError = error.message, catalogLoading = false) } }
        }
    }

    fun selectCatalogProduct(product: CoffeePickerItem) {
        checkInAnalytics?.field("bean")
        catalogJob?.cancel()
        _state.update { it.withDraft(it.draft.copy(coffeeProductSlug = product.slug,
            coffeeProductId = null, customBeanName = null)).copy(
            catalogLoading = false, catalogError = null, catalogSelectionName = product.name,
        ) }
    }

    fun closeEditor() { checkInAnalytics?.cancel("close"); _state.update { it.copy(editing = false, formError = null, fieldErrors = emptyMap()) } }

    fun save() {
        val snapshot = _state.value
        if (snapshot.saving) return
        val tracker = checkInAnalytics
        tracker?.submit()
        val errors = homeCheckInFieldErrors(snapshot.draft, editing = snapshot.selected != null)
        if (errors.isNotEmpty()) {
            tracker?.validation(errors.keys.map { it.analyticsField() })
            _state.update { it.copy(formError = null, fieldErrors = errors) }
            return
        }
        _state.update { it.copy(saving = true, formError = null, fieldErrors = emptyMap()) }
        workScope.launch {
            val result = snapshot.selected?.let { repository.update(it.id, snapshot.draft) }
                ?: repository.create(snapshot.draft)
            result.onSuccess { entry ->
                tracker?.saved(entry.visibility?.name ?: "unknown", entry.photos.isNotEmpty())
                _state.update { it.copy(saving = false, editing = false, selected = entry,
                    saveNotice = homeVisibilitySaveNotice(snapshot.draft.visibility, entry.visibility)) }
                if (snapshot.selected == null) onCheckInCreated()
                refresh()
            }.onFailure { error ->
                val failure = error as? HomeCheckInFailure
                if (failure?.fieldErrors?.isNotEmpty() == true) tracker?.validation(failure.fieldErrors.keys.map { it.analyticsField() }, server = true)
                else tracker?.failed(if (failure?.operation == com.coffeepeek.feature.homecheckin.domain.HomeCheckInOperation.PhotoUpload) CheckInStage.Photo else CheckInStage.Save, safeAnalyticsError(error))
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

private fun HomeCheckInField.analyticsField() = when (this) {
    HomeCheckInField.Date -> "date"; HomeCheckInField.Method -> "brew_method"; HomeCheckInField.Bean -> "bean"
    HomeCheckInField.Dose -> "dose"; HomeCheckInField.Duration -> "brew_duration"; HomeCheckInField.Rating -> "rating_taste"
    HomeCheckInField.Note -> "note"; HomeCheckInField.Drink -> "drink"; HomeCheckInField.Photos -> "photos"; HomeCheckInField.Visibility -> "visibility"
}
