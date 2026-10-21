package com.coffeepeek.admin.di.favorites

import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.ShopLocation
import com.coffeepeek.feature.favorites.data.local.FavoritesStorage
import com.coffeepeek.feature.favorites.data.repository.createFavoritesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LegacyFavoritesRepositoryBridgeTest {
    private class Storage(initial: String? = null) : FavoritesStorage {
        val value = MutableStateFlow(initial)
        var readFailure: Exception? = null
        var writeFailure: Exception? = null

        override suspend fun read(key: String): String? {
            assertEquals("local_favorite_shops", key)
            readFailure?.let { throw it }
            return value.value
        }

        override fun observe(key: String) = value

        override suspend fun write(key: String, value: String?) {
            assertEquals("local_favorite_shops", key)
            writeFailure?.let { throw it }
            this.value.value = value
        }
    }

    @Test fun legacyConsumersAndNewRepositoryShareSavedSnapshot() = runTest {
        val storage = Storage()
        val repository = createFavoritesRepository(storage, UnconfinedTestDispatcher(testScheduler))
        val legacy = createLegacyFavoritesRepositoryBridge(repository)
        val shop = CoffeeShop(
            id = "cafe", title = "Coffee", rating = 4.5, reviewCount = 8,
            cityName = "Minsk", priceRange = "Moderate", photoUrl = "photo",
            address = "old", isOpen = true, tags = listOf("quiet"),
            brewMethods = listOf("v60"), roasterPhotoUrls = listOf("logo"),
            location = ShopLocation(latitude = 53.9, longitude = 27.5),
        )

        legacy.addFavorite(shop, "new address").getOrThrow()
        val saved = repository.read().getOrThrow().single()
        assertEquals("new address", saved.address)
        assertEquals(53.9, saved.latitude)
        assertEquals(27.5, saved.longitude)
        assertEquals(listOf("logo"), saved.roasterPhotoUrls)
        assertEquals(setOf("cafe"), legacy.getFavoriteIds())
        assertTrue(legacy.isFavorite("cafe"))
        assertFalse(legacy.isFavorite("other"))

        val details = legacy.getFavorites().getOrThrow().single()
        assertEquals(shop.copy(address = "new address", isFavorite = true,
            location = ShopLocation("new address", 53.9, 27.5)), details.shop)
        assertEquals(details.shop.location, details.location)

        repository.remove("cafe").getOrThrow()
        assertEquals(emptySet(), legacy.getFavoriteIds())
        assertEquals(null, storage.value.value)
    }

    @Test fun oldSavedRowsAndSingularLogoRemainReadable() = runTest {
        val storage = Storage("""[{"id":"old","title":"Old","roasterPhotoUrl":"legacy-logo"}]""")
        val repository = createFavoritesRepository(storage, UnconfinedTestDispatcher(testScheduler))
        val legacy = createLegacyFavoritesRepositoryBridge(repository)

        assertEquals(listOf("legacy-logo"), legacy.getFavorites().getOrThrow().single().shop.roasterPhotoUrls)
        assertEquals(setOf("old"), legacy.getFavoriteIds())
        legacy.clearAll()
        assertEquals(null, storage.value.value)
    }

    @Test fun corruptDataNeverBecomesEmptyOrOverwrittenByLegacyOperations() = runTest {
        val storage = Storage("corrupt")
        val repository = createFavoritesRepository(storage, UnconfinedTestDispatcher(testScheduler))
        val legacy = createLegacyFavoritesRepositoryBridge(repository)

        assertTrue(legacy.getFavorites().isFailure)
        assertEquals(emptySet(), legacy.getFavoriteIds())
        assertTrue(legacy.addFavorite(CoffeeShop("new", "New", null, cityName = null,
            priceRange = null, photoUrl = null)).isFailure)
        assertTrue(legacy.removeFavorite("old").isFailure)
        assertEquals("corrupt", storage.value.value)

        legacy.clearAll() // Explicit session cleanup may delete corrupt data.
        assertEquals(null, storage.value.value)
    }

    @Test fun failuresAndCancellationPropagateAcrossCompatibilityBoundary() = runTest {
        val storage = Storage()
        val repository = createFavoritesRepository(storage, UnconfinedTestDispatcher(testScheduler))
        val legacy = createLegacyFavoritesRepositoryBridge(repository)

        storage.readFailure = IllegalStateException("read failed")
        assertTrue(legacy.getFavorites().isFailure)
        assertFalse(legacy.isFavorite("id"))
        storage.readFailure = CancellationException("cancelled")
        assertFailsWith<CancellationException> { legacy.getFavorites() }
        assertFailsWith<CancellationException> { legacy.isFavorite("id") }
        storage.readFailure = null
        storage.writeFailure = IllegalStateException("write failed")
        assertFailsWith<IllegalStateException> { legacy.clearAll() }
    }
}
