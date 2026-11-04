package com.coffeepeek.feature.collections.data.mapper

import com.coffeepeek.feature.collections.data.backend.CollectionPlaceDto
import com.coffeepeek.feature.collections.domain.model.*
import io.ktor.http.*

internal fun collectionPlace(dto: CollectionPlaceDto, apiBaseUrl: String): CollectionPlace? {
    val kind = when (dto.kind) { "coffee_shop" -> CollectionPlaceKind.CoffeeShop; "roaster" -> CollectionPlaceKind.Roaster; else -> return null }
    val address = dto.address ?: return null
    if (address.slug.isBlank() || address.slug.any { it == '/' || it == '\\' || it.isISOControl() } || address.slug in listOf(".", "..")) return null
    val photo = dto.coverPhoto?.let { it.urls?.card ?: it.fullUrl }?.let { collectionPhotoUrl(it, apiBaseUrl) }
    return CollectionPlace(kind, address.slug, dto.name.ifBlank { address.slug }, photo, dto.cityName, dto.streetAddress,
        dto.rating?.takeIf { it.isFinite() && it > 0.0 && it <= 5.0 }, dto.checkInCount?.takeIf { it >= 0 })
}

private fun collectionPhotoUrl(raw: String, apiBaseUrl: String): String? {
    val value = if (raw.startsWith("/") && !raw.startsWith("//")) apiBaseUrl.trimEnd('/') + raw else raw
    val url = runCatching { Url(value) }.getOrNull() ?: return null
    return value.takeIf { url.protocol == URLProtocol.HTTPS && url.user == null && url.password == null }
}
