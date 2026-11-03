package com.coffeepeek.feature.collections.domain.model

import kotlin.test.*

class CollectionRulesTest {
    @Test fun shopsPrecedeAllRoastersWithoutChangingOrderWithinEitherKindOrTheInput() {
        val roaster = CollectionPlace(CollectionPlaceKind.Roaster, "roaster", "Roaster")
        val shop = CollectionPlace(CollectionPlaceKind.CoffeeShop, "shop", "Shop")
        val input = listOf(roaster, shop, roaster.copy(slug = "second-roaster"), shop.copy(slug = "second-shop"))
        assertEquals(listOf("shop", "second-shop", "roaster", "second-roaster"), CollectionRules.orderedPlaces(input).map { it.slug })
        assertEquals("roaster", input.first().slug)
        assertEquals(input.size, CollectionRules.orderedPlaces(input).size)
    }

    @Test fun requiresReadableTitleWithinBounds() {
        assertFalse(CollectionRules.validTitle("  "))
        assertFalse(CollectionRules.validTitle("Name\nOther"))
        assertTrue(CollectionRules.validTitle("Мой кофейный Минск"))
        assertTrue(CollectionRules.validTitle("a".repeat(100)))
        assertFalse(CollectionRules.validTitle("a".repeat(101)))
    }
    @Test fun kindIsPartOfIdentityAndDuplicatesAreRejected() {
        val shop = CollectionPlace(CollectionPlaceKind.CoffeeShop, "same", "Coffee shop")
        assertTrue(CollectionRules.validSelection(listOf(shop, shop.copy(kind = CollectionPlaceKind.Roaster))))
        assertFalse(CollectionRules.validSelection(listOf(shop, shop)))
        assertFalse(CollectionRules.validSelection(emptyList()))
        assertFalse(CollectionRules.validSelection(listOf(shop.copy(slug = ""))))
    }
    @Test fun aCollectionCanContainMoreThanAHundredDistinctSavedPlaces() {
        val places = (1..205).map { CollectionPlace(CollectionPlaceKind.Roaster, "roaster-$it", "Roaster $it") }
        assertTrue(CollectionRules.validSelection(places))
        assertFalse(CollectionRules.validSelection(places + places.first()))
    }
}
