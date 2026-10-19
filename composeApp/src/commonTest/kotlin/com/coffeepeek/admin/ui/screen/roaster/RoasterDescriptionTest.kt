package com.coffeepeek.admin.ui.screen.roaster

import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.CreateRoasterInput
import com.coffeepeek.domain.model.ModerationStatus
import com.coffeepeek.domain.model.PagedResult
import com.coffeepeek.domain.model.PublicAddress
import com.coffeepeek.domain.model.RoasterDetails
import com.coffeepeek.domain.model.RoasterSubmission
import com.coffeepeek.domain.model.RoasterSubmissionResult
import com.coffeepeek.domain.model.RoasterSummary
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.RoasterRepository
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoasterDescriptionTest {
    @Test
    fun loadsDescriptionsOnDemandWithoutBlockingTheCatalogOrLosingSummaryData() = runBlocking {
        withTimeout(5_000) {
            val response = CompletableDeferred<Result<RoasterDetails>>()
            val repository = DescriptionRoasters { response.await() }
            val vm = viewModel(repository)
            try {
                val catalog = vm.state.first { !it.isLoading }
                assertTrue(repository.requests.isEmpty())
                val first = async(start = CoroutineStart.UNDISPATCHED) { vm.loadDetails("roast") }
                val duplicate = async(start = CoroutineStart.UNDISPATCHED) { vm.loadDetails("roast") }
                assertFalse(vm.state.value.isLoading)
                assertEquals(listOf("roast"), repository.requests)

                response.complete(Result.success(RoasterDetails("roast", "Roast", about = "Обжариваем кофе в Минске")))
                first.await()
                duplicate.await()
                val item = vm.state.value.items.first()
                assertEquals("Обжариваем кофе в Минске", item.details?.about)
                assertEquals(catalog.items.first().catalog, item.catalog)
                assertNull(vm.state.value.items.last().details)
                vm.loadDetails("roast")
                assertEquals(listOf("roast"), repository.requests)
            } finally { vm.close() }
        }
    }

    @Test
    fun failedDescriptionDoesNotHideCardsAndCanBeRetried() = runBlocking {
        withTimeout(5_000) {
            var available = false
            val repository = DescriptionRoasters {
                if (available) Result.success(RoasterDetails(it, "Roast", about = "Описание"))
                else Result.failure(IllegalStateException("Offline"))
            }
            val vm = viewModel(repository)
            try {
                val catalog = vm.state.first { !it.isLoading }
                vm.loadDetails(null)
                vm.loadDetails("")
                vm.loadDetails("missing")
                assertTrue(repository.requests.isEmpty())
                vm.loadDetails("roast")
                assertEquals(catalog, vm.state.value)
                available = true
                vm.loadDetails("roast")
                assertEquals("Описание", vm.state.value.items.first().details?.about)
                assertEquals(listOf("roast", "roast"), repository.requests)
            } finally { vm.close() }
        }
    }

    @Test
    fun limitsConcurrentDetailRequestsAndCancelsCardsThatLeaveComposition() = runBlocking {
        withTimeout(5_000) {
            var active = 0
            var peak = 0
            val response = CompletableDeferred<Unit>()
            val repository = DescriptionRoasters(count = 6) {
                active++
                peak = maxOf(peak, active)
                try {
                    response.await()
                    Result.success(RoasterDetails(it, it, about = "Описание $it"))
                } finally { active-- }
            }
            val vm = viewModel(repository)
            try {
                val items = vm.state.first { !it.isLoading }.items
                val jobs = items.map { item ->
                    async(start = CoroutineStart.UNDISPATCHED) { vm.loadDetails(item.routeId) }
                }
                assertEquals(4, active)
                jobs.first().cancelAndJoin()
                assertNull(vm.state.value.items.first().details)
                response.complete(Unit)
                jobs.drop(1).forEach { it.await() }
                assertEquals(4, peak)
                assertTrue(vm.state.value.items.drop(1).all { it.details?.about != null })
                vm.loadDetails(items.first().routeId)
                assertEquals("Описание roast", vm.state.value.items.first().details?.about)
            } finally { vm.close() }
        }
    }

    private fun viewModel(repository: RoasterRepository) = RoasterListViewModel(
        repository,
        object : RoasterFavorites {
            override fun observeFavorites() = flowOf(emptyList<CatalogItem>())
            override suspend fun setFavorite(roaster: CatalogItem, isFavorite: Boolean) = Result.success(Unit)
        },
        DescriptionSessions,
    )
}

private class DescriptionRoasters(
    private val count: Int = 2,
    private val details: suspend (String) -> Result<RoasterDetails>,
) : RoasterRepository {
    val requests = mutableListOf<String>()
    override suspend fun getRoasters() = Result.success(List(count) { index ->
        val slug = if (index == 0) "roast" else "roast-$index"
        RoasterSummary(
            PublicAddress(slug, "/roasters/$slug", 1, false), slug,
            coffeeShopsCount = 7, coffeeProductsCount = 21,
        )
    })
    override suspend fun getRoaster(id: String): Result<RoasterDetails> {
        requests += id
        return details(id)
    }
    override suspend fun submitRoaster(input: CreateRoasterInput): Result<RoasterSubmissionResult> = error("Unused")
    override suspend fun getMyRoasterSubmissions(status: ModerationStatus, page: Int, pageSize: Int): Result<PagedResult<RoasterSubmission>> = error("Unused")
}

private object DescriptionSessions : SessionRepository {
    override fun peekSession(): Session? = null
    override fun applySession(session: Session?) = Unit
    override fun isActiveSession(session: Session?) = false
    override suspend fun getSession(): Session? = null
    override suspend fun persistSession(session: Session?) = Unit
    override suspend fun saveSession(session: Session?) = Unit
    override suspend fun warmCache() = Unit
    override fun observeSession() = flowOf<Session?>(null)
    override suspend fun isLoggedIn() = false
}
