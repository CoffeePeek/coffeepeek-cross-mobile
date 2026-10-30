package com.coffeepeek.admin.feature.favorites

import com.coffeepeek.admin.feature.favorites.data.ServerRoasterFavorites
import com.coffeepeek.admin.di.favorites.toFavoriteAddress
import com.coffeepeek.admin.di.favorites.toPublicAddress
import com.coffeepeek.feature.favorites.domain.model.FavoriteEntry
import com.coffeepeek.feature.favorites.domain.model.FavoriteKind
import com.coffeepeek.feature.favorites.domain.repository.AccountFavorites
import com.coffeepeek.domain.model.*
import com.coffeepeek.feature.roaster.api.PublicRoasterDescription
import com.coffeepeek.feature.roaster.api.PublicRoasterDetails
import com.coffeepeek.feature.roaster.api.PublicRoasterSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServerRoasterFavoritesTest {
    @Test fun failedInitialLoadIsVisibleAndTheSameSubscriptionRecoversAfterRetry() = runBlocking {
        val state = FavoriteEntries()
        state.value.value = Result.failure(IllegalStateException("Offline"))
        val address = PublicAddress("roast", "/roasters/roast", 1, false)
        val favorites = ServerRoasterFavorites(state, roasters(listOf(PublicRoasterSummary("roast", null, emptyList(), 0, 0, 0, "Roast"))))
        val updates = Channel<List<CatalogItem>>(Channel.UNLIMITED)
        val observer = launch(start = CoroutineStart.UNDISPATCHED) {
            favorites.observeFavorites().collect { updates.send(it) }
        }
        try {
            assertTrue(withTimeout(2_000) { updates.receive() }.isEmpty())
            assertEquals("Не удалось загрузить избранных обжарщиков", favorites.observeErrors().first())
            state.value.value = Result.success(listOf(FavoriteEntry(FavoriteKind.Roaster, address.toFavoriteAddress())))
            assertEquals("Roast", withTimeout(2_000) { updates.receive() }.single().name)
            assertEquals(null, favorites.observeErrors().first())
        } finally {
            observer.cancelAndJoin()
            updates.close()
        }
    }

    @Test fun hydratesOnlyServerSavedRoastersAndMutatesByTheirPublicAddress() = runBlocking {
        val state = FavoriteEntries()
        val address = PublicAddress("public-roast", "/roasters/public-roast", 1, false)
        state.value.value = Result.success(listOf(FavoriteEntry(FavoriteKind.Roaster, address.toFavoriteAddress()), FavoriteEntry(FavoriteKind.CoffeeShop, address.toFavoriteAddress())))
        val roasters = roasters(listOf(PublicRoasterSummary("public-roast", "photo.png", emptyList(), 7, 0, 0, "Roast")))
        val favorites = ServerRoasterFavorites(state, roasters)
        val saved = favorites.observeFavorites().first().single()
        assertEquals("Roast", saved.name)
        assertEquals("photo.png", saved.photoUrl)
        assertEquals(7, saved.coffeeShopsCount)
        assertEquals(address, saved.address)
        favorites.setFavorite(saved.copy(id = "catalog-guid"), false).getOrThrow()
        assertEquals(Triple(FavoriteKind.Roaster, address, false), state.lastMutation)
        assertTrue(favorites.setFavorite(CatalogItem("legacy-guid", "Legacy"), true).isFailure)
    }

    @Test fun preservesRemovablePublicAddressWhenCatalogMetadataIsUnavailable() = runBlocking {
        val state = FavoriteEntries()
        val address = PublicAddress("retired-roast", "/roasters/retired-roast", 1, false)
        state.value.value = Result.success(listOf(FavoriteEntry(FavoriteKind.Roaster, address.toFavoriteAddress())))
        val saved = ServerRoasterFavorites(state, roasters(emptyList())).observeFavorites().first().single()
        assertEquals("retired-roast", saved.name)
        assertEquals(address, saved.address)
    }

    @Test fun keepsServerDisplayFieldsForRoastersWithoutAddressAndFailedHydration() = runBlocking {
        val state = FavoriteEntries()
        state.value.value = Result.success(listOf(
            FavoriteEntry(FavoriteKind.Roaster, name = "Архивный обжарщик", coverPhotoUrl = "https://media.example/logo.jpg", createdAtUtc = "2026-10-15T10:00:00Z"),
        ))
        val saved = ServerRoasterFavorites(state, roasters(emptyList())).observeFavorites().first().single()
        assertEquals("Архивный обжарщик", saved.name)
        assertEquals("https://media.example/logo.jpg", saved.photoUrl)
        assertEquals(null, saved.address)
    }

    private fun roasters(catalog: List<PublicRoasterSummary>) = object : PublicRoasterDetails {
        override suspend fun summaries() = Result.success(catalog)
        override suspend fun load(slug: String): Result<PublicRoasterDescription> = Result.failure(IllegalStateException("Unavailable"))
    }
}

private class FavoriteEntries : AccountFavorites {
    val value = MutableStateFlow<Result<List<FavoriteEntry>>>(Result.success(emptyList()))
    var lastMutation: Triple<FavoriteKind, PublicAddress, Boolean>? = null
    override fun observeFavorites() = value
    override suspend fun getFavorites(refresh: Boolean) = value.value
    override suspend fun setFavorite(entry: FavoriteEntry, favorite: Boolean): Result<Unit> {
        lastMutation = Triple(entry.kind, entry.address!!.toPublicAddress(), favorite)
        return Result.success(Unit)
    }
    override fun clearCache() = Unit
}
