package com.coffeepeek.feature.ownership.data.backend

import com.coffeepeek.feature.ownership.domain.model.BusinessPhoto
import com.coffeepeek.feature.ownership.domain.model.BusinessProfile

internal fun BusinessPhoto.metadata() = PhotoMetadataDto(fileName, contentType, checkNotNull(storageKey), size)
internal fun BusinessProfile.shopBody() = ShopWriteDto(name, about.emptyAsNull(), phone.emptyAsNull(), email.emptyAsNull(),
    site.emptyAsNull(), instagram.emptyAsNull(), LocationDto(cityId, address.emptyAsNull(), latitude, longitude))
internal fun BusinessProfile.roasterBody(photos: List<BusinessPhoto>?) = RoasterWriteDto(name, about.emptyAsNull(), cityId,
    address.emptyAsNull(), latitude, longitude, instagram.emptyAsNull(), site.emptyAsNull(), photos?.map { it.metadata() })
private fun String.emptyAsNull() = takeIf { it.isNotEmpty() }
