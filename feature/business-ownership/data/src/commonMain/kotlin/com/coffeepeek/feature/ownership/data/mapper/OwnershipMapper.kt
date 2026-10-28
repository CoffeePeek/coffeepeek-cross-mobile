package com.coffeepeek.feature.ownership.data.mapper

import com.coffeepeek.feature.ownership.data.backend.*
import com.coffeepeek.feature.ownership.domain.model.*

internal fun ClaimDto.domain() = OwnershipClaim(id, BusinessKind.valueOf(businessType), businessId, businessNameSnapshot,
    businessSlugSnapshot, contact, evidence, ClaimStatus.valueOf(status), createdAtUtc, decisionReason, reviewedAtUtc, hasCurrentAccess)
internal fun <T, R> OwnershipPageDto<T>.domain(map: (T) -> R) = BusinessPage(items.map(map), totalCount, page, pageSize)
internal fun ProfileDto.domain(kind: BusinessKind): BusinessProfile {
    val links = if (kind == BusinessKind.CoffeeShop) contacts else contact
    return BusinessProfile(id, kind, profileRevision, name, (if (kind == BusinessKind.CoffeeShop) description else about).orEmpty(),
        links?.phoneNumber.orEmpty(), links?.email.orEmpty(), links?.siteLink.orEmpty(), links?.instagramLink.orEmpty(),
        cityId ?: location?.cityId, location?.address.orEmpty(), location?.latitude, location?.longitude,
        photos.map { BusinessPhoto(it.id ?: it.storageKey, it.id, it.storageKey, it.fileName, it.mediaType(), it.fullUrl, it.sizeBytes) })
}

private fun PhotoDto.mediaType(): String = contentType?.takeIf { it.isNotBlank() } ?: when (fileName.substringAfterLast('.').lowercase()) {
    "png" -> "image/png"
    "webp" -> "image/webp"
    "gif" -> "image/gif"
    "avif" -> "image/avif"
    "heic" -> "image/heic"
    "heif" -> "image/heif"
    else -> "image/jpeg"
}
