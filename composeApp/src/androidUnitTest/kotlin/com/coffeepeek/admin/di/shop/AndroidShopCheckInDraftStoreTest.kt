package com.coffeepeek.admin.di.shop

import com.coffeepeek.core.session.Session
import com.coffeepeek.feature.shop.data.local.createShopCheckInDraftStorage
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility
import com.coffeepeek.feature.shop.domain.model.ShopRating
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppShopCheckInDraftStoreTest {
    @get:Rule val temporary = TemporaryFolder()
    private val now = Instant.parse("2026-10-09T12:00:00Z").toEpochMilli()
    private val dates = AndroidShopCheckInDateFormatter({ ZoneId.of("Europe/Minsk") }, { now })
    private val sessions = CheckInTestSessions()

    private fun store() = AppShopCheckInDraftStore(createShopCheckInDraftStorage(temporary.root.path), sessions)
    private fun initial(shop: String = "public-address") =
        ShopCheckInCreateInput(shop, "", ShopRating(4, 4, 4), "2026-10-09T12:34:56.789Z")

    @Test fun restoresEveryFieldAndOriginalPhotosAfterRestart() = runTest {
        val initial = initial()
        val binding = store().forShop("internal-id", initial.shopSlug, "owner")
        val snapshot = binding.open(initial).getOrThrow()
        assertFalse(snapshot.exists)
        val input = initial.copy(
            text = "Хороший кофе",
            rating = ShopRating(5, 3, 4),
            visibility = ShopCheckInVisibility.Private,
            drinkSlug = "other",
            customDrinkName = "Фильтр",
            photos = listOf(ShopCheckInPhoto(byteArrayOf(1, 2), "visit.png", "image/png")),
        )
        binding.save(snapshot.copy(input = input)).getOrThrow()
        binding.save(snapshot.copy(input = input.copy(text = "Готово"), deliveryUnconfirmed = true)).getOrThrow()

        val restarted = store()
        assertEquals(initial.shopSlug, restarted.observeLatestShop("owner").first())
        val renamed = initial.copy(shopSlug = "new-public-address")
        val restored = restarted.forShop("internal-id", renamed.shopSlug, "owner").open(renamed).getOrThrow()
        assertEquals(input.copy(text = "Готово", shopSlug = renamed.shopSlug), restored.input)
        assertTrue(restored.exists)
        assertTrue(restored.deliveryUnconfirmed)
    }

    @Test fun closedModalDraftStaysWithOriginalAccount() = runTest {
        val adapter = store()
        val binding = adapter.forShop("internal-id", "public-address", "owner")
        val snapshot = binding.open(initial()).getOrThrow()
        binding.save(snapshot.copy(input = snapshot.input.copy(text = "Private note"), deliveryUnconfirmed = true)).getOrThrow()
        sessions.applySession(null)
        assertTrue(binding.save(snapshot).isFailure)
        sessions.applySession(Session("different-token", userId = "other"))
        val fresh = adapter.forShop("internal-id", "public-address", "other").open(initial()).getOrThrow()
        assertFalse(fresh.exists)
        assertNull(adapter.observeLatestShop("other").first())
        assertTrue(binding.clear("public-address").isFailure)
        sessions.applySession(Session("new-token", userId = "owner"))
        val restored = adapter.forShop("internal-id", "public-address", "owner").open(initial()).getOrThrow()
        assertEquals("Private note", restored.input.text)
        assertTrue(restored.deliveryUnconfirmed)
    }

    @Test fun clearingOneCafeLeavesOtherDraftsAndShowsTheNextOne() = runTest {
        val adapter = store()
        val first = adapter.forShop("internal-id", "public-address", "owner")
        val initial = initial()
        val original = first.open(initial).getOrThrow()
        first.save(original.copy(input = initial.copy(text = "First"))).getOrThrow()
        val other = adapter.forShop("second-id", "other-address", "owner")
        val second = other.open(initial("other-address")).getOrThrow()
        assertFalse(second.exists)
        other.save(second.copy(input = second.input.copy(text = "Second"))).getOrThrow()

        other.clear("other-address").getOrThrow()
        assertFalse(other.open(second.input).getOrThrow().exists)
        assertEquals(initial.shopSlug, adapter.observeLatestShop("owner").first())
        assertEquals("First", first.open(initial).getOrThrow().input.text)
        first.clear(initial.shopSlug).getOrThrow()
        assertNull(adapter.observeLatestShop("owner").first())
    }

    @Test fun staleBindingAndWrongSlugCannotChangeCurrentDraft() = runTest {
        val adapter = store()
        val old = adapter.forShop("internal-id", "public-address", "owner")
        val stale = old.open(initial()).getOrThrow()
        val fresh = adapter.forShop("internal-id", "public-address", "owner")
        assertTrue(fresh.open(initial("wrong-address")).isFailure)
        val replacement = fresh.open(initial()).getOrThrow()
        fresh.save(replacement.copy(input = replacement.input.copy(text = "New form"))).getOrThrow()
        assertTrue(old.save(stale).isFailure)
        assertTrue(old.clear("public-address").isFailure)
        assertTrue(fresh.clear("wrong-address").isFailure)
        assertEquals("New form", fresh.open(initial()).getOrThrow().input.text)
    }

    @Test fun selectedDateRoundTripsInBothPositiveAndNegativeTimezones() {
        val selected = Instant.parse("2026-06-15T00:00:00Z").toEpochMilli()
        for (zone in listOf("Europe/Minsk", "America/New_York")) {
            val formatter = AndroidShopCheckInDateFormatter({ ZoneId.of(zone) }, { now })
            assertEquals(selected, formatter.pickerMillis(formatter.visitInstant(selected)))
        }
        assertEquals("2026-06-14T21:00:00Z", dates.visitInstant(selected))
        assertNull(dates.pickerMillis("invalid"))
    }

    @Test fun dateChangeKeepsLocalVisitTime() {
        val reference = "2026-10-09T12:34:56.789Z"
        val selected = Instant.parse("2026-06-15T00:00:00Z").toEpochMilli()
        assertEquals("2026-06-15T12:34:56.789Z", dates.visitInstant(selected, reference))
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

    @Test fun repeatedDstHourKeepsTheExactOriginalOffset() {
        val reference = "2026-10-25T01:30:00Z"
        val formatter = AndroidShopCheckInDateFormatter({ ZoneId.of("Europe/Berlin") },
            { Instant.parse("2026-10-26T00:00:00Z").toEpochMilli() })
        val day = Instant.parse("2026-10-25T00:00:00Z").toEpochMilli()
        assertEquals(reference, formatter.visitInstant(day, reference))
    }
}
