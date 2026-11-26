package com.coffeepeek.feature.homecheckin.impl.ui.data

internal fun List<HomeCheckInShelfBag>.matching(query: String): List<HomeCheckInShelfBag> {
    val text = query.trim().takeIf(String::isNotEmpty) ?: return this
    return filter { bag -> listOfNotNull(bag.name, bag.roasterName).any { it.contains(text, ignoreCase = true) } }
}
