@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.di.roaster

import com.coffeepeek.admin.di.favorites.RoasterFavorites
import com.coffeepeek.admin.di.favorites.CatalogItem
import com.coffeepeek.admin.di.collections.FavoriteCollectionsPrompt
import com.coffeepeek.feature.shop.api.PublicShopPreview
import com.coffeepeek.feature.shop.api.PublicShopPreviews
import com.coffeepeek.admin.di.favorites.PublicAddress
import com.coffeepeek.feature.favorites.domain.model.FavoriteEntry
import com.coffeepeek.feature.favorites.domain.model.FavoriteKind
import com.coffeepeek.feature.favorites.domain.model.FavoriteTarget
import com.coffeepeek.feature.favorites.domain.repository.AccountFavorites
import com.coffeepeek.feature.roaster.api.RoasterFavoriteTarget
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

internal class MemorySettings : SettingRepository {
    val values = mutableMapOf<String, String>()
    override suspend fun save(model: Setting) { values[model.key] = model.value }
    override suspend fun read(key: String) = values[key]?.let { Setting(key, it) }
    override fun readFlow(key: String): Flow<Setting?> = MutableStateFlow(values[key]?.let { Setting(key, it) })
    override suspend fun readAll() = values.map { Setting(it.key, it.value) }
    override fun readAllFlow(): Flow<List<Setting>> = MutableStateFlow(emptyList())
    override suspend fun delete(key: String) { values.remove(key) }
}

private class FakeFavorites : RoasterFavorites {
    val rows = MutableStateFlow<List<CatalogItem>>(emptyList())
    val errors = MutableSharedFlow<String?>(extraBufferCapacity = 4)
    val calls = mutableListOf<Pair<CatalogItem, Boolean>>()
    var result: Result<Unit> = Result.success(Unit)
    override fun observeFavorites() = rows
    override fun observeErrors(): Flow<String?> = errors
    override suspend fun setFavorite(roaster: CatalogItem, isFavorite: Boolean): Result<Unit> { calls += roaster to isFavorite; return result }
}

private class PromptNoop : FavoriteCollectionsPrompt {
    override fun shop(slug: String) {}
    override fun roaster(slug: String) {}
}

private class FakeAccountFavorites : AccountFavorites {
    val memberships = MutableStateFlow<Result<Set<FavoriteTarget>>>(Result.success(emptySet()))
    override fun observeFavorites(): Flow<Result<List<FavoriteEntry>>> = MutableStateFlow(Result.success(emptyList()))
    override fun observeMemberships(): Flow<Result<Set<FavoriteTarget>>> = memberships
    override suspend fun getFavorites(refresh: Boolean) = Result.success(emptyList<FavoriteEntry>())
    override suspend fun setFavorite(entry: FavoriteEntry, favorite: Boolean) = Result.success(Unit)
    override fun clearCache() {}
}

class RoasterPortsTest {
    private val target = RoasterFavoriteTarget("roast", "Roast", "https://m/p", "/roasters/roast", 3, true)

    @Test fun favoriteKeysUseTheExistingRoasterFavoriteIdAndErrorsPassThrough() = runTest {
        val store = FakeFavorites()
        val port = RoasterFavoritesAdapter(store, backgroundScope)
        store.rows.value = listOf(
            CatalogItem("guid-1", "A", address = PublicAddress("a", "/roasters/a", 1, false)), // favorite id = slug
            CatalogItem("guid-2", "B"), // no address: falls back to the id
        )
        assertEquals(setOf("a", "guid-2"), port.observeKeys().first().getOrThrow())
        val seen = async { port.observeErrors().first() }
        runCurrent() // the collector subscribes before the hot flow emits
        store.errors.tryEmit("Лимит")
        assertEquals("Лимит", seen.await())
    }

