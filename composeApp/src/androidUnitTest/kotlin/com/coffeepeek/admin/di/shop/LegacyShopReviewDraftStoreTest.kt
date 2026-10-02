package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.settings.ReviewDraft
import com.coffeepeek.admin.settings.ReviewDraftStore
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.model.ShopReviewPhoto
import com.coffeepeek.feature.shop.impl.ui.data.ShopReviewDraftSnapshot
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import com.coffeepeek.room.utils.JsonExt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LegacyShopReviewDraftStoreTest {
    private class Settings : SettingRepository {
        val rows = mutableMapOf<String, Setting>()
        override suspend fun save(model: Setting) { rows[model.key] = model }
        override suspend fun read(key: String): Setting? = rows[key]
        override fun readFlow(key: String): Flow<Setting?> = flowOf(rows[key])
        override suspend fun readAll(): List<Setting> = rows.values.toList()
        override fun readAllFlow(): Flow<List<Setting>> = flowOf(rows.values.toList())
        override suspend fun delete(key: String) { rows.remove(key) }
    }

    @Test fun readsExistingKeyAndKeepsPhotoBytesInLegacyMemoryUntilClear() = runTest {
        val settings = Settings()
        val key = ReviewDraftStore.newReviewKey("shop-1")
        settings.save(Setting(key, JsonExt.json.encodeToString(ReviewDraft(
            "Old title", "Old comment", 3, 4, 5, System.currentTimeMillis()))))
        val adapter = createLegacyShopReviewCreateDraftStore("shop-1", ReviewDraftStore(settings))

        val restored = adapter.load().getOrThrow()!!
        assertEquals("Old title", restored.header)
        assertEquals(ShopRating(3, 4, 5), restored.rating)
        val photo = ShopReviewPhoto(byteArrayOf(1, 2, 3), "coffee.jpg")
        assertTrue(adapter.save(ShopReviewDraftSnapshot("New title", "New comment",
            ShopRating(5, 5, 5), listOf(photo))).isSuccess)
        assertEquals(photo, adapter.load().getOrThrow()?.photos?.single())

        assertTrue(adapter.clear().isSuccess)
        assertNull(adapter.load().getOrThrow())
        assertNull(settings.read(key))
    }

    @Test fun editAdapterUsesPublishedReviewIdKeyWithoutTouchingCreateDraft() = runTest {
        val settings = Settings()
        val createKey = ReviewDraftStore.newReviewKey("shop-1")
        val editKey = ReviewDraftStore.editReviewKey("published-1")
        settings.save(Setting(createKey, JsonExt.json.encodeToString(ReviewDraft(
            "Create", "Create comment", 4, 4, 4, System.currentTimeMillis()))))
        settings.save(Setting(editKey, JsonExt.json.encodeToString(ReviewDraft(
            "Edited", "Edited comment", 5, 4, 3, System.currentTimeMillis()))))
        val adapter = createLegacyShopReviewEditDraftStore("published-1", ReviewDraftStore(settings))

        assertEquals("Edited", adapter.load().getOrThrow()?.header)
        assertTrue(adapter.clear().isSuccess)
        assertNull(settings.read(editKey))
        assertTrue(settings.read(createKey) != null)
    }
}
