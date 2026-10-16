package com.coffeepeek.admin.feature.catalog.api

/** Public brewing methods shared by shop and home preparation forms. */
internal data class BrewMethodOption(val slug: String, val name: String)

internal fun interface BrewMethodCatalog {
    suspend fun load(): Result<List<BrewMethodOption>>
}

/** Shared fallback labels for saved recipes when the catalog has not been loaded. */
internal fun String.methodLabel(): String = when (this) {
    "espresso" -> "Эспрессо"
    "pour_over" -> "Пуровер"
    "batch_drip" -> "Фильтр batch"
    "moka_pot" -> "Гейзерная кофеварка"
    "cezve" -> "Турка"
    "aeropress" -> "Аэропресс"
    "french_press" -> "Френч-пресс"
    "other" -> "Другое"
    "hario-v60" -> "Hario V60"
    "chemex" -> "Chemex"
    "cold-brew" -> "Cold Brew"
    "batch-brew" -> "Batch Brew"
    "moka-pot" -> "Moka Pot"
    "cezve-turkish-coffee" -> "Cezve (Turkish Coffee)"
    else -> this
}

/** Old home entries keep their stored code; the selector displays the current catalog name. */
internal fun String.catalogBrewMethodSlug(): String = when (this) {
    "pour_over" -> "hario-v60"
    "batch_drip" -> "batch-brew"
    "moka_pot" -> "moka-pot"
    "cezve" -> "cezve-turkish-coffee"
    else -> this
}
