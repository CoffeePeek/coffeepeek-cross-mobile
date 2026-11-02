package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.coffeepeek.feature.discovery.domain.model.CatalogEntry
import com.coffeepeek.feature.discovery.impl.R

@Composable
internal fun filterTagLabel(entry: CatalogEntry): String =
    tagLabelResource(entry.id)?.let { stringResource(it) } ?: entry.name

@Composable
internal fun filterBrewMethodLabel(entry: CatalogEntry): String =
    brewMethodLabelResource(entry.id)?.let { stringResource(it) } ?: entry.name

private fun tagLabelResource(id: String): Int? = when (id.catalogKey()) {
    "roastery" -> R.string.discovery_tag_roastery
    "beans-for-sale" -> R.string.discovery_tag_beans_for_sale
    "decaf" -> R.string.discovery_tag_decaf
    "to-go" -> R.string.discovery_tag_to_go
    "plan-based-milk", "plant-based-milk" -> R.string.discovery_tag_plant_milk
    "laptop-friendly" -> R.string.discovery_tag_laptop_friendly
    "outside-seating" -> R.string.discovery_tag_outside_seating
    "breakfasts" -> R.string.discovery_tag_breakfasts
    "bakery" -> R.string.discovery_tag_bakery
    "confectionery" -> R.string.discovery_tag_confectionery
    "pet-friendly" -> R.string.discovery_tag_pet_friendly
    "vegan" -> R.string.discovery_tag_vegan
    "gluten-free" -> R.string.discovery_tag_gluten_free
    else -> null
}

private fun brewMethodLabelResource(id: String): Int? = when (id.catalogKey()) {
    "cold-brew" -> R.string.discovery_brew_cold_brew
    "batch-brew" -> R.string.discovery_brew_batch_brew
    "espresso" -> R.string.discovery_brew_espresso
    "moka-pot" -> R.string.discovery_brew_moka_pot
    "cezve-turkish-coffee" -> R.string.discovery_brew_cezve
    else -> null
}

private fun String.catalogKey(): String = trim().lowercase().replace('_', '-')
