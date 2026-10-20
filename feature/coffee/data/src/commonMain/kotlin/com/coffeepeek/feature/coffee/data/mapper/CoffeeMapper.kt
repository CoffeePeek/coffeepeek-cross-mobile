package com.coffeepeek.feature.coffee.data.mapper

import com.coffeepeek.feature.coffee.data.backend.CoffeeDto
import com.coffeepeek.feature.coffee.data.backend.CoffeeFilterGroupDto
import com.coffeepeek.feature.coffee.data.backend.CoffeeOfferDto
import com.coffeepeek.feature.coffee.data.backend.CoffeePhotoDto
import com.coffeepeek.feature.coffee.data.backend.coffeeFilterCodes
import com.coffeepeek.feature.coffee.domain.model.Coffee
import com.coffeepeek.feature.coffee.domain.model.CoffeeAvailability
import com.coffeepeek.feature.coffee.domain.model.CoffeeDetails
import com.coffeepeek.feature.coffee.domain.model.CoffeeFilterGroup
import com.coffeepeek.feature.coffee.domain.model.CoffeeFilterOption
import com.coffeepeek.feature.coffee.domain.model.CoffeeFilters
import com.coffeepeek.feature.coffee.domain.model.CoffeeOffer
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal fun CoffeeDto.toDomain() = Coffee(
    slug = address.slug,
    name = name,
    photoUrl = (coverPhoto?.cardUrl ?: photos.firstOrNull()?.cardUrl)?.takeIf(String::isNotBlank),
    roasterName = roaster.name,
    roasterPhotoUrl = roaster.coverPhoto?.thumbnailUrl,
    countries = countries.map { it.nameRu ?: it.nameEn ?: it.code },
    tasteCodes = classification?.tasteGroups.orEmpty(),
    offers = matchingOffers.ifEmpty { offers }.map { it.toDomain() },
)

internal fun CoffeeDto.toDetails() = CoffeeDetails(
    coffee = toDomain(),
    canonicalPath = address.canonicalPath?.takeIf { it.startsWith("/coffees/") } ?: "/coffees/${address.slug}",
    description = description?.takeIf(String::isNotBlank),
    photos = photos.mapNotNull { it.detailUrl }.ifEmpty { listOfNotNull(coverPhoto?.detailUrl) },
    roasterSlug = roaster.address?.slug,
    productKind = productKind,
    productForm = productForm,
    roastLevel = classification?.roastLevel,
    acidity = classification?.acidity,
    tasteDescriptors = tasteDescriptors,
    checkedAtUtc = catalogCheckedAtUtc,
)

private val CoffeePhotoDto.cardUrl: String? get() = urls?.card ?: fullUrl
private val CoffeePhotoDto.thumbnailUrl: String? get() = urls?.thumbnail ?: fullUrl
private val CoffeePhotoDto.detailUrl: String? get() = urls?.detail ?: fullUrl

internal fun CoffeeOfferDto.toDomain() = CoffeeOffer(
    price = price?.takeIf { it.isFinite() && it >= 0 },
    currency = currency,
    weightGrams = weightGrams?.takeIf { it > 0 },
    key = offerKey,
    availability = CoffeeAvailability.entries.firstOrNull { it.name == availability } ?: CoffeeAvailability.Unknown,
    sellerName = sellerName,
    sourceUrl = sourceUrl?.takeIf { value ->
        runCatching { Url(value) }.getOrNull()?.let {
            it.protocol in setOf(URLProtocol.HTTP, URLProtocol.HTTPS) && it.host.isNotBlank()
        } == true
    },
    grind = grind,
    brewPurpose = brewPurpose,
    availabilityScope = availabilityScope,
    checkedAtUtc = checkedAtUtc,
)

internal fun CoffeeFilters.toJson(): JsonObject = buildJsonObject {
    values.forEach { (code, selected) ->
        require(code in coffeeFilterCodes)
        if (selected.isNotEmpty()) put(code, JsonArray(selected.sorted().map(::JsonPrimitive)))
    }
    put("availableOnly", availableOnly)
}

internal fun List<CoffeeFilterGroupDto>.toDomain(): List<CoffeeFilterGroup> =
    filter { it.code in coffeeFilterCodes }.map { group ->
        CoffeeFilterGroup(group.code, group.name, group.values.sortedBy { it.sortOrder }.map {
            CoffeeFilterOption(it.code, it.name)
        })
    }
