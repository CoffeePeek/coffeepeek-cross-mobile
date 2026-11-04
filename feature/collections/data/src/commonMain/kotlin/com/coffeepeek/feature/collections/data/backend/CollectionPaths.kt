package com.coffeepeek.feature.collections.data.backend

import com.coffeepeek.feature.collections.domain.model.CollectionPlaceKind

internal fun collectionId(id: String): String {
    require(Regex("[0-9a-fA-F]{32}|[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}").matches(id))
    return id.replace("-", "").lowercase()
}

internal fun collectionPublicUrl(id: String, canonicalPath: String): String {
    require(canonicalPath == "/collections/${collectionId(id)}")
    return "https://coffeepeek.by$canonicalPath"
}

internal fun collectionWireKind(kind: CollectionPlaceKind): String = when (kind) {
    CollectionPlaceKind.CoffeeShop -> "coffee_shop"
    CollectionPlaceKind.Roaster -> "roaster"
}
