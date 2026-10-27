package com.coffeepeek.feature.shelf.data.mapper

import com.coffeepeek.feature.shelf.data.backend.ShelfBagDto
import com.coffeepeek.feature.shelf.data.backend.ShelfBagWriteDto
import com.coffeepeek.feature.shelf.data.backend.UploadedCardDto
import com.coffeepeek.feature.shelf.domain.model.*

/** Unknown status or grind values, or a shelf bag without a position, make the whole response invalid. */
internal fun ShelfBagDto.toDomain(apiBaseUrl: String): ShelfBag {
    val status = ShelfBagStatus.entries.firstOrNull { it.name == status } ?: invalid()
    if ((status == ShelfBagStatus.OnShelf) != (position != null) || id.isBlank()) invalid()
    return ShelfBag(
        id = id, status = status, position = position,
        photo = photo?.let { ShelfBagPhoto(it.id, absolute(apiBaseUrl, it.url)) },
        coffeeProductId = coffeeProductId, coffeeProductSlug = coffeeProductSlug, name = name, roasterName = roasterName,
        weightGrams = weightGrams, grind = grind?.let { value -> ShelfGrind.entries.firstOrNull { it.name == value } ?: invalid() },
        roastedOn = roastedOn, addedAtUtc = addedAtUtc, finishedAtUtc = finishedAtUtc, rating = rating, buyAgain = buyAgain,
    )
}

internal fun ShelfBagDetails.toWrite(photo: UploadedCardDto?) = ShelfBagWriteDto(
    photo = photo,
    coffeeProductSlug = (bean as? ShelfBean.Catalog)?.slug,
    coffeeProductId = (bean as? ShelfBean.Kept)?.coffeeProductId,
    customName = (bean as? ShelfBean.Custom)?.name?.trim(),
    roasterName = roasterName?.trim()?.ifEmpty { null },
    weightGrams = weightGrams, grind = grind?.name, roastedOn = roastedOn,
)

/** Protected card routes are relative to the gateway; never build storage URLs on the client. */
private fun absolute(apiBaseUrl: String, url: String): String = when {
    url.startsWith("https://") || url.startsWith("http://") -> url
    url.startsWith("/") -> apiBaseUrl.trimEnd('/') + url
    else -> invalid()
}

private fun invalid(): Nothing = throw ShelfFailure(ShelfIssue.InvalidResponse)
