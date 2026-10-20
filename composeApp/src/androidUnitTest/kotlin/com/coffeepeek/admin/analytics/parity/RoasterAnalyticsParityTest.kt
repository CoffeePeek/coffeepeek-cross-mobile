@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.analytics.parity

import com.coffeepeek.admin.analytics.parity.ParityHarness.record
import com.coffeepeek.admin.di.roaster.AndroidRoasterAnalytics
import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.admin.ui.screen.roaster.RoasterListViewModel
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.CreateRoasterInput
import com.coffeepeek.domain.model.ModerationStatus
import com.coffeepeek.domain.model.PagedResult
import com.coffeepeek.domain.model.PublicAddress
import com.coffeepeek.domain.model.RoasterDetails
import com.coffeepeek.domain.model.RoasterSubmission
import com.coffeepeek.domain.model.RoasterSubmissionResult
import com.coffeepeek.domain.model.RoasterSummary
import com.coffeepeek.domain.repository.RoasterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test

/** Roaster catalogue list: legacy `RoasterListViewModel` vs the migrated `RoasterListAnalytics` adapter. */
class RoasterAnalyticsParityTest {
    private val light = CatalogItem("light", "Светлая", slug = "light")
    private fun summary(slug: String, name: String, vararg tags: CatalogItem) =
        RoasterSummary(PublicAddress(slug, "/roasters/$slug", 1, false), name, tags = tags.toList())

    private class Repository(private val catalog: Result<List<RoasterSummary>>) : RoasterRepository {
        override suspend fun getRoasters() = catalog
        override suspend fun getRoaster(id: String): Result<RoasterDetails> = Result.failure(IllegalStateException("unused"))
        override suspend fun submitRoaster(input: CreateRoasterInput): Result<RoasterSubmissionResult> = TODO()
        override suspend fun getMyRoasterSubmissions(status: ModerationStatus, page: Int, pageSize: Int): Result<PagedResult<RoasterSubmission>> = TODO()
    }

    private object NoFavorites : RoasterFavorites {
        override fun observeFavorites() = flowOf(emptyList<CatalogItem>())
        override suspend fun setFavorite(roaster: CatalogItem, isFavorite: Boolean) = Result.success(Unit)
    }

    private suspend fun TestScope.legacyJourney(catalog: Result<List<RoasterSummary>>, open: Boolean) = record {
        val vm = RoasterListViewModel(Repository(catalog), NoFavorites, CheckInTestSessions())
        withContext(Dispatchers.Default) { while (vm.state.value.isLoading) delay(10) }
        vm.resultsShown("", emptySet(), false)
        vm.resultsShown("кит", emptySet(), false)
        vm.toggleTag("light")
        vm.resultsShown("кит", emptySet(), false)
        if (open) vm.openedRoaster(0)
    }

    private suspend fun TestScope.migratedJourney(catalog: Result<List<RoasterSummary>>, open: Boolean) = record {
        val list = AndroidRoasterAnalytics.list()
        val failed = catalog.isFailure
        val total = catalog.getOrNull()?.size ?: 0
        list.resultsShown("|", "", emptyList(), total, failed)
        list.resultsShown("кит|", "кит", emptyList(), total, failed)
        list.resultsShown("кит|tags", "кит", listOf("tags"), total, failed)
        if (open) list.opened(1)
    }

    private fun journey(name: String, catalog: Result<List<RoasterSummary>>, open: Boolean) = runTest {
        ParityHarness.assertParity(name, legacyJourney(catalog, open), migratedJourney(catalog, open))
    }

    // Names contain the typed query, so the visible count equals the catalogue size in both journeys.
    private val roasters = Result.success(listOf(summary("kit-one", "Китч", light), summary("kit-two", "Кит Лаб", light)))

    @Test fun searchTagAndOpen() = journey("search, tag, open", roasters, open = true)
    @Test fun searchWithoutOpening() = journey("search without opening", roasters, open = false)
    @Test fun catalogFailure() = journey("catalog failure", Result.failure(java.io.IOException("offline")), open = false)
}
