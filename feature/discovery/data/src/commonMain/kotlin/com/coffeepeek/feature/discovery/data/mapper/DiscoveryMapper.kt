package com.coffeepeek.feature.discovery.data.mapper

import com.coffeepeek.feature.discovery.data.backend.CityDto
import com.coffeepeek.feature.discovery.data.backend.NamedDto
import com.coffeepeek.feature.discovery.data.backend.ShopSearchRowDto
import com.coffeepeek.feature.discovery.data.backend.PhotoDto
import com.coffeepeek.feature.discovery.domain.model.CatalogEntry
import com.coffeepeek.feature.discovery.domain.model.CityEntry
import com.coffeepeek.feature.discovery.domain.model.ShopKind
import com.coffeepeek.feature.discovery.domain.model.ShopSummary

private const val TAG_LIMIT = 3

/**
 * A row of the shop search. The server omits the kind and the new flag, so the row carries what was asked for, as the legacy search did: [requestedKind], and
 * the requested visited and new switches that the server applied.
 */
internal fun ShopSearchRowDto.toDomain(favorites: Set<String>, requestedKind: ShopKind?, visitedAsked: Boolean, newAsked: Boolean) = ShopSummary(
    id = address.slug,
    title = name,
    rating = rating?.takeIf { it > 0 },
    checkInCount = checkInCount,
    address = addressLine,
    photoUrl = coverPhoto?.card(),
    isOpen = isOpen,
    isNew = newAsked,
    isVisited = isVisited == true || visitedAsked,
    isFavorite = address.slug in favorites || isFavorite == true,
    tags = tags.names().take(TAG_LIMIT),
    kind = requestedKind ?: ShopKind.CoffeeBar,
    distanceMeters = distanceMeters,
)

/** What prod shows for a map shop without a title. */
internal const val UNNAMED_SHOP = "Кофейня"

internal fun CityDto.toDomain() = CityEntry(address.slug, name)

internal fun NamedDto.toCatalogEntry() = CatalogEntry(key, name.orEmpty())

private fun PhotoDto.card(): String? = urls?.card?.takeIf(String::isNotBlank) ?: fullUrl?.takeIf(String::isNotBlank)

private fun List<NamedDto>.names() = mapNotNull { it.name?.takeIf(String::isNotBlank) }
