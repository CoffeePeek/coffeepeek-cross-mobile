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

class AppShopCheckInDraftStoreTest {
    private val now = Instant.parse("2026-10-09T12:00:00Z").toEpochMilli()
    private val dates = AndroidShopCheckInDateFormatter({ ZoneId.of("Europe/Minsk") }, { now })
    private fun initial(shop: String = "shop") = ShopCheckInCreateInput(shop, "", ShopRating(4, 4, 4), dates.visitInstant(now))

    @Test fun closedModalDraftNeverMovesToAnotherAccount() {
        val legacy = CheckInDraftStore { now }
        val adapter = AppShopCheckInDraftStore(legacy, dates)
        val old = adapter.forShop("internal-id", "public-address", "first-account")
        val snapshot = old.open(initial("public-address")).getOrThrow()
        old.save(snapshot.copy(input = snapshot.input.copy(text = "Private note"), deliveryUnconfirmed = true)).getOrThrow()
        val current = adapter.forShop("internal-id", "public-address", "second-account")
        val fresh = current.open(initial("public-address")).getOrThrow()
        assertEquals("", fresh.input.text)
        assertFalse(fresh.deliveryUnconfirmed)
        assertTrue(old.open(initial("public-address")).isFailure)
        assertTrue(old.save(snapshot).isFailure)
        assertTrue(old.clear("public-address").isFailure)
        assertEquals("", legacy.peek()!!.note)
    }

    @Test fun sameAccountReopeningKeepsTheInputAndUncertaintyGuard() {
        val legacy = CheckInDraftStore { now }
        val adapter = AppShopCheckInDraftStore(legacy, dates)
        val old = adapter.forShop("internal-id", "public-address", "owner")
        val snapshot = old.open(initial("public-address")).getOrThrow()
        old.save(snapshot.copy(input = snapshot.input.copy(text = "Retain note"), deliveryUnconfirmed = true)).getOrThrow()
        val reopened = adapter.forShop("internal-id", "public-address", "owner").open(initial("public-address")).getOrThrow()
        assertEquals("Retain note", reopened.input.text)
        assertTrue(reopened.deliveryUnconfirmed)
    }

    @Test fun restoresEveryLegacyFieldWithoutAddingAnotherStore() {
        val legacy = CheckInDraftStore { now }
        val draft = legacy.open("shop").copy(note = "Фильтр", isPublic = true,
            coffeeRating = 5, serviceRating = 3, placeRating = 4, drinkSlug = "other",
            customDrinkName = "Тоник", drinkName = "Другое",
            photos = listOf(PickedImage(byteArrayOf(1, 2), "visit.png", "image/png")))
        legacy.save(draft)
        val adapter = AppShopCheckInDraftStore(legacy, dates)
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
        val adapter = AppShopCheckInDraftStore(legacy, dates)
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
        val adapter = AppShopCheckInDraftStore(legacy, dates)
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
        val adapter = AppShopCheckInDraftStore(legacy, dates)
        adapter.save(adapter.open(initial()).getOrThrow().copy(deliveryUnconfirmed = true)).getOrThrow()
        adapter.clear("shop").getOrThrow()
        assertEquals(null, legacy.peek())
        val fresh = adapter.open(initial()).getOrThrow()
        assertFalse(fresh.deliveryUnconfirmed)
        assertEquals(ShopRating(4, 4, 4), fresh.input.rating)
        assertEquals(ShopCheckInVisibility.Public, fresh.input.visibility)
    }

    @Test fun selectedDateRoundTripsInBothPositiveAndNegativeTimezones() {
        val selected = Instant.parse("2026-06-15T00:00:00Z").toEpochMilli()
        for (zone in listOf("Europe/Minsk", "America/New_York")) {
            val formatter = AndroidShopCheckInDateFormatter({ ZoneId.of(zone) }, { now })
            assertEquals(selected, formatter.pickerMillis(formatter.visitInstant(selected)))
            val legacy = CheckInDraftStore { now }
            val adapter = AppShopCheckInDraftStore(legacy, formatter)
            val first = adapter.open(initial()).getOrThrow()
            adapter.save(first.copy(input = first.input.copy(visitedAtIso = formatter.visitInstant(selected)))).getOrThrow()
            assertEquals(Instant.parse(formatter.visitInstant(selected)).toEpochMilli(), legacy.peek()!!.visitMillis)
            assertEquals(first.input.shopSlug, adapter.open(initial()).getOrThrow().input.shopSlug)
        }
        assertEquals("2026-06-14T21:00:00Z", dates.visitInstant(selected))
        assertEquals(null, dates.pickerMillis("invalid"))
    }

    @Test fun todayIsTheLocalDayEvenJustAfterLocalMidnightInAPositiveOffset() {
        // 00:30 on 16 Oct in Minsk (UTC+3) is still 15 Oct in UTC, yet the 16th must be selectable.
        val afterMidnight = Instant.parse("2026-10-15T21:30:00Z").toEpochMilli()
        val minsk = AndroidShopCheckInDateFormatter({ ZoneId.of("Europe/Minsk") }, { afterMidnight })
        assertEquals(Instant.parse("2026-10-16T00:00:00Z").toEpochMilli(), minsk.nowMillis())
        // 23:30 on 15 Oct in New York is already 16 Oct in UTC, yet only the 15th is today.
        val beforeMidnight = Instant.parse("2026-10-16T03:30:00Z").toEpochMilli()
        val newYork = AndroidShopCheckInDateFormatter({ ZoneId.of("America/New_York") }, { beforeMidnight })
        assertEquals(Instant.parse("2026-10-15T00:00:00Z").toEpochMilli(), newYork.nowMillis())
    }

