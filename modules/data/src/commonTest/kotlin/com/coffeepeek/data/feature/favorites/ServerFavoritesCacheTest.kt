package com.coffeepeek.data.feature.favorites

import com.coffeepeek.domain.feature.favorites.api.FavoriteEntry
import com.coffeepeek.domain.feature.favorites.api.FavoriteKind
import com.coffeepeek.domain.feature.favorites.api.FavoriteTarget
import com.coffeepeek.domain.model.PublicAddress
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServerFavoritesCacheTest {
    private val owner = FavoriteCacheOwner("owner", 1)
    private val shop = FavoriteTarget(FavoriteKind.CoffeeShop, "shop")
    private val entry = FavoriteEntry(FavoriteKind.CoffeeShop, PublicAddress("shop", "/coffee-shops/shop", 1, false))

    @Test fun confirmedRemovalWinsOverAGetThatStartedBeforeItsPut() = runTest {
        val cache = ServerFavoritesCache { owner }
        cache.recordRows(owner, listOf(entry), cache.revision())
        val previousRead = cache.revision()
        assertTrue(cache.confirm(owner, shop, false))
        assertFalse(cache.cachedMembership(shop)!!)
        assertTrue(cache.peek(owner)!!.isEmpty())
        assertTrue(cache.recordRows(owner, listOf(entry), previousRead)!!.isEmpty())
        assertTrue(cache.memberships(owner).first().getOrThrow().isEmpty())
    }

    @Test fun confirmedAdditionAppearsWithoutInventingMetadataOrAwaitingTheFirstGet() = runTest {
        val cache = ServerFavoritesCache { owner }
        val previousRead = cache.revision()
        assertTrue(cache.confirm(owner, shop, true))
        assertEquals(setOf(shop), cache.memberships(owner).first().getOrThrow())
        assertNull(cache.peek(owner))
        cache.recordRows(owner, emptyList(), previousRead)
        assertEquals(setOf(shop), cache.memberships(owner).first().getOrThrow())
        cache.recordRows(owner, listOf(entry), cache.revision())
        assertEquals(listOf(entry), cache.peek(owner))
    }

    @Test fun failedMetadataReadDoesNotForgetAConfirmedMembership() = runTest {
        val cache = ServerFavoritesCache { owner }
        cache.confirm(owner, shop, true)
        cache.fail(owner, IllegalStateException("offline"))
        assertEquals(setOf(shop), cache.memberships(owner).first().getOrThrow())
        assertNull(cache.peek(owner))
    }

    @Test fun anIncompleteOldReadNeverBecomesAReadyFullListAndFailureKeepsKnownMemberships() = runTest {
        val cache = ServerFavoritesCache { owner }
        cache.recordRows(owner, listOf(entry), cache.revision())
        val previousRead = cache.revision()
        val added = FavoriteTarget(FavoriteKind.Roaster, "new")
        cache.confirm(owner, added, true)
        assertNull(cache.recordRows(owner, listOf(entry), previousRead))
        assertNull(cache.peek(owner))
        cache.fail(owner, IllegalStateException("offline"))
        assertTrue(cache.rows(owner).first().isFailure)
        assertEquals(setOf(shop, added), cache.memberships(owner).first().getOrThrow())
    }

    @Test fun replacingTheSameAccountSessionRejectsItsOldConfirmation() {
        var active = owner
        val cache = ServerFavoritesCache { active }
        cache.confirm(owner, shop, true)
        active = owner.copy(generation = 2)
        assertNull(cache.cachedMembership(shop))
        assertFalse(cache.confirm(owner, shop, false))
        cache.confirm(active, shop, false)
        assertFalse(cache.cachedMembership(shop)!!)
    }

    @Test fun anAuthoritativeGetStartedAfterTheWriteCanReconcileExternalChanges() {
        val cache = ServerFavoritesCache { owner }
        cache.confirm(owner, shop, true)
        cache.recordRows(owner, emptyList(), cache.revision())
        assertFalse(cache.cachedMembership(shop)!!)
    }
}
