package com.coffeepeek.feature.homecheckin.impl.ui.data

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeCheckInShelfBagSearchTest {
    private val bags = listOf(
        HomeCheckInShelfBag("one", "Эфиопия", "Spiritus"),
        HomeCheckInShelfBag("two", null, "Река"),
        HomeCheckInShelfBag("three", null, null),
    )

    @Test fun matchingUsesNamesAndRoastersWithAndroidQuerySemantics() {
        assertEquals(listOf("one"), bags.matching("  ЭФИОП ").map { it.id })
        assertEquals(listOf("one"), bags.matching("spirit").map { it.id })
        assertEquals(listOf("two"), bags.matching("РЕК").map { it.id })
        assertEquals(emptyList(), bags.matching("missing"))
    }

    @Test fun emptyQueryRetainsOrderAndBagsWithoutLabels() {
        assertEquals(bags, bags.matching("  "))
        assertEquals(3, bags.size)
    }
}