    @Test fun cancellationIsNotConvertedIntoStorageFailure() {
        val legacy = CheckInDraftStore { throw CancellationException("cancel") }
        assertFailsWith<CancellationException> { AppShopCheckInDraftStore(legacy, dates).open(initial()) }
    }

    @Test fun dateChangeKeepsLocalVisitTimeAndRoundTripKeepsTheExactInstant() {
        val reference = "2026-10-09T12:34:56.789Z"
        val selected = Instant.parse("2026-06-15T00:00:00Z").toEpochMilli()
        assertEquals("2026-06-15T12:34:56.789Z", dates.visitInstant(selected, reference))
        val legacy = CheckInDraftStore { now }
        val adapter = AppShopCheckInDraftStore(legacy, dates)
        val first = adapter.open(initial()).getOrThrow()
        val input = first.input.copy(visitedAtIso = reference)
        adapter.save(first.copy(input = input)).getOrThrow()
        assertEquals(reference, adapter.open(initial()).getOrThrow().input.visitedAtIso)
    }

    @Test fun repeatedDstHourKeepsTheExactOriginalOffset() {
        val reference = "2026-10-25T01:30:00Z"
        val formatter = AndroidShopCheckInDateFormatter({ ZoneId.of("Europe/Berlin") },
            { Instant.parse("2026-10-26T00:00:00Z").toEpochMilli() })
        val day = Instant.parse("2026-10-25T00:00:00Z").toEpochMilli()
        assertEquals(reference, formatter.visitInstant(day, reference))
    }

    @Test fun bindingPreservesInternalDraftKeyAndUsesPublicSlugForTransport() {
        val legacy = CheckInDraftStore { now }
        legacy.save(legacy.open("internal-id").copy(note = "Existing draft"))
        val adapter = AppShopCheckInDraftStore(legacy, dates)
        val binding = adapter.forShop("internal-id", "public-address")
        val snapshot = binding.open(initial("public-address")).getOrThrow()
        assertEquals("Existing draft", snapshot.input.text)
        assertEquals("public-address", snapshot.input.shopSlug)
        binding.save(snapshot.copy(deliveryUnconfirmed = true)).getOrThrow()
        assertEquals("internal-id", legacy.peek()!!.shopId)
        val reopened = adapter.forShop("internal-id", "public-address")
        assertTrue(reopened.open(initial("public-address")).getOrThrow().deliveryUnconfirmed)
        reopened.clear("public-address").getOrThrow()
        assertEquals(null, legacy.peek())
    }

    @Test fun oldBindingCannotOverwriteReplacementEvenAfterItWasOpenedByNewBinding() {
        val legacy = CheckInDraftStore { now }
        val adapter = AppShopCheckInDraftStore(legacy, dates)
        val old = adapter.forShop("internal-id", "public-address")
        val stale = old.open(initial("public-address")).getOrThrow()
        legacy.clearAll()
        val fresh = adapter.forShop("internal-id", "public-address")
        val replacement = fresh.open(initial("public-address")).getOrThrow()
        fresh.save(replacement.copy(input = replacement.input.copy(text = "New session"))).getOrThrow()
        assertTrue(old.save(stale).isFailure)
        assertTrue(old.clear("public-address").isFailure)
        assertEquals("New session", legacy.peek()!!.note)
    }

    @Test fun bindingRejectsWrongSlugWithoutOpeningOrClearingDraft() {
        val legacy = CheckInDraftStore { now }
        val binding = AppShopCheckInDraftStore(legacy, dates).forShop("internal-id", "public-address")
        assertTrue(binding.open(initial("wrong-address")).isFailure)
        assertEquals(null, legacy.peek())
        binding.open(initial("public-address")).getOrThrow()
        assertTrue(binding.clear("wrong-address").isFailure)
        assertEquals("internal-id", legacy.peek()!!.shopId)
    }

    @Test fun originalPhotosSurviveModalReopenButNeverAccountReplacement() {
        val legacy = CheckInDraftStore { now }
        val adapter = AppShopCheckInDraftStore(legacy, dates)
        val binding = adapter.forShop("internal-id", "public-address", "owner")
        val first = binding.open(initial("public-address")).getOrThrow()
        val original = ShopCheckInPhoto(byteArrayOf(1, 2), "visit.jpg")
        binding.save(first.copy(input = first.input.copy(photos = listOf(original)))).getOrThrow()
        val reopened = adapter.forShop("internal-id", "public-address", "owner")
        val selected = reopened.open(initial("public-address")).getOrThrow()
        assertEquals(listOf(original), selected.input.photos)
        assertEquals(1, legacy.peek()!!.photos.size)
        val otherAccount = adapter.forShop("internal-id", "public-address", "other")
            .open(initial("public-address")).getOrThrow()
        assertTrue(otherAccount.input.photos.isEmpty())
        assertTrue(reopened.save(selected).isFailure)
    }

    @Test fun legacyClearInvalidatesOriginalPhotoDraft() {
        val legacy = CheckInDraftStore { now }
        val adapter = AppShopCheckInDraftStore(legacy, dates)
        val first = adapter.open(initial()).getOrThrow()
        adapter.save(first.copy(input = first.input.copy(photos = listOf(ShopCheckInPhoto(byteArrayOf(1), "visit.jpg"))))).getOrThrow()
        legacy.clearAll()
        val fresh = adapter.open(initial()).getOrThrow()
        assertTrue(fresh.input.photos.isEmpty())
    }
}
