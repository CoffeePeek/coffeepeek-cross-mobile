package com.coffeepeek.admin.di.coffee

import com.coffeepeek.admin.utils.utcIsoToLocalDateTime
import com.coffeepeek.feature.coffee.domain.model.Coffee
import com.coffeepeek.feature.coffee.domain.model.CoffeeDetails
import com.coffeepeek.feature.coffee.domain.model.CoffeeOffer
import com.coffeepeek.feature.coffee.impl.CoffeeDetailSnapshot

data class IosCoffeeDetailState(
    val product: IosCoffeeProduct?,
    val isLoading: Boolean,
    val loadFailed: Boolean,
    val arePhotosOpen: Boolean,
    val isDescriptionExpanded: Boolean,
    val similar: List<IosSimilarCoffee>,
    val similarLoading: Boolean,
    val similarFailed: Boolean,
)

data class IosCoffeeProduct(
    val name: String,
    val photo: String?,
    val countries: String,
    val productKind: String?,
    val productForm: String?,
    val descriptionText: String?,
    val roast: String?,
    val acidity: String?,
    val profile: String,
    val tasteDescriptors: List<String>,
    val roasterName: String,
    val roasterPhoto: String?,
    val canOpenRoaster: Boolean,
    val checkedAt: String?,
    val offers: List<IosCoffeeOffer>,
)

data class IosCoffeeOffer(
    val price: String?,
    val currency: String?,
    val weight: String?,
    val availability: String,
    val seller: String?,
    val grind: String?,
    val brew: String?,
    val source: String?,
    val checkedAt: String?,
    val canOpenSeller: Boolean,
)

data class IosSimilarCoffee(
    val slug: String,
    val name: String,
    val photo: String?,
    val countries: String,
    val tastes: List<String>,
    val offers: List<IosCoffeeOffer>,
    val initialWeight: String?,
)

internal fun CoffeeDetailSnapshot.toIosState() = IosCoffeeDetailState(
    product = details?.let { product(it) },
    isLoading = isLoading,
    loadFailed = loadFailed,
    arePhotosOpen = arePhotosOpen,
    isDescriptionExpanded = isDescriptionExpanded,
    similar = similar.map { similar(it) },
    similarLoading = similarLoading,
    similarFailed = similarFailed,
)

private fun CoffeeDetailSnapshot.product(details: CoffeeDetails) = IosCoffeeProduct(
    name = details.coffee.name,
    photo = details.photos.firstOrNull(),
    countries = details.coffee.countries.joinToString(", "),
    productKind = details.productKind,
    productForm = details.productForm,
    descriptionText = details.description,
    roast = label("roast", details.roastLevel),
    acidity = label("acidity", details.acidity),
    profile = tastes(details.coffee).joinToString(", "),
    tasteDescriptors = details.tasteDescriptors,
    roasterName = details.coffee.roasterName,
    roasterPhoto = details.coffee.roasterPhotoUrl,
    canOpenRoaster = !details.roasterSlug.isNullOrBlank(),
    checkedAt = details.checkedAtUtc?.let(::utcIsoToLocalDateTime),
    offers = details.coffee.offers.map { offer(it) },
)

private fun CoffeeDetailSnapshot.offer(offer: CoffeeOffer) = IosCoffeeOffer(
    price = offerPrice(offer),
    currency = offer.currency,
    weight = offer.weightGrams?.toString(),
    availability = offer.availability.name,
    seller = offer.sellerName,
    grind = offer.grind,
    brew = label("brew", offer.brewPurpose),
    source = offer.availabilityScope,
    checkedAt = offer.checkedAtUtc?.let(::utcIsoToLocalDateTime),
    canOpenSeller = !offer.sourceUrl.isNullOrBlank(),
)

private fun CoffeeDetailSnapshot.similar(coffee: Coffee) = IosSimilarCoffee(
    slug = coffee.slug,
    name = coffee.name,
    photo = coffee.photoUrl,
    countries = coffee.countries.joinToString(", "),
    tastes = tastes(coffee),
    offers = coffee.offers.distinctBy { it.weightGrams }
        .sortedBy { it.weightGrams ?: Int.MAX_VALUE }
        .map { offer(it) },
    initialWeight = coffee.offers.firstOrNull()?.weightGrams?.toString(),
)
