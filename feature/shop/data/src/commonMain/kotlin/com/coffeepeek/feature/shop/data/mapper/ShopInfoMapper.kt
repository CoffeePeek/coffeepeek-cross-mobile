package com.coffeepeek.feature.shop.data.mapper

import com.coffeepeek.feature.shop.data.backend.ShopContactDto
import com.coffeepeek.feature.shop.data.backend.ShopDetailsDto
import com.coffeepeek.feature.shop.domain.model.ShopCoffeeDetails
import com.coffeepeek.feature.shop.domain.model.ShopContact
import com.coffeepeek.feature.shop.domain.model.ShopRoaster

internal fun ShopDetailsDto.toCoffeeDetails(): ShopCoffeeDetails = ShopCoffeeDetails(
    beans = coffeeBeans.mapNotNull { it.name?.trim()?.takeIf(String::isNotEmpty) },
    roasters = roasters.mapNotNull { item ->
        val name = item.name?.trim()?.takeIf(String::isNotEmpty) ?: return@mapNotNull null
        val slug = item.address?.slug?.takeIf(String::isNotBlank) ?: item.slug
        ShopRoaster(slug, name, item.photoUrl?.trim()?.takeIf(String::isNotEmpty),
            item.address?.canonicalPath)
    },
    equipment = equipments.mapNotNull { it.name?.trim()?.takeIf(String::isNotEmpty) },
)

internal fun ShopContactDto.toDomainOrNull(): ShopContact? {
    val mapped = ShopContact(
        phone = phone?.trim()?.takeIf(String::isNotEmpty),
        email = email?.trim()?.takeIf(String::isNotEmpty),
        website = website?.trim()?.takeIf(String::isNotEmpty),
        instagram = instagram?.trim()?.takeIf(String::isNotEmpty),
    )
    return mapped.takeIf { it.phone != null || it.email != null || it.website != null || it.instagram != null }
}
