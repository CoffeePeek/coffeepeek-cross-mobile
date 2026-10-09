package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.ui.screen.shop.CheckInDraftStore
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility
import com.coffeepeek.feature.shop.domain.model.ShopRating
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class AndroidShopCheckInDraftStoreTest {
    private val now = Instant.parse("2026-10-09T12:00:00Z").toEpochMilli()
    private val dates = AndroidShopCheckInDateFormatter({ ZoneId.of("Europe/Minsk") }, { now })
    private fun initial(shop: String = "shop") = ShopCheckInCreateInput(shop, "", ShopRating(4, 4, 4), dates.visitInstant(now))

    @Test fun restoresEveryLegacyFieldWithoutAddingAnotherStore() {
        val legacy = CheckInDraftStore { now }
        val draft = legacy.open("shop").copy(note = "Фильтр", isPublic = true,
            coffeeRating = 5, serviceRating = 3, placeRating = 4, drinkSlug = "other",
            customDrinkName = "Тоник", drinkName = "Другое",
            photos = listOf(PickedImage(byteArrayOf(1, 2), "visit.png", "image/png")))
        legacy.save(draft)
        val adapter = AndroidShopCheckInDraftStore(legacy, dates)
        val restored = adapter.open(initial()).getOrThrow().input
        assertEquals("Фильтр", restored.text)
        assertEquals(ShopRating(coffee = 5, service = 3, place = 4), restored.rating)
        assertEquals(ShopCheckInVisibility.Public, restored.visibility)
        assertEquals("Тоник", restored.customDrinkName)
        assertEquals(ShopCheckInPhoto(byteArrayOf(1, 2), "visit.png", "image/png"), restored.photos.single())
        adapter.save(adapter.open(initial()).getOrThrow().copy(input = restored.copy(text = "Updated"))).getOrThrow()
        assertEquals("Updated", legacy.peek()!!.note)
        assertEquals(now, legacy.peek()!!.visitMillis)
        assertEquals("Другое", legacy.peek()!!.drinkName)
    }

    @Test fun keepsDeliveryGuardOnReopenButResetsItAfterLogoutAndShopReplacement() {
        val legacy = CheckInDraftStore { now }
        val adapter = AndroidShopCheckInDraftStore(legacy, dates)
        val first = adapter.open(initial()).getOrThrow()
        adapter.save(first.copy(deliveryUnconfirmed = true)).getOrThrow()
        assertTrue(adapter.open(initial()).getOrThrow().deliveryUnconfirmed)
        legacy.clearAll()
        assertFalse(adapter.open(initial()).getOrThrow().deliveryUnconfirmed)
        adapter.save(adapter.open(initial()).getOrThrow().copy(deliveryUnconfirmed = true)).getOrThrow()
        assertFalse(adapter.open(initial("other-shop")).getOrThrow().deliveryUnconfirmed)
        assertEquals("other-shop", legacy.peek()!!.shopId)
    }

    @Test fun staleSaveAndCleanupCannotRecreateOrEraseAnotherDraft() {
        val legacy = CheckInDraftStore { now }
        val adapter = AndroidShopCheckInDraftStore(legacy, dates)
        val stale = adapter.open(initial()).getOrThrow()
        legacy.clearAll()
        assertTrue(adapter.save(stale).isFailure)
        assertEquals(null, legacy.peek())
        val replacement = legacy.open("shop").copy(note = "New account draft")
        legacy.save(replacement)
        assertTrue(adapter.clear("shop").isFailure)
        assertEquals(replacement, legacy.peek())
    }

    @Test fun clearingSuccessfulSubmissionUsesTheSameStoreAndNextOpenHasDefaults() {
        val legacy = CheckInDraftStore { now }
        val adapter = AndroidShopCheckInDraftStore(legacy, dates)
        adapter.save(adapter.open(initial()).getOrThrow().copy(deliveryUnconfirmed = true)).getOrThrow()
        adapter.clear("shop").getOrThrow()
        assertEquals(null, legacy.peek())
        val fresh = adapter.open(initial()).getOrThrow()
        assertFalse(fresh.deliveryUnconfirmed)
        assertEquals(ShopRating(4, 4, 4), fresh.input.rating)
        assertEquals(ShopCheckInVisibility.Private, fresh.input.visibility)
    }

    @Test fun selectedDateRoundTripsInBothPositiveAndNegativeTimezones() {
        val selected = Instant.parse("2026-06-15T00:00:00Z").toEpochMilli()
        for (zone in listOf("Europe/Minsk", "America/New_York")) {
            val formatter = AndroidShopCheckInDateFormatter({ ZoneId.of(zone) }, { now })
            assertEquals(selected, formatter.pickerMillis(formatter.visitInstant(selected)))
            val legacy = CheckInDraftStore { now }
            val adapter = AndroidShopCheckInDraftStore(legacy, formatter)
            val first = adapter.open(initial()).getOrThrow()
            adapter.save(first.copy(input = first.input.copy(visitedAtIso = formatter.visitInstant(selected)))).getOrThrow()
            assertEquals(selected, legacy.peek()!!.visitMillis)
            assertEquals(first.input.shopSlug, adapter.open(initial()).getOrThrow().input.shopSlug)
        }
        assertEquals("2026-06-14T21:00:00Z", dates.visitInstant(selected))
        assertEquals(null, dates.pickerMillis("invalid"))
    }

    @Test fun cancellationIsNotConvertedIntoStorageFailure() {
        val legacy = CheckInDraftStore { throw CancellationException("cancel") }
        assertFailsWith<CancellationException> { AndroidShopCheckInDraftStore(legacy, dates).open(initial()) }
    }
}
