package com.coffeepeek.feature.collections.impl.ui.model

import com.coffeepeek.feature.collections.domain.model.*
import kotlin.test.*

class CollectionPosterLayoutTest {
    @Test fun mixedPagesKeepAllPlacesInStableKindOrder() {
        CollectionPosterFormat.entries.forEach { format ->
            (1..205).forEach { count -> verifyMixedPages(count, format) }
        }
    }

    @Test fun fourShopsAndFourRoastersFitOneStory() {
        val places = (1..8).map { place(it, if (it <= 4) CollectionPlaceKind.CoffeeShop else CollectionPlaceKind.Roaster) }
        val page = collectionPosterLayout(places, CollectionPosterFormat.Story).pages.single()
        assertEquals(8, page.size)
        assertEquals(page[4].top, page[5].top)
        assertEquals(page[6].top, page[7].top)
        verifyGeometry(page, CollectionPosterFormat.Story, 178f)
    }

    @Test fun measuredRoasterWidthsShareRowsAndWrapWithoutOverlap() {
        val places = (1..4).map { place(it, CollectionPlaceKind.Roaster) }
        val widths = places.zip(listOf(440f, 560f, 760f, 420f)).associate { it.first.key to it.second }
        val cards = collectionPosterLayout(places, CollectionPosterFormat.Chat) { widths.getValue(it.key) }.pages.single()
        assertEquals(440f, cards[0].width)
        assertEquals(560f, cards[1].width)
        assertEquals(cards[0].top, cards[1].top)
        assertTrue(cards[2].top > cards[1].top)
        assertTrue(cards[3].top > cards[2].top)
        verifyGeometry(cards, CollectionPosterFormat.Chat, 178f)
    }

    @Test fun longHeadingAndExtremeWidthsRemainInsideTheContentArea() {
        val places = (1..205).map { place(it, CollectionPlaceKind.Roaster) }
        CollectionPosterFormat.entries.forEach { format ->
            val pages = collectionPosterLayout(places, format, 228f) { if (it.slug.endsWith('0')) 2000f else 10f }.pages
            assertEquals(places, pages.flatten().map { it.place })
            pages.forEach { verifyGeometry(it, format, 228f) }
        }
    }

    @Test fun eightRoastersFitOneChatImageAndShopsRetainReadableRows() {
        val roasters = (1..8).map { place(it, CollectionPlaceKind.Roaster) }
        assertEquals(8, collectionPosterLayout(roasters, CollectionPosterFormat.Chat).pages.single().size)
        val shops = (1..7).map { place(it, CollectionPlaceKind.CoffeeShop) }
        val cards = collectionPosterLayout(shops, CollectionPosterFormat.Story).pages.single()
        assertEquals(7, cards.size)
        assertTrue(cards.all { it.width == 1016f && it.height == 216f })
    }

    @Test fun emptyInputProducesNoBlankExportPages() {
        assertTrue(collectionPosterLayout(emptyList(), CollectionPosterFormat.Chat).pages.isEmpty())
    }

    private fun verifyMixedPages(count: Int, format: CollectionPosterFormat) {
        val places = (1..count).map { place(it, if (it % 3 == 0) CollectionPlaceKind.Roaster else CollectionPlaceKind.CoffeeShop) }
        val pages = collectionPosterLayout(places, format).pages
        assertEquals(CollectionRules.orderedPlaces(places), pages.flatten().map { it.place })
        assertTrue(pages.all { it.isNotEmpty() })
        pages.forEach { verifyGeometry(it, format, 178f) }
    }

    private fun verifyGeometry(cards: List<CollectionPosterCard>, format: CollectionPosterFormat, contentTop: Float) {
        assertTrue(cards.all { it.left >= 32f && it.left + it.width <= 1048.01f })
        assertTrue(cards.all { it.top >= contentTop && it.top + it.height <= format.height - 108f })
        cards.forEachIndexed { index, first ->
            cards.drop(index + 1).forEach { second ->
                assertTrue(first.left + first.width <= second.left || second.left + second.width <= first.left ||
                    first.top + first.height <= second.top || second.top + second.height <= first.top)
            }
        }
    }

    private fun place(id: Int, kind: CollectionPlaceKind) = CollectionPlace(kind, "place-$id", "Place $id")
}
