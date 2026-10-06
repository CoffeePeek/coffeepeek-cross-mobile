package com.coffeepeek.data.mapper

import com.coffeepeek.api.model.PublicAddressDto
import com.coffeepeek.domain.model.PublicAddress

internal fun PublicAddressDto.toDomain() = PublicAddress(slug, canonicalPath, revision, isAlias)
