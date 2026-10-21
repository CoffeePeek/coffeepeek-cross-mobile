@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.di.roaster

import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.domain.model.PublicAddress
import com.coffeepeek.feature.roaster.api.RoasterFavoriteTarget
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
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

private class FakeShops(private val details: suspend (String) -> Result<CoffeeShopDetails>) : ShopRepository {
    override suspend fun getShopDetails(id: String) = details(id)
    override suspend fun searchShops(filters: com.coffeepeek.domain.model.ShopFilters) = TODO()
    override suspend fun getMapContent(bounds: com.coffeepeek.domain.model.MapBounds, zoom: Float, filters: com.coffeepeek.domain.model.ShopFilters) = TODO()
    override suspend fun getCatalogs() = TODO()
    override suspend fun getConsumedDrinks() = TODO()
    override suspend fun getMenuDrinks() = TODO()
    override suspend fun createShop(input: com.coffeepeek.domain.model.CreateShopInput) = TODO()
    override suspend fun getMyShopSubmissions(status: com.coffeepeek.domain.model.ModerationStatus, page: Int, pageSize: Int) = TODO()
}

class RoasterPortsTest {
    private val target = RoasterFavoriteTarget("roast", "Roast", "https://m/p", "/roasters/roast", 3, true)

    @Test fun favoriteKeysUseTheLegacyRoasterFavoriteIdAndErrorsPassThrough() = runTest {
        val store = FakeFavorites()
        val port = RoasterFavoritesAdapter(store, backgroundScope)
        store.rows.value = listOf(
            CatalogItem("guid-1", "A", address = PublicAddress("a", "/roasters/a", 1, false)), // favorite id = slug
            CatalogItem("guid-2", "B"), // no address: falls back to the id
        )
        assertEquals(setOf("a", "guid-2"), port.observeKeys().first())
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

    @Test fun shopPhotoUsesFirstNonBlankPhotoThenShopPhotoAndNeverThrows() = runTest {
        fun shop(photoUrl: String?) = CoffeeShop(id = "s", title = "Shop", rating = null, cityName = null, priceRange = null, photoUrl = photoUrl)
        fun details(photos: List<String>, shopPhoto: String? = null) = CoffeeShopDetails(shop = shop(shopPhoto), photos = photos)
        val shops = mapOf(
            "photos" to Result.success(details(listOf(" ", "https://m/hero", "https://m/other"))),
            "cover" to Result.success(details(emptyList(), "https://m/cover")),
            "blank" to Result.success(details(listOf(""), " ")),
            "failed" to Result.failure(IllegalStateException("net")),
        )
        val adapter = RoasterShopPhotosAdapter(FakeShops { shops.getValue(it) })
        assertEquals("https://m/hero", adapter.firstPhoto("photos"))
        assertEquals("https://m/cover", adapter.firstPhoto("cover"))
        assertNull(adapter.firstPhoto("blank"))
        assertNull(adapter.firstPhoto("failed"))
        assertFailsWith<CancellationException> { RoasterShopPhotosAdapter(FakeShops { throw CancellationException("stop") }).firstPhoto("x") }
    }

    @Test fun catalogStoreUsesTheLegacyKeyAndIgnoresBlankOrNullValues() = runTest {
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
