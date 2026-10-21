package com.coffeepeek.feature.shop.data.mapper

import com.coffeepeek.feature.shop.data.backend.ShopDetailsDto
import com.coffeepeek.feature.shop.domain.model.ShopFeature
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal fun ShopDetailsDto.toFeatures(): List<ShopFeature> {
    val methods = brewMethods.mapNotNull { item ->
        val name = item.name?.trim()?.takeIf(String::isNotEmpty) ?: return@mapNotNull null
        ShopFeature(name, item.slug.ifBlank { null }, isBrewMethod = true)
    }
    val labels = shopTags.toTagFeatures().ifEmpty { tags.toTagFeatures() }
    return (methods + labels).distinctBy { it.name.trim().lowercase() }
}

private fun JsonElement?.toTagFeatures(): List<ShopFeature> = (this as? JsonArray)?.mapNotNull { item ->
    val name = when (item) {
        is JsonPrimitive -> item.contentOrNull
        is JsonObject -> listOf("name", "title", "label", "value")
            .firstNotNullOfOrNull { key -> (item[key] as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) }
        else -> null
    }?.trim()?.takeIf(String::isNotEmpty) ?: return@mapNotNull null
    val slug = (item as? JsonObject)?.get("slug")
        ?.let { it as? JsonPrimitive }?.contentOrNull?.trim()?.takeIf(String::isNotEmpty)
    ShopFeature(name, slug, isBrewMethod = false)
}.orEmpty()
