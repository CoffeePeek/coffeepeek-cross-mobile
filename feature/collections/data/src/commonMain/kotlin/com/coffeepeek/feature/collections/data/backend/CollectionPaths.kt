package com.coffeepeek.feature.collections.data.backend

import com.coffeepeek.feature.collections.domain.model.CollectionPlaceKind

internal fun collectionId(id: String): String {
    require(Regex("[0-9a-fA-F]{32}|[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}").matches(id))
    return id.replace("-", "").lowercase()
}

/** System.Text.Json binds Guid body fields in the hyphenated D form. */
internal fun collectionBodyId(id: String): String {
    val compact = collectionId(id)
    return "${compact.substring(0, 8)}-${compact.substring(8, 12)}-${compact.substring(12, 16)}-" +
        "${compact.substring(16, 20)}-${compact.substring(20)}"
}

internal fun collectionPublicUrl(id: String, canonicalPath: String): String {
    require(canonicalPath == "/collections/${collectionId(id)}")
    return "https://coffeepeek.by$canonicalPath"
}

internal fun collectionWireKind(kind: CollectionPlaceKind): String = when (kind) {
    CollectionPlaceKind.CoffeeShop -> "coffee_shop"
    CollectionPlaceKind.Roaster -> "roaster"
}
