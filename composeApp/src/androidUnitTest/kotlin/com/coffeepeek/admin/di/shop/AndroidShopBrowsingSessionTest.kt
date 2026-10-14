package com.coffeepeek.admin.di.shop

import com.coffeepeek.domain.model.Session
import com.coffeepeek.feature.shop.domain.model.ShopHelpfulVote
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInVoteRepository
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidShopBrowsingSessionTest {
    @Test fun guestsCanReadButCannotVote() = runTest {
        val sessions = CheckInTestSessions(null)
        val lease = AndroidShopBrowsingSession(sessions)
        val failure = IllegalStateException("Backend unavailable")
        var reads = 0
        val details = lease.details(object : ShopDetailsRepository {
            override suspend fun getDetails(shopId: String): Result<com.coffeepeek.feature.shop.domain.model.ShopDetails> {
                reads++
                return Result.failure(failure)
            }
        })
        assertSame(failure, details.getDetails("shop").exceptionOrNull())
        assertEquals(1, reads)
        val votes = Votes()
        assertTrue(lease.votes(votes).setHelpful("visit", true).isFailure)
        assertEquals(0, votes.calls)
    }

    @Test fun tokenRenewalDoesNotCancelOrRepeatVote() = runTest {
        val sessions = CheckInTestSessions()
        val lease = AndroidShopBrowsingSession(sessions)
        val gate = CompletableDeferred<Unit>()
        val votes = Votes().apply { write = { gate.await(); Result.success(ShopHelpfulVote(true, 1)) } }
        val request = async { lease.votes(votes).setHelpful("visit", true) }
        runCurrent()
        sessions.applySession(Session("renewed", userId = "owner"))
        runCurrent()
        assertFalse(request.isCompleted)
        assertTrue(lease.isCurrent())
        gate.complete(Unit)
        assertTrue(request.await().isSuccess)
        assertEquals(1, votes.calls)
    }

    @Test fun replacedAccountDropsEvenANonCooperativeLateVote() = runTest {
        val sessions = CheckInTestSessions()
        val lease = AndroidShopBrowsingSession(sessions)
        val gate = CompletableDeferred<Unit>()
        val votes = Votes().apply {
            write = { withContext(NonCancellable) { gate.await() }; Result.success(ShopHelpfulVote(true, 1)) }
        }
        val request = async { lease.votes(votes).setHelpful("visit", true) }
        runCurrent()
        sessions.applySession(Session("other", userId = "other"))
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertFailsWith<CancellationException> { request.await() }
        sessions.applySession(Session("owner-again", userId = "owner"))
        assertFalse(lease.isCurrent())
        assertFailsWith<CancellationException> { lease.votes(votes).setHelpful("visit", true) }
        assertEquals(1, votes.calls)
    }

    @Test fun peekSessionIsCheckedBeforeTheObserverCanCatchUp() = runTest {
        val sessions = CheckInTestSessions()
        val lease = AndroidShopBrowsingSession(sessions)
        val votes = Votes().apply {
            write = { sessions.applySession(null); Result.success(ShopHelpfulVote(true, 1)) }
        }
        assertFailsWith<CancellationException> { lease.votes(votes).setHelpful("visit", true) }
        assertFalse(lease.isCurrent())
    }

    @Test fun knownFailuresRemainResultFailures() = runTest {
        val sessions = CheckInTestSessions()
        val failure = IllegalStateException("Rejected")
        val votes = Votes().apply { write = { Result.failure(failure) } }
        assertSame(failure, AndroidShopBrowsingSession(sessions).votes(votes)
            .setHelpful("visit", true).exceptionOrNull())
    }

    @Test fun guestsNeverReadThePreviousAccountsLocalFavorites() = runTest {
        var reads = 0
        val repository = object : FavoritesRepository {
            override fun observe() = flowOf(Result.success(emptyList<FavoriteShop>())).also { reads++ }
            override suspend fun read(): Result<List<FavoriteShop>> { reads++; return Result.success(emptyList()) }
            override suspend fun save(shop: FavoriteShop): Result<Unit> = error("Guest must not write")
            override suspend fun remove(shopId: String): Result<Unit> = error("Guest must not write")
            override suspend fun clear(): Result<Unit> = error("Guest must not clear")
        }
        val favorites = AndroidShopBrowsingSession(CheckInTestSessions(null)).favorites(repository)
        assertTrue(favorites.observe().first().getOrThrow().isEmpty())
        assertTrue(favorites.read().getOrThrow().isEmpty())
        assertTrue(favorites.remove("shop").isFailure)
        assertTrue(favorites.clear().isFailure)
        assertEquals(0, reads)
    }

    private class Votes : ShopCheckInVoteRepository {
        var calls = 0
        var write: suspend () -> Result<ShopHelpfulVote> = { Result.success(ShopHelpfulVote(true, 1)) }
        override suspend fun setHelpful(checkInId: String, helpful: Boolean): Result<ShopHelpfulVote> {
            calls++
            return write()
        }
    }
}
