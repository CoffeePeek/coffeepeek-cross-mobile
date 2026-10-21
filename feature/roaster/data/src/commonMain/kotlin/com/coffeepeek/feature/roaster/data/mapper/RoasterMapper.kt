package com.coffeepeek.feature.roaster.data.mapper

import com.coffeepeek.feature.roaster.data.backend.RoasterAddressDto
import com.coffeepeek.feature.roaster.data.backend.RoasterDetailsDto
import com.coffeepeek.feature.roaster.data.backend.RoasterShortPhotoDto
import com.coffeepeek.feature.roaster.data.backend.RoasterSummaryDto
import com.coffeepeek.feature.roaster.data.backend.RoasterTagDto
import com.coffeepeek.feature.roaster.domain.model.RoasterAddress
import com.coffeepeek.feature.roaster.domain.model.RoasterContact
import com.coffeepeek.feature.roaster.domain.model.RoasterDetails
import com.coffeepeek.feature.roaster.domain.model.RoasterLocation
import com.coffeepeek.feature.roaster.domain.model.RoasterPhoto
import com.coffeepeek.feature.roaster.domain.model.RoasterShop
import com.coffeepeek.feature.roaster.domain.model.RoasterSummary
import com.coffeepeek.feature.roaster.domain.model.RoasterTag

/** Resolves photo storage keys against the configured file host, exactly like the legacy catalogue. */
internal class RoasterFileUrlResolver(private val baseUrl: String) {
    fun resolve(storageKey: String?, fullUrl: String? = null): String? {
        fullUrl?.trim()?.takeIf(String::isNotBlank)?.let { return it }
        val key = storageKey?.trim().orEmpty()
        if (key.isBlank()) return null
        if (key.startsWith("http://") || key.startsWith("https://")) return key
        return "${baseUrl.trimEnd('/')}/api/file/$key"
    }
}

internal fun RoasterAddressDto.toAddressOrNull(): RoasterAddress? =
    slug?.takeIf(String::isNotBlank)?.let { RoasterAddress(it, canonicalPath.orEmpty(), revision, isAlias) }

internal fun RoasterAddressDto.toAddress(): RoasterAddress =
    RoasterAddress(slug.orEmpty(), canonicalPath.orEmpty(), revision, isAlias)

internal fun RoasterSummaryDto.toDomain(files: RoasterFileUrlResolver): RoasterSummary {
    val address = address?.toAddressOrNull()
    return RoasterSummary(
        address = address,
        name = name?.takeIf(String::isNotBlank) ?: address?.slug.orEmpty(),
        photoUrl = coverPhoto?.urls?.card?.takeIf(String::isNotBlank)
            ?: photoUrl?.takeIf(String::isNotBlank) ?: coverPhoto?.fullUrl?.takeIf(String::isNotBlank)
            ?: files.resolve(coverPhoto?.storageKey),
        tags = tags.orEmpty().mapNotNull { it.toDomain() }.sortedBy { it.sortOrder },
        coffeeShopsCount = coffeeShopsCount,
        coffeeProductsCount = coffeeProductsCount,
        availableCoffeeProducts = availableCoffeeProducts,
    )
}

private fun RoasterTagDto.toDomain(): RoasterTag? {
    val slug = slug?.takeIf(String::isNotBlank) ?: return null
    return RoasterTag(slug, name?.takeIf(String::isNotBlank) ?: slug, description, sortOrder)
}

internal fun RoasterDetailsDto.toDomain(files: RoasterFileUrlResolver): RoasterDetails = RoasterDetails(
    id = address.slug.orEmpty(),
    address = address.toAddress(),
    name = name,
    about = about,
    location = location?.takeIf { it.address.isNotBlank() }?.let { RoasterLocation(it.address, it.latitude, it.longitude) },
    contact = contact?.let {
        RoasterContact(it.instagramLink?.takeIf(String::isNotBlank), it.siteLink?.takeIf(String::isNotBlank))
    },
    photos = photos.sortedBy { it.sortIndex }.mapNotNull { photo ->
        files.resolve(photo.storageKey, photo.fullUrl)?.let { RoasterPhoto(photo.id, it, photo.sortIndex) }
    },
    // Legacy behaviour: shop thumbnails use the cover photo's full URL; the app enriches missing ones separately.
    shops = shops.map { RoasterShop(it.address.slug.orEmpty(), it.name, it.coverPhoto.fullUrlOrNull(), it.address.toAddress()) },
)

private fun RoasterShortPhotoDto?.fullUrlOrNull(): String? = this?.fullUrl
