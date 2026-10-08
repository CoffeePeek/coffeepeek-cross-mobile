package com.coffeepeek.admin.ui.screen.shop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CheckInDraftStoreTest {

    @Test
    fun newDraftUsesVisibleFourStarDefaults() {
        val draft = CheckInDraftStore(now = { 1_000L }).open("shop-a")

        assertEquals(4, draft.coffeeRating)
        assertEquals(4, draft.serviceRating)
        assertEquals(4, draft.placeRating)
    }

    @Test
    fun reopeningSameShopKeepsDraft() {
        val store = CheckInDraftStore(now = { 1_000L })
        val initial = store.open("shop-a")
        store.save(initial.copy(note = "Вернусь сюда снова"))

        val reopened = store.open("shop-a")

        assertEquals("Вернусь сюда снова", reopened.note)
        assertEquals(1_000L, reopened.visitMillis)
    }

    @Test
    fun openingAnotherShopReplacesDraft() {
        var now = 1_000L
        val store = CheckInDraftStore(now = { now })
        val first = store.open("shop-a")
        store.save(first.copy(note = "Черновик"))

        now = 2_000L
        val second = store.open("shop-b")

        assertEquals("shop-b", second.shopId)
        assertEquals("", second.note)
        assertEquals(2_000L, second.visitMillis)
        assertNotEquals(first.shopId, second.shopId)
    }

    @Test
    fun completedDraftIsCleared() {
        var now = 1_000L
        val store = CheckInDraftStore(now = { now })
        val initial = store.open("shop-a")
        store.save(initial.copy(note = "Сохранённая заметка"))
        store.clear("shop-a")

        now = 2_000L
        val reopened = store.open("shop-a")

        assertEquals("", reopened.note)
        assertEquals(2_000L, reopened.visitMillis)
    }
}
