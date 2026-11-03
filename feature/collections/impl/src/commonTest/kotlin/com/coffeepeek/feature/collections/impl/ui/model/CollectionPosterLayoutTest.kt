package com.coffeepeek.feature.collections.impl.ui.model

import com.coffeepeek.feature.collections.domain.model.*
import kotlin.test.*

class CollectionPosterLayoutTest {
    @Test fun groupingHappensBeforePaginationSoLaterShopsNeverAppearAfterRoasters() {
        val input = (1..11).map { place(it, if (it % 3 == 0) CollectionPlaceKind.Roaster else CollectionPlaceKind.CoffeeShop) }
        CollectionPosterFormat.entries.forEach { format ->
            val pages = (0..input.size / format.perPage).map { collectionPosterPage(input, format, it) }
            val output = pages.flatten()
            assertEquals(input.size, output.size)
            assertEquals(input.map { it.key }.toSet(), output.map { it.key }.toSet())
            assertEquals(input.filter { it.kind == CollectionPlaceKind.CoffeeShop }, output.takeWhile { it.kind == CollectionPlaceKind.CoffeeShop })
            assertEquals(input.filter { it.kind == CollectionPlaceKind.Roaster }, output.takeLastWhile { it.kind == CollectionPlaceKind.Roaster })
        }
    }

    @Test fun fullAndPartialPagesStayBelowTheHeadingAboveTheFooterAndDoNotOverlap() {
        CollectionPosterFormat.entries.forEach { format ->
            (1..205).forEach { count ->
                val places = (1..count).map { place(it, if (it % 2 == 0) CollectionPlaceKind.Roaster else CollectionPlaceKind.CoffeeShop) }
                (0..count / format.perPage).forEach { page ->
                    val cards = collectionPosterCards(collectionPosterPage(places, format, page), format)
                    assertTrue(cards.all { it.height > 0f && it.top >= 260f && it.top + it.height <= format.height - 80f + 0.01f })
                    assertTrue(cards.zipWithNext().all { (first, second) -> first.top + first.height < second.top })
                }
            }
        }
    }

    @Test fun roastersUseTheCompactCardAndAnOutOfRangePageHasNoDuplicates() {
        val input = listOf(place(1, CollectionPlaceKind.CoffeeShop), place(2, CollectionPlaceKind.Roaster))
        val cards = collectionPosterCards(input, CollectionPosterFormat.Chat)
        assertTrue(cards.first().height > cards.last().height)
        assertTrue(collectionPosterPage(input, CollectionPosterFormat.Chat, 9).isEmpty())
    }

    private fun place(id: Int, kind: CollectionPlaceKind) = CollectionPlace(kind, "place-$id", "Place $id")
}
