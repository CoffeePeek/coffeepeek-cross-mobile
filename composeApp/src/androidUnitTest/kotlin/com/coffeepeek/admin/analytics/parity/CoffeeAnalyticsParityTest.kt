@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.analytics.parity

import com.coffeepeek.admin.analytics.parity.ParityHarness.record
import com.coffeepeek.admin.di.coffee.AndroidCoffeeAnalytics
import com.coffeepeek.admin.feature.coffee.domain.Coffee
import com.coffeepeek.admin.feature.coffee.domain.CoffeeDetails
import com.coffeepeek.admin.feature.coffee.domain.CoffeeFilterGroup
import com.coffeepeek.admin.feature.coffee.domain.CoffeeFilters
import com.coffeepeek.admin.feature.coffee.domain.CoffeePage
import com.coffeepeek.admin.feature.coffee.domain.CoffeeRepository
import com.coffeepeek.admin.feature.coffee.ui.CoffeeListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test

/** Coffee catalogue list: legacy `CoffeeListViewModel` vs the migrated `CoffeeListAnalytics` adapter. */
class CoffeeAnalyticsParityTest {
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val coffee = Coffee("ethiopia", "Ethiopia", null, "Roaster", null, listOf("Ethiopia"), listOf("fruity"), emptyList())
    private val tasteFilters = CoffeeFilters(values = mapOf("taste" to setOf("fruity")))

    private class FakeRepository(private val page: Result<CoffeePage>) : CoffeeRepository {
        override suspend fun search(query: String, filters: CoffeeFilters, page: Int) = this.page
        override suspend fun getFilterGroups() = Result.success(emptyList<CoffeeFilterGroup>())
        override suspend fun getDetails(slug: String): Result<CoffeeDetails> = Result.failure(IllegalStateException("unused"))
    }

    /** Initial load, a typed query, applying then resetting a taste filter, and opening the first result. */
    private suspend fun TestScope.legacyJourney(page: Result<CoffeePage>, open: Boolean): List<WireEvent> {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return record {
            val vm = CoffeeListViewModel(FakeRepository(page))
            runCurrent()
            vm.onQueryChange("ethio")
            advanceTimeBy(301); runCurrent()
            vm.applyFilters(tasteFilters)
            runCurrent()
            vm.applyFilters(CoffeeFilters())
            runCurrent()
            if (open) vm.openedCoffee(0)
        }
    }

    private suspend fun TestScope.migratedJourney(page: Result<CoffeePage>, open: Boolean): List<WireEvent> = record {
        val list = AndroidCoffeeAnalytics.list()
        fun settle(count: Int?) = page.fold({ count?.let(list::resultsViewed) }, { list.searchFailed(it) })
        val count = page.getOrNull()?.let { it.totalCount ?: if (it.totalPages <= 1) it.items.size else null }
        list.searchStarted("|", "", emptyList()); settle(count)
        list.searchStarted("ethio|", "ethio", emptyList()); settle(count)
        list.filtersApplied(listOf("taste"), listOf("taste"), reset = false)
        list.searchStarted("ethio|taste", "ethio", listOf("taste")); settle(count)
        list.filtersApplied(emptyList(), listOf("taste"), reset = true)
        list.searchStarted("ethio|", "ethio", emptyList()); settle(count)
        if (open) list.opened(1)
    }

    private fun journey(name: String, page: Result<CoffeePage>, open: Boolean) = runTest {
        ParityHarness.assertParity(name, legacyJourney(page, open), migratedJourney(page, open))
    }

    @Test fun searchFilterAndOpen() = journey("search, filter, open",
        Result.success(CoffeePage(listOf(coffee), 1, 1, totalCount = 1)), open = true)

    @Test fun searchWithoutOpening() = journey("search without opening",
        Result.success(CoffeePage(listOf(coffee), 1, 1)), open = false)

    @Test fun searchFailure() = journey("search failure",
        Result.failure(java.io.IOException("offline")), open = false)

    @Test fun filtersOpened() = runTest {
        val legacy = record { com.coffeepeek.admin.analytics.Analytics.track(
            com.coffeepeek.admin.analytics.AnalyticsEvent.FILTERS_OPENED, "catalog" to "coffees", "placement" to "mobile") }
        val migrated = record { AndroidCoffeeAnalytics.list().filtersOpened() }
        ParityHarness.assertParity("filters opened", legacy, migrated)
    }
}
