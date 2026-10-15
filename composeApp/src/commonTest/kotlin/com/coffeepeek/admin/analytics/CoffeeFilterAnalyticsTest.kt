@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.analytics

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.admin.feature.coffee.domain.CoffeeFilters
import com.coffeepeek.admin.feature.coffee.domain.CoffeePage
import com.coffeepeek.admin.feature.coffee.domain.CoffeeRepository
import com.coffeepeek.admin.feature.coffee.ui.CoffeeListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals

class CoffeeFilterAnalyticsTest {
    @Test fun searchAndFilterEventsReportOnlyActiveKeysIncludingAfterReset() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModels = ViewModelStore()
        try {
            val sdk = RecordingTransport()
            val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
            service.setForeground(true); service.restoreConsent(); service.syncIdentity("backend-id"); Analytics.install(service)
            val repository = object : CoffeeRepository {
                override suspend fun search(query: String, filters: CoffeeFilters, page: Int) =
                    Result.success(CoffeePage(emptyList(), 1, 1, 0))
                override suspend fun getFilterGroups() = Result.success(emptyList<com.coffeepeek.admin.feature.coffee.domain.CoffeeFilterGroup>())
                override suspend fun getDetails(slug: String) = error("Unused")
            }
            val vm = CoffeeListViewModel(repository)
            viewModels.put("coffee", vm)
            runCurrent()
            assertEquals(emptyList<String>(), sdk.events.single { it.first == "catalog_search_started" }.second["filter_keys"])

            vm.applyFilters(CoffeeFilters(values = mapOf("roast" to setOf("light"), "countries" to emptySet()), availableOnly = true))
            runCurrent()
            val applied = sdk.events.last { it.first == "filters_applied" }.second
            assertEquals(listOf("availableOnly", "roast"), applied["filter_keys"])
            assertEquals("apply", applied["action"])
            assertEquals(listOf("availableOnly", "roast"), sdk.events.last { it.first == "catalog_search_started" }.second["filter_keys"])

            vm.applyFilters(CoffeeFilters())
            runCurrent()
            val reset = sdk.events.last { it.first == "filters_applied" }.second
            assertEquals("reset", reset["action"])
            assertEquals(emptyList<String>(), reset["filter_keys"])
            assertEquals(emptyList<String>(), sdk.events.last { it.first == "catalog_search_started" }.second["filter_keys"])
        } finally {
            viewModels.clear()
            Dispatchers.resetMain()
        }
    }
}
