package com.coffeepeek.admin.feature.homecheckin.ui

import com.coffeepeek.admin.feature.catalog.api.BrewMethodCatalog
import com.coffeepeek.admin.feature.coffee.api.CoffeePicker
import com.coffeepeek.admin.feature.coffee.api.CoffeePickerItem
import com.coffeepeek.feature.homecheckin.domain.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeCheckInCalendarTest {
    @Test
    fun cancelledMonthPhotoRequestCannotReplaceTheNewMonthsPhotos() = runBlocking {
        val repository = CalendarRepository(pauseFirstMonthPhotos = true)
        val vm = viewModel(repository)
        try {
            withTimeout(2_000) { repository.firstPhotosStarted.await() }
            vm.changeMonth(-1)
            val month = vm.state.value.month
            withTimeout(2_000) { vm.state.first { !it.calendarLoading && it.calendarItems.size == 2 } }
            repository.releaseFirstPhotos.complete(Unit)
            withTimeout(2_000) { repository.firstPhotosFinished.await() }
            assertEquals(listOf(month.from, month.from), vm.state.value.calendarItems.map(HomeCheckIn::preparedOn))
            assertEquals(month, vm.state.value.month)
        } finally { repository.releaseFirstPhotos.complete(Unit); vm.close() }
    }

    @Test
    fun calendarLoadsAllMonthPhotoPagesIndependentlyOfTimelineSelection() = runBlocking {
        val repository = CalendarRepository()
        val vm = viewModel(repository)
        try {
            val state = withTimeout(2_000) { vm.state.first { !it.calendarLoading && it.calendarItems.size == 2 } }
            assertEquals(listOf(1, 2), repository.monthPages)
            assertEquals(listOf("first.png", "second.png"), state.calendarItems.map(HomeCheckIn::drinkPhotoUrl))
            assertEquals(listOf(state.month.from to state.month.to, state.month.from to state.month.to), repository.monthRanges)
            vm.selectDate(state.month.from)
            withTimeout(2_000) { vm.state.first { !it.loading && it.items.size == 1 } }
            assertEquals(listOf("first", "second"), vm.state.value.calendarItems.map(HomeCheckIn::id))
            assertEquals(listOf(1, 2), repository.monthPages, "Selecting a day must not reload or replace the month photos")
        } finally { vm.close() }
    }

    @Test
    fun movingToAnEmptyMonthClearsPreviousPhotosWithoutLoadingHistoryPages() = runBlocking {
        val repository = CalendarRepository()
        val vm = viewModel(repository)
        try {
            withTimeout(2_000) { vm.state.first { it.calendarItems.size == 2 } }
            repository.emptyCalendar = true
            vm.changeMonth(-1)
            val state = withTimeout(2_000) { vm.state.first { !it.calendarLoading } }
            assertTrue(state.calendarItems.isEmpty())
            assertTrue(state.calendar.isEmpty())
            assertEquals(listOf(1, 2), repository.monthPages)
        } finally { vm.close() }
    }

    @Test
    fun unavailableMonthPhotosKeepCountsAndExposeARetryableCalendarError() = runBlocking {
        val vm = viewModel(CalendarRepository(failPhotos = true))
        try {
            val state = withTimeout(2_000) { vm.state.first { it.calendarError != null } }
            assertFalse(state.calendarLoading)
            assertEquals(2, state.calendar.single().brewCount)
            assertTrue(state.calendarItems.isEmpty())
        } finally { vm.close() }
    }

    private fun viewModel(repository: HomeCheckInRepository) = HomeCheckInViewModel(
        repository,
        object : CoffeePicker { override suspend fun search(query: String) = Result.success(emptyList<CoffeePickerItem>()) },
        BrewMethodCatalog { Result.success(emptyList()) },
    )
}

private class CalendarRepository(
    private val failPhotos: Boolean = false,
    private val pauseFirstMonthPhotos: Boolean = false,
) : HomeCheckInRepository {
    val monthPages = mutableListOf<Int>()
    val monthRanges = mutableListOf<Pair<String, String>>()
    var emptyCalendar = false
    val firstPhotosStarted = CompletableDeferred<Unit>()
    val releaseFirstPhotos = CompletableDeferred<Unit>()
    val firstPhotosFinished = CompletableDeferred<Unit>()

    override suspend fun history(page: Int, pageSize: Int, from: String?, to: String?): Result<HomeCheckInPage> {
        if (from == null || to == null) return Result.success(HomeCheckInPage(emptyList(), 0, page, pageSize))
        if (from == to) return Result.success(HomeCheckInPage(listOf(entry("first", from)), 1, page, pageSize))
        monthPages += page
        monthRanges += from to to
        if (pauseFirstMonthPhotos && monthPages.size == 1) withContext(NonCancellable) {
            firstPhotosStarted.complete(Unit)
            releaseFirstPhotos.await()
            firstPhotosFinished.complete(Unit)
        }
        if (failPhotos) return Result.failure(IllegalStateException("Не удалось загрузить фото календаря"))
        val entries = if (page == 1) listOf(entry("first", from)) else listOf(entry("second", from), entry("first", from))
        return Result.success(HomeCheckInPage(entries, 101, page, pageSize))
    }

    override suspend fun calendar(from: String, to: String) =
        Result.success(if (emptyCalendar) emptyList() else listOf(HomeCalendarDay(from, 2)))

    private fun entry(id: String, date: String) = HomeCheckIn(
        id, "${date}T09:00:00Z", date, "${date}T09:00:00Z", null, null, null, null,
        null, null, null, null, null, null, emptyList(), "$id.png",
    )

    override suspend fun create(input: HomeCheckInInput): Result<HomeCheckIn> = error("Unused")
    override suspend fun detail(id: String): Result<HomeCheckIn> = error("Unused")
    override suspend fun update(id: String, input: HomeCheckInInput): Result<HomeCheckIn> = error("Unused")
    override suspend fun delete(id: String) = Result.success(Unit)
    override suspend fun beanStats() = Result.success(emptyList<HomeBeanStats>())
    override suspend fun methodStats() = Result.success(emptyList<HomeMethodStats>())
    override suspend fun photo(url: String) = Result.success(byteArrayOf())
}
