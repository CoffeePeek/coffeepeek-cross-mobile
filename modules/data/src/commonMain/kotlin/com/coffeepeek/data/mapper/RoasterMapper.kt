package com.coffeepeek.data.mapper

import com.coffeepeek.api.model.response.shop.RoasterSummaryDto
import com.coffeepeek.api.model.response.shop.RoasterTagDto
import com.coffeepeek.api.model.response.shop.variantOr
import com.coffeepeek.data.util.FileUrlResolver
import com.coffeepeek.domain.model.RoasterSummary
import com.coffeepeek.domain.model.CatalogItem

internal fun RoasterSummaryDto.toDomain(fileUrlResolver: FileUrlResolver): RoasterSummary {
    val publicAddress = address?.toDomain()
    return RoasterSummary(
        publicAddress = publicAddress,
        name = name?.takeIf(String::isNotBlank) ?: publicAddress?.slug.orEmpty(),
        photoUrl = coverPhoto?.urls.variantOr(photoUrl?.takeIf(String::isNotBlank) ?: coverPhoto?.fullUrl) { it.card }
            ?: fileUrlResolver.resolve(coverPhoto?.storageKey),
        tags = tags.orEmpty().mapNotNull { it.toDomain() }.sortedBy { it.sortOrder },
        coffeeShopsCount = coffeeShopsCount,
        coffeeProductsCount = coffeeProductsCount,
        availableCoffeeProducts = availableCoffeeProducts,
    )
}

internal fun RoasterTagDto.toDomain(): CatalogItem? {
    val slug = slug?.takeIf(String::isNotBlank) ?: return null
    return CatalogItem(slug, name?.takeIf(String::isNotBlank) ?: slug, slug = slug, description = description, sortOrder = sortOrder)
}
