package com.coffeepeek.admin.ui.screen.shop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CheckInDraftStoreTest {

    @Test
    fun confirmingVisitDateKeepsTheExactTimeAndOtherDraftFields() {
        val timestamp = kotlin.time.Instant.parse("2026-10-14T20:37:42.123Z").toEpochMilliseconds()
        val draft = CheckInDraft("shop-a", visitMillis = timestamp, note = "Кофе", coffeeRating = 5)
        assertEquals(draft, draft.withVisitDate(
            com.coffeepeek.admin.utils.instantToDatePickerMillis(timestamp), now = timestamp + 60_000))
    }

    @Test
    fun restoringADraftAndChoosingTodayDoesNotPlaceTheVisitInTheFuture() {
        val timestamp = kotlin.time.Instant.parse("2026-10-13T20:37:42.123Z").toEpochMilliseconds()
        val today = com.coffeepeek.admin.utils.instantToDatePickerMillis(timestamp + 86_400_000)
        val adjusted = com.coffeepeek.admin.utils.datePickerMillisWithLocalTime(today, timestamp)
        val now = adjusted - 60_000
        val draft = CheckInDraft("shop-a", visitMillis = timestamp)
        assertEquals(now, draft.withVisitDate(today, now).visitMillis)
    }

    @Test
    fun newDraftUsesVisibleFourStarDefaults() {
        val draft = CheckInDraftStore(now = { 1_000L }).open("shop-a")

        assertEquals(4, draft.coffeeRating)
        assertEquals(4, draft.serviceRating)
        assertEquals(4, draft.placeRating)
        assertEquals(true, draft.isPublic)
    }

    @Test
    fun reopeningSameShopKeepsDraft() {
        val store = CheckInDraftStore(now = { 1_000L })
        val initial = store.open("shop-a")
        store.save(initial.copy(note = "Вернусь сюда снова", isPublic = false))

        val reopened = store.open("shop-a")

        assertEquals("Вернусь сюда снова", reopened.note)
        assertEquals(false, reopened.isPublic)
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
