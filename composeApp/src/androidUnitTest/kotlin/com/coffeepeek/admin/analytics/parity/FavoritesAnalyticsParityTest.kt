@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.analytics.parity

import com.coffeepeek.admin.analytics.Entity
import com.coffeepeek.admin.analytics.parity.ParityHarness.record
import com.coffeepeek.admin.di.favorites.trackFavoriteMutation
import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.admin.feature.favorites.ui.TrackedFavoriteInteractions
import com.coffeepeek.domain.model.Session
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/** Server-backed favorite writes: `TrackedFavoriteInteractions` (legacy) vs `trackFavoriteMutation` (migrated). */
class FavoritesAnalyticsParityTest {
    private suspend fun TestScope.legacy(roaster: Boolean, desired: Boolean, result: Result<Unit>): List<WireEvent> {
        val sessions = CheckInTestSessions(Session("token", userId = "fixture-owner"))
        return record {
            TrackedFavoriteInteractions(sessions, backgroundScope)
                .change("key", roaster, desired, { result })
            runCurrent()
        }
    }

    private suspend fun TestScope.migrated(roaster: Boolean, desired: Boolean, result: Result<Unit>) = record {
        trackFavoriteMutation(if (roaster) Entity.Roaster else Entity.Shop, desired) { result }
    }

    private fun scenario(name: String, roaster: Boolean, desired: Boolean, result: Result<Unit>) = runTest {
        ParityHarness.assertParity(name, legacy(roaster, desired, result), migrated(roaster, desired, result))
    }

    @Test fun shopAdded() = scenario("shop added", roaster = false, desired = true, Result.success(Unit))
    @Test fun shopRemoved() = scenario("shop removed", roaster = false, desired = false, Result.success(Unit))
    @Test fun roasterAdded() = scenario("roaster added", roaster = true, desired = true, Result.success(Unit))
    @Test fun roasterRemoved() = scenario("roaster removed", roaster = true, desired = false, Result.success(Unit))
    @Test fun shopWriteFailed() = scenario("shop write failed", false, true, Result.failure(IllegalStateException("boom")))
    @Test fun roasterWriteFailed() = scenario("roaster write failed", true, false, Result.failure(java.io.IOException("offline")))
}
