package com.coffeepeek.admin.di.shop

import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.authorblocking.api.AuthorVisibilitySnapshot
import com.coffeepeek.feature.shop.domain.model.*
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ShopAuthorVisibilityTest {
    @Test fun blockedIdsAndLegacySlugsAreFilteredWithoutErasingOwnHistory() = runTest {
        val visibility = Visibility(AuthorVisibilitySnapshot(blockedIds = setOf("id-a"), blockedSlugs = setOf("slug-b")))
        val a = visit("a", "id-a", "slug-a")
        val b = visit("b", null, "slug-b")
        val c = visit("c", "id-c", "slug-c")
        val snapshot = details(listOf(a, b, c)).copy(userCheckIns = listOf(a))
        val result = ShopBrowsingSession(CheckInTestSessions(), visibility = visibility)
            .details(repository { Result.success(snapshot) }).getDetails("shop").getOrThrow()
        assertEquals(listOf(c), result.checkIns)
        assertEquals(listOf(a), result.userCheckIns)
    }

    @Test fun unknownVisibilityFailsClosedAndKeepsRefreshFailure() = runTest {
        val error = IllegalStateException("Blocks unavailable")
        val visibility = Visibility(AuthorVisibilitySnapshot(ready = false), Result.failure(error))
        var reads = 0
        val result = ShopBrowsingSession(CheckInTestSessions(), visibility = visibility)
            .details(repository { reads++; Result.success(details(emptyList())) }).getDetails("shop")
        assertSame(error, result.exceptionOrNull())
        assertEquals(0, reads)
    }

    @Test fun revisionChangeRevokesEvenANonCooperativeLateRead() = runTest {
        val visibility = Visibility(AuthorVisibilitySnapshot())
        val lease = ShopBrowsingSession(CheckInTestSessions(), visibility = visibility)
        val gate = CompletableDeferred<Unit>()
        val request = async {
            lease.details(repository {
                withContext(NonCancellable) { gate.await() }
                Result.success(details(listOf(visit("a", "id-a", "slug-a"))))
            }).getDetails("shop")
        }
        runCurrent()
        visibility.visibility.value = AuthorVisibilitySnapshot(revision = 1, blockedIds = setOf("id-a"))
        runCurrent()
        gate.complete(Unit)
        assertFailsWith<CancellationException> { request.await() }
        assertFalse(lease.isCurrent())
    }

    private class Visibility(
        initial: AuthorVisibilitySnapshot,
        private val refreshResult: Result<Unit> = Result.success(Unit),
    ) : AuthorVisibility {
        override val visibility = MutableStateFlow(initial)
        override suspend fun refresh() = refreshResult
    }

    private fun repository(read: suspend () -> Result<ShopDetails>) = object : ShopDetailsRepository {
        override suspend fun getDetails(shopId: String) = read()
    }

    private fun visit(id: String, authorId: String?, slug: String?) = ShopCheckIn(
        id, slug.orEmpty(), "shop", "", "", "", null, emptyList(), emptyList(), null,
        authorId = authorId, authorSlug = slug,
    )

    private fun details(visits: List<ShopCheckIn>) = ShopDetails(
        overview = ShopOverview("shop", "Shop", null, null, null, null, null, 0, false, emptyList()),
        menu = null, schedules = emptyList(), coffee = ShopCoffeeDetails(), contact = null,
        features = emptyList(), reviews = emptyList(), userCheckIns = emptyList(), checkIns = visits,
    )
}