    @Test fun changeSendsTheFullPublicAddressAndReportsFailureOnly() = runTest {
        val store = FakeFavorites()
        val port = RoasterFavoritesAdapter(store, backgroundScope)
        var failed = 0
        port.change(target, favorite = true) { failed++ }; runCurrent()
        val (item, favorite) = store.calls.single()
        assertEquals(PublicAddress("roast", "/roasters/roast", 3, true), item.address)
        assertEquals(listOf("Roast", "https://m/p", true), listOf(item.name, item.photoUrl, favorite))
        assertEquals(0, failed)
        store.result = Result.failure(IllegalStateException("server"))
        port.change(target, favorite = false) { failed++ }; runCurrent()
        assertEquals(1, failed)
        assertEquals(false, store.calls.last().second)
    }

    @Test fun shopPhotoReadsPublicDetailVariantAndFailsSoftlyButPreservesCancellation() = runTest {
        fun preview(photo: String?) = PublicShopPreview("cover", photo, null, 0, false, null, null)
        val shops = mapOf(
            "photos" to Result.success(preview("https://m/hero")),
            "cover" to Result.success(preview("https://m/cover")),
            "blank" to Result.success(preview(" ")),
            "failed" to Result.failure(IllegalStateException("net")),
            "cancelled" to Result.failure(CancellationException("stop")),
        )
        val adapter = RoasterShopPhotosAdapter(PublicShopPreviews { shops.getValue(it) })
        assertEquals("https://m/hero", adapter.firstPhoto("photos"))
        assertEquals("https://m/cover", adapter.firstPhoto("cover"))
        assertNull(adapter.firstPhoto("blank"))
        assertNull(adapter.firstPhoto("failed"))
        assertFailsWith<CancellationException> { adapter.firstPhoto("cancelled") }
        assertFailsWith<CancellationException> { RoasterShopPhotosAdapter(PublicShopPreviews { throw CancellationException("stop") }).firstPhoto("x") }
    }

    @Test fun membershipFailuresSurfaceAnErrorAndTheNextSuccessKeysKeepFlowing() = runTest {
        val firstFailure = IllegalStateException("Initial request failed")
        val serverFavorites = FakeAccountFavorites().apply { memberships.value = Result.failure(firstFailure) }
        val port = RoasterFavoritesAdapter(FakeFavorites(), backgroundScope, PromptNoop(), serverFavorites)
        val results = mutableListOf<Result<Set<String>>>()
        backgroundScope.launch { port.observeKeys().collect { results += it } }
        runCurrent()
        assertSame(firstFailure, results.single().exceptionOrNull())
        serverFavorites.memberships.value = Result.success(setOf(FavoriteTarget(FavoriteKind.Roaster, "a")))
        runCurrent()
        assertEquals(setOf("a"), results.last().getOrThrow())
        val refreshFailure = IllegalStateException("Refresh failed")
        serverFavorites.memberships.value = Result.failure(refreshFailure)
        runCurrent()
        assertSame(refreshFailure, results.last().exceptionOrNull())
        serverFavorites.memberships.value = Result.success(setOf(FavoriteTarget(FavoriteKind.Roaster, "b")))
        runCurrent()
        assertEquals(setOf("b"), results.last().getOrThrow())
    }

    @Test fun cancelledMembershipDoesNotBecomeAReadFailure() = runTest {
        val cancelled = CancellationException("Account changed")
        val serverFavorites = FakeAccountFavorites().apply { memberships.value = Result.failure(cancelled) }
        val port = RoasterFavoritesAdapter(FakeFavorites(), backgroundScope, PromptNoop(), serverFavorites)
        assertSame(cancelled, assertFailsWith<CancellationException> { port.observeKeys().first() })
    }

    @Test fun catalogStoreUsesTheExistingKeyAndIgnoresBlankOrNullValues() = runTest {
        val settings = MemorySettings()
        val key = roasterCatalogCacheKey("https://api.example/")
        assertEquals("roaster_catalog_v1:https://api.example", key)
        val store = SettingRoasterCatalogStore(settings, key)
        assertNull(store.read())
        store.write("""{"savedAtMs":1,"items":[]}""")
        assertEquals("""{"savedAtMs":1,"items":[]}""", store.read())
        assertEquals(setOf(key), settings.values.keys)
        settings.values[key] = "null"; assertNull(store.read())
        settings.values[key] = " "; assertNull(store.read())
    }
}
