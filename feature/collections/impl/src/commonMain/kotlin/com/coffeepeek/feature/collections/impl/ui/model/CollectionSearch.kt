package com.coffeepeek.feature.collections.impl.ui.model

internal fun matchesCollectionQuery(title: String, query: String): Boolean =
    title.contains(query, ignoreCase = true)
