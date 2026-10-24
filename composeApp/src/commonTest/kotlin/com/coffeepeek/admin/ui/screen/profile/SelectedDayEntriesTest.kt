package com.coffeepeek.admin.ui.screen.profile

import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.legacy.homecheckin.domain.HomeCheckIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SelectedDayEntriesTest {
    private fun shop(id: String, visitedAt: String, createdAt: String = "2026-10-23T23:00:00Z") =
        CheckIn(id = id, shopId = "shop-$id", shopName = "Shop $id", note = "", createdAt = createdAt, visitedAt = visitedAt)

    private fun home(id: String, preparedAt: String) = HomeCheckIn(
        id = id, preparedAtUtc = preparedAt, preparedOn = "2026-10-23", createdAtUtc = preparedAt, text = null, brewMethodCode = null,
        coffeeProductId = null, beanName = null, doseGrams = null, brewDurationSeconds = null, tasteScore = null, drinkName = null,
        drinkSlug = null, customDrinkName = null, photos = emptyList(),
    )

    @Test fun shopVisitsComeFirstByTimeAndHomeBrewsFollowByTime() {
        val entries = selectedDayEntries(
            listOf(shop("late", "2026-10-23T15:00:00Z"), shop("early", "2026-10-23T08:00:00Z")),
            listOf(home("h2", "2026-10-23T12:00:00Z"), home("h1", "2026-10-23T07:00:00Z")),
        )
        assertEquals(listOf("early", "late", "h1", "h2"), entries.map { it.first.id })
        assertEquals(listOf(false, false, true, true), entries.map { it.second })
    }

    @Test fun aVisitWithoutATimeIsOrderedByItsCreationTime() {
        val entries = selectedDayEntries(listOf(shop("b", "", "2026-10-23T18:00:00Z"), shop("a", "", "2026-10-23T09:00:00Z")), emptyList())
        assertEquals(listOf("a", "b"), entries.map { it.first.id })
    }

    @Test fun aDayWithoutCheckInsHasNoEntriesAndAHomeBrewIsMarkedAsHome() {
        assertTrue(selectedDayEntries(emptyList(), emptyList()).isEmpty())
        val only = selectedDayEntries(emptyList(), listOf(home("h", "2026-10-23T10:00:00Z"))).single()
        assertTrue(only.second); assertTrue(only.first.isHome); assertEquals("", only.first.shopId)
    }
}
