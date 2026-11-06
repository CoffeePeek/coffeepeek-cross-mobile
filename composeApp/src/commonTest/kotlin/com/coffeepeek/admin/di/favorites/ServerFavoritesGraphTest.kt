package com.coffeepeek.admin.di.favorites

import com.coffeepeek.core.session.Session
import com.coffeepeek.feature.favorites.domain.model.FavoriteKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/** Actual shared production module; no copied definitions, platform storage, UI or HTTP. */
class ServerFavoritesGraphTest {
    @Test fun graphResolvesAllKindsWithoutOpeningReaders() {
        val fixture = ServerFavoritesGraphFixture()
        try {
            assertSame(fixture.shops, fixture.shops)
            assertSame(fixture.roasters, fixture.roasters)
            assertEquals(0, fixture.server.reads)
            assertEquals(0, fixture.server.observations)
            assertEquals(0, fixture.catalogs.reads)
            assertTrue(fixture.server.writes.isEmpty())
        } finally { fixture.close() }
    }

    @Test fun favoriteKindsReadAndRemoveThroughTheSameExistingServerSource() = runTest {
        val fixture = ServerFavoritesGraphFixture()
        try {
            val shop = fixture.shops.observe().first { it.getOrNull()?.isNotEmpty() == true }.getOrThrow().single()
            val roaster = fixture.roasters.observe().first { it.getOrNull()?.isNotEmpty() == true }.getOrThrow().single()
            assertEquals("canonical", shop.publicSlug)
            assertEquals("roast", roaster.publicSlug)
            fixture.shops.remove(shop.id).getOrThrow()
            fixture.roasters.remove(roaster.id).getOrThrow()
            assertEquals(listOf<Triple<FavoriteKind, String?, Boolean>>(
                Triple(FavoriteKind.CoffeeShop, "canonical", false),
                Triple(FavoriteKind.Roaster, "roast", false),
            ), fixture.server.writes)
        } finally { fixture.close() }
    }

    @Test fun clearingLocalStateDoesNotDeleteServerMembership() = runTest {
        val fixture = ServerFavoritesGraphFixture()
        try {
            val before = fixture.server.state.value
            fixture.shops.clear().getOrThrow()
            assertEquals(1, fixture.server.clears)
            assertEquals(before, fixture.server.state.value)
            assertTrue(fixture.server.writes.isEmpty())
        } finally { fixture.close() }
    }

    @Test fun replacedAccountCannotRemoveRowsCapturedByThePreviousOwner() = runTest {
        val fixture = ServerFavoritesGraphFixture()
        try {
            val roaster = fixture.roasters.observe().first { it.getOrNull()?.isNotEmpty() == true }.getOrThrow().single()
            fixture.sessions.state.value = Session("new-token", userId = "B")
            assertFailsWith<CancellationException> { fixture.roasters.remove(roaster.id) }
            fixture.server.afterRead = { fixture.sessions.state.value = Session("third-token", userId = "C") }
            assertFailsWith<CancellationException> { fixture.shops.remove("canonical") }
            assertTrue(fixture.server.writes.isEmpty())
        } finally { fixture.close() }
    }
}
