package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.impl.resources.Res
import com.coffeepeek.feature.discovery.impl.resources.discovery_brew_batch_brew
import com.coffeepeek.feature.discovery.impl.resources.discovery_brew_cezve
import com.coffeepeek.feature.discovery.impl.resources.discovery_brew_cold_brew
import com.coffeepeek.feature.discovery.impl.resources.discovery_brew_espresso
import com.coffeepeek.feature.discovery.impl.resources.discovery_brew_moka_pot
import com.coffeepeek.feature.discovery.impl.resources.discovery_tag_bakery
import com.coffeepeek.feature.discovery.impl.resources.discovery_tag_beans_for_sale
import com.coffeepeek.feature.discovery.impl.resources.discovery_tag_breakfasts
import com.coffeepeek.feature.discovery.impl.resources.discovery_tag_confectionery
import com.coffeepeek.feature.discovery.impl.resources.discovery_tag_decaf
import com.coffeepeek.feature.discovery.impl.resources.discovery_tag_gluten_free
import com.coffeepeek.feature.discovery.impl.resources.discovery_tag_laptop_friendly
import com.coffeepeek.feature.discovery.impl.resources.discovery_tag_outside_seating
import com.coffeepeek.feature.discovery.impl.resources.discovery_tag_pet_friendly
import com.coffeepeek.feature.discovery.impl.resources.discovery_tag_plant_milk
import com.coffeepeek.feature.discovery.impl.resources.discovery_tag_roastery
import com.coffeepeek.feature.discovery.impl.resources.discovery_tag_to_go
import com.coffeepeek.feature.discovery.impl.resources.discovery_tag_vegan
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import com.coffeepeek.feature.discovery.domain.model.CatalogEntry

@Composable
internal fun filterTagLabel(entry: CatalogEntry): String =
    tagLabelResource(entry.id)?.let { stringResource(it) } ?: entry.name

@Composable
internal fun filterBrewMethodLabel(entry: CatalogEntry): String =
    brewMethodLabelResource(entry.id)?.let { stringResource(it) } ?: entry.name

private fun tagLabelResource(id: String): StringResource? = when (id.catalogKey()) {
    "roastery" -> Res.string.discovery_tag_roastery
    "beans-for-sale" -> Res.string.discovery_tag_beans_for_sale
    "decaf" -> Res.string.discovery_tag_decaf
    "to-go" -> Res.string.discovery_tag_to_go
    "plan-based-milk", "plant-based-milk" -> Res.string.discovery_tag_plant_milk
    "laptop-friendly" -> Res.string.discovery_tag_laptop_friendly
    "outside-seating" -> Res.string.discovery_tag_outside_seating
    "breakfasts" -> Res.string.discovery_tag_breakfasts
    "bakery" -> Res.string.discovery_tag_bakery
    "confectionery" -> Res.string.discovery_tag_confectionery
    "pet-friendly" -> Res.string.discovery_tag_pet_friendly
    "vegan" -> Res.string.discovery_tag_vegan
    "gluten-free" -> Res.string.discovery_tag_gluten_free
    else -> null
}

private fun brewMethodLabelResource(id: String): StringResource? = when (id.catalogKey()) {
    "cold-brew" -> Res.string.discovery_brew_cold_brew
    "batch-brew" -> Res.string.discovery_brew_batch_brew
    "espresso" -> Res.string.discovery_brew_espresso
    "moka-pot" -> Res.string.discovery_brew_moka_pot
    "cezve-turkish-coffee" -> Res.string.discovery_brew_cezve
    else -> null
}

private fun String.catalogKey(): String = trim().lowercase().replace('_', '-')
