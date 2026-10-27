package com.coffeepeek.feature.contributions.data.mapper

import com.coffeepeek.core.network.flexibleIntOrNull
import com.coffeepeek.feature.contributions.data.backend.RefCatalogItemDto
import com.coffeepeek.feature.contributions.data.backend.RefDrinkDto
import com.coffeepeek.feature.contributions.data.backend.RefPhotoUrlsDto
import com.coffeepeek.feature.contributions.data.backend.RefShopDto
import com.coffeepeek.feature.contributions.data.backend.RefPhotoDto
import com.coffeepeek.feature.contributions.domain.model.Availability
import com.coffeepeek.feature.contributions.domain.model.CatalogOption
import com.coffeepeek.feature.contributions.domain.model.Contacts
import com.coffeepeek.feature.contributions.domain.model.DrinkOption
import com.coffeepeek.feature.contributions.domain.model.MenuRow
import com.coffeepeek.feature.contributions.domain.model.ShopPhotoRef
import com.coffeepeek.feature.contributions.domain.model.ShopSnapshot
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

internal fun RefCatalogItemDto.toOption() = CatalogOption(key, name.orEmpty())

internal fun RefDrinkDto.toOption() = DrinkOption(slug, nameRu.ifBlank { nameEn }.ifBlank { slug })

/** The picked variant, else the full url; null when both are blank. */
private fun String?.orFull(full: String?): String? = this?.takeIf(String::isNotBlank) ?: full?.takeIf(String::isNotBlank)

private fun RefPhotoUrlsDto?.fullscreenOr(full: String?) = this?.fullscreen.orFull(full)

/** The shop photos as the legacy details reader built them: the small variant, skipping a photo with no id or no url. */
private fun List<RefPhotoDto>.shopPhotos(): List<ShopPhotoRef> = mapNotNull { photo ->
    val id = photo.id.takeIf(String::isNotBlank) ?: return@mapNotNull null
    val full = photo.urls.fullscreenOr(photo.fullUrl) ?: return@mapNotNull null
    (photo.sortIndex.flexibleIntOrNull() ?: 0) to ShopPhotoRef(id, photo.urls?.thumbnail.orFull(full) ?: full)
}.sortedBy { it.first }.map { it.second }

/** The menu photos: the detail variant, skipping a photo with no url; the id is kept as it came. */
private fun List<RefPhotoDto>.menuPhotos(): List<ShopPhotoRef> = mapNotNull { photo ->
    val full = photo.urls.fullscreenOr(photo.fullUrl) ?: return@mapNotNull null
    (photo.sortIndex.flexibleIntOrNull() ?: 0) to ShopPhotoRef(photo.id, photo.urls?.detail.orFull(full) ?: full)
}.sortedBy { it.first }.map { it.second }

private fun JsonElement?.toPrice(): Double? {
    val primitive = this as? JsonPrimitive ?: return null
    primitive.doubleOrNull?.let { return it }
    return primitive.contentOrNull?.trim()?.replace(',', '.')?.toDoubleOrNull()
}

private fun JsonElement?.tagIds(): List<String> {
    val array = this as? JsonArray ?: return emptyList()
    return array.mapNotNull { item ->
        ((item as? JsonObject)?.get("slug") as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)
    }.distinct()
}

private fun String.toAvailability() = when (lowercase()) {
    "present", "1" -> Availability.Present
    "absent", "2" -> Availability.Absent
    else -> Availability.Unknown
}

/** [catalogOrder] is the slug order of the menu drinks catalog; rows it does not know keep their place after the known ones. */
internal fun RefShopDto.toSnapshot(slug: String, catalogOrder: List<String>?): ShopSnapshot {
    val order = catalogOrder?.takeIf(List<String>::isNotEmpty)?.withIndex()?.associate { it.value to it.index }
    val rows = menu?.items.orEmpty().map { MenuRow(it.slug, it.availability.toAvailability(), it.price.toPrice()) }
    return ShopSnapshot(
        slug = slug,
        title = name.orEmpty(),
        description = description,
        contacts = Contacts(shopContact?.phoneNumber, shopContact?.email, shopContact?.siteLink, shopContact?.instagramLink),
        photos = photos.shopPhotos(),
        tagIds = shopTags.tagIds().ifEmpty { tags.tagIds() },
        roasterIds = roasters.map { it.key },
        equipmentIds = equipments.map { it.key },
        brewMethodIds = brewMethods.map { it.key },
        menu = if (order == null) rows else rows.sortedBy { order[it.slug] ?: Int.MAX_VALUE },
        menuPhotos = menu?.photos.orEmpty().menuPhotos(),
    )
}
