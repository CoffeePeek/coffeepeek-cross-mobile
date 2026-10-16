package com.coffeepeek.feature.favorites.data.repository

import com.coffeepeek.feature.favorites.data.local.FavoritesStorage
import com.coffeepeek.feature.favorites.data.local.model.StoredFavorite
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FavoritesRepositoryTest {
    private fun kotlinx.coroutines.test.TestScope.repository(storage: FavoritesStorage) =
        createFavoritesRepository(storage, UnconfinedTestDispatcher(testScheduler))
    private class Storage(initial: String? = null) : FavoritesStorage {
        val state = MutableStateFlow(initial)
        var failWrite: Exception? = null
        var writes = 0
        override suspend fun read(key: String): String? { assertEquals("local_favorite_shops", key); yield(); return state.value }
        override fun observe(key: String) = state
        override suspend fun write(key: String, value: String?) {
            assertEquals("local_favorite_shops", key)
            failWrite?.let { throw it }
            writes++; state.value = value
        }
    }

    @Test fun readsLegacySingularLogoAndMissingOptionalFields() = runTest {
        val repo = repository(Storage("""[{"id":"old","title":"Кофе","roasterPhotoUrl":"logo","unknown":1}]"""))
        val shop = repo.read().getOrThrow().single()
        assertEquals(listOf("logo"), shop.roasterPhotoUrls)
        assertEquals(0, shop.reviewCount)
        assertNull(shop.latitude)
    }

    @Test fun duplicateStoredIdsKeepFirstNewestSnapshotForStableUiKeys() = runTest {
        val repo = repository(Storage("""[{"id":"a","title":"Newest"},{"id":"a","title":"Older"}]"""))
        assertEquals(listOf("Newest"), repo.read().getOrThrow().map { it.title })
    }

    @Test fun roundTripsEverySavedFieldAndWritesLegacyCompatibleJson() = runTest {
        val storage = Storage()
        val repo = repository(storage)
        val shop = FavoriteShop("id", "Кофе", 4.8, 21, "Минск", "$$", "photo", "address",
            53.9, 27.5, true, listOf("tag"), listOf("espresso"), listOf("logo1", "logo2"))
        repo.save(shop).getOrThrow()
        assertEquals(shop, repo.read().getOrThrow().single())
        val old = Json.decodeFromString<List<StoredFavorite>>(storage.state.value!!).single()
        assertEquals("logo1", old.roasterPhotoUrl)
        assertEquals(shop.roasterPhotoUrls, old.roasterPhotoUrls)
    }

    @Test fun replaceMovesToFrontWithoutDuplicateAndLastRemovalDeletesKey() = runTest {
        val storage = Storage()
        val repo = repository(storage)
        repo.save(FavoriteShop("a", "A")).getOrThrow()
        repo.save(FavoriteShop("b", "B")).getOrThrow()
        repo.save(FavoriteShop("a", "Updated")).getOrThrow()
        assertEquals(listOf("a", "b"), repo.read().getOrThrow().map { it.id })
        repo.remove("a").getOrThrow(); repo.remove("b").getOrThrow()
        assertNull(storage.state.value)
    }

    @Test fun corruptStorageIsFailureAndMutationNeverOverwritesIt() = runTest {
        val storage = Storage("broken json")
        val repo = repository(storage)
        assertTrue(repo.read().isFailure)
        assertTrue(repo.observe().first().isFailure)
        assertTrue(repo.save(FavoriteShop("a", "A")).isFailure)
        assertTrue(repo.remove("a").isFailure)
        assertEquals("broken json", storage.state.value)
        assertEquals(0, storage.writes)
    }

    @Test fun cancellationOfWriteIsRethrownWithoutChangingStoredData() = runTest {
        val storage = Storage("[]")
        storage.failWrite = CancellationException("cancel")
        val repo = repository(storage)
        assertFailsWith<CancellationException> { repo.save(FavoriteShop("a", "A")) }
        assertFailsWith<CancellationException> { repo.remove("a") }
        assertFailsWith<CancellationException> { repo.clear() }
        assertEquals("[]", storage.state.value)
    }

    @Test fun concurrentWritesOnOneRepositoryDoNotLoseFavorites() = runTest {
        val repo = repository(Storage())
        val a = async { repo.save(FavoriteShop("a", "A")).getOrThrow() }
        val b = async { repo.save(FavoriteShop("b", "B")).getOrThrow() }
        a.await(); b.await()
        assertEquals(setOf("a", "b"), repo.read().getOrThrow().map { it.id }.toSet())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun observationIncludesExternalWritesAndClearWithoutGlobalEventBus() = runTest {
        val storage = Storage()
        val repo = repository(storage)
        val collected = async(UnconfinedTestDispatcher(testScheduler)) { repo.observe().take(3).toList() }
        storage.state.value = """[{"id":"external","title":"External"}]"""
        repo.clear().getOrThrow()
        assertEquals(listOf(emptyList(), listOf("external"), emptyList()), collected.await().map { result ->
            result.getOrThrow().map { it.id }
        })
    }

    @Test fun blankIdsAndOrdinaryWriteFailuresAreExplicitFailures() = runTest {
        val storage = Storage()
        val repo = repository(storage)
        assertTrue(repo.save(FavoriteShop("", "Invalid")).isFailure)
        assertTrue(repo.remove(" ").isFailure)
        storage.failWrite = IllegalStateException("disk full")
        assertTrue(repo.save(FavoriteShop("a", "A")).isFailure)
        assertEquals(0, storage.writes)
    }

    @Test fun readCancellationPropagatesAndObservationSetupFailuresAreResults() = runTest {
        val storage = object : FavoritesStorage {
            override suspend fun read(key: String): String? = throw CancellationException("cancel")
            override fun observe(key: String): kotlinx.coroutines.flow.Flow<String?> = throw IllegalStateException("offline")
            override suspend fun write(key: String, value: String?) = Unit
        }
        val repo = repository(storage)
        assertFailsWith<CancellationException> { repo.read() }
        assertTrue(repo.observe().first().isFailure)
    }

    @Test fun observationSetupCancellationIsNotEmittedAsFailure() = runTest {
        val storage = object : FavoritesStorage {
            override suspend fun read(key: String): String? = null
            override fun observe(key: String): kotlinx.coroutines.flow.Flow<String?> = throw CancellationException("cancel")
            override suspend fun write(key: String, value: String?) = Unit
        }
        assertFailsWith<CancellationException> { repository(storage).observe().first() }
    }
}
