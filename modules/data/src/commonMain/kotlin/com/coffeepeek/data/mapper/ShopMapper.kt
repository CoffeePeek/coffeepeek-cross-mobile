package com.coffeepeek.data.mapper

import com.coffeepeek.data.time.utcSchedulesToLocal
import com.coffeepeek.api.model.response.shop.CoffeeShopDetailsDto
import com.coffeepeek.api.model.response.shop.ReviewDto
import com.coffeepeek.api.model.response.shop.ShopMenuDto
import com.coffeepeek.api.model.response.shop.ShopMenuItemDto
import com.coffeepeek.api.model.response.shop.ShortShopDto
import com.coffeepeek.api.model.response.shop.variantOr
import com.coffeepeek.data.util.FileUrlResolver
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.model.CoffeeShopType
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.Review
import com.coffeepeek.domain.model.ReviewRating
import com.coffeepeek.domain.model.ShopMenu
import com.coffeepeek.domain.model.ShopMenuItem
import com.coffeepeek.domain.model.ShopMenuPhoto
import com.coffeepeek.domain.model.ShopPhoto
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

internal object ShopMapper {

    fun ShortShopDto.toDomain() = CoffeeShop(
        id = address.slug,
        publicAddress = address.toDomain(),
        title = name,
        rating = rating.takeIf { it > 0 },
        cityName = null,
        priceRange = priceRangeLabel(priceRange),
        photoUrl = photos.firstOrNull()?.let { it.urls.variantOr(it.fullUrl) { u -> u.card } },
        isFavorite = isFavorite,
        address = location?.address,
        isOpen = isOpen,
        isNew = isNew,
        isVisited = isVisited,
        reviewCount = reviewCount,
        tags = extractBackendTags(tags, shopTags)
            .ifEmpty { (brewMethods + beans).mapNotNull { it.name?.takeIf(String::isNotBlank) } }
            .take(3),
        brewMethods = brewMethods.mapNotNull { it.name?.takeIf(String::isNotBlank) },
        roasterPhotoUrls = roasters.mapNotNull { it.photoUrl?.takeIf(String::isNotBlank) }.distinct(),
        type = parseShopType(type, coffeeFocus),
        location = location?.toDomain(),
    )

    fun CoffeeShopDetailsDto.toDomain(fileUrls: FileUrlResolver) = CoffeeShopDetails(
        shop = CoffeeShop(
            id = address.slug,
            publicAddress = address.toDomain(),
            title = name.orEmpty(),
            rating = rating.takeIf { it > 0 },
            cityName = null,
            priceRange = priceRangeLabel(priceRange),
            photoUrl = photos.firstOrNull()?.let { it.urls.variantOr(it.fullUrl) { u -> u.card } },
            isFavorite = isFavorite,
            address = location?.address,
            isOpen = isOpen,
            isNew = isNew,
            isVisited = isVisited,
            reviewCount = reviewCount,
            tags = extractBackendTags(tags, shopTags)
                .ifEmpty {
                    (brewMethods + coffeeBeans)
                        .mapNotNull { it.name?.takeIf(String::isNotBlank) }
                }
                .take(3),
            brewMethods = brewMethods.mapNotNull { it.name?.takeIf(String::isNotBlank) },
            roasterPhotoUrls = roasters.mapNotNull { it.photoUrl?.takeIf(String::isNotBlank) }.distinct(),
            type = parseShopType(type, coffeeFocus),
            location = location?.toDomain(),
        ),
        cityId = city?.slug.orEmpty(),
        description = description,
        location = location?.toDomain(),
        isVisited = isVisited,
        isNew = isNew,
        canCreateReview = canCreateReview,
        existingReviewId = existingReviewId,
        photos = photos.mapNotNull { it.urls.variantOr(it.fullUrl) { u -> u.detail } },
        fullscreenPhotos = photos.mapNotNull { it.urls.variantOr(it.fullUrl) { u -> u.fullscreen } },
        shopPhotos = photos.mapNotNull { photo ->
            val id = photo.id.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val url = photo.urls.variantOr(photo.fullUrl) { it.fullscreen } ?: return@mapNotNull null
            ShopPhoto(
                id = id,
                fullUrl = url,
                previewUrl = photo.urls.variantOr(url) { it.thumbnail } ?: url,
                sortIndex = photo.sortIndex,
            )
        }.sortedBy { it.sortIndex },
        reviews = reviews.map { it.toDomain(fileUrls) },
        userCheckIns = userCheckIns.map { checkIn ->
            CheckIn(
                drinkSlug = checkIn.drinkSlug,
                customDrinkName = checkIn.customDrinkName,
                drinkNameRu = checkIn.drinkNameRu,
                drinkNameEn = checkIn.drinkNameEn,
                id = checkIn.id,
                shopId = checkIn.shop?.slug.orEmpty(),
                shopName = checkIn.shopName.orEmpty().ifBlank { name.orEmpty() },
                note = checkIn.note.orEmpty(),
                createdAt = checkIn.createdAt,
                reviewId = checkIn.reviewId,
                visitedAt = checkIn.visitedAt,
                photoUrls = checkIn.photos.mapNotNull { photo ->
                    fileUrls.resolve(photo.storageKey, photo.urls.variantOr(photo.fullUrl) { it.fullscreen })
                },
                photoThumbnailUrls = checkIn.photos.mapNotNull { photo ->
                    fileUrls.resolve(photo.storageKey, photo.urls.variantOr(photo.fullUrl) { it.thumbnail })
                },
                rating = checkIn.rating?.let { rating ->
                    ReviewRating(
                        place = rating.place,
                        service = rating.service,
                        coffee = rating.coffee,
                    )
                },
            )
        },
        contact = shopContact?.let { c ->
            com.coffeepeek.domain.model.ShopContact(
                instagram = c.instagramLink,
                email = c.email,
                website = c.siteLink,
                phone = c.phoneNumber,
            )
        },
        brewMethods = brewMethods.mapNotNull { it.name?.takeIf(String::isNotBlank) },
        brewMethodItems = brewMethods.map {
            CatalogItem(id = it.key, name = it.name.orEmpty(), slug = it.key, photoUrl = it.photoUrl, address = it.address?.toDomain())
        },
        coffeeBeans = coffeeBeans.mapNotNull { it.name?.takeIf(String::isNotBlank) },
        roasters = roasters.map {
            CatalogItem(
                id = it.key,
                name = it.name.orEmpty(),
                slug = it.key,
                photoUrl = it.photoUrl,
                address = it.address?.toDomain(),
            )
        },
        equipment = equipments.mapNotNull { it.name?.takeIf(String::isNotBlank) },
        equipmentItems = equipments.map {
            CatalogItem(id = it.key, name = it.name.orEmpty(), slug = it.key, photoUrl = it.photoUrl, address = it.address?.toDomain())
        },
        tagItems = parseTagItems(shopTags).ifEmpty { parseTagItems(tags) },
        schedules = utcSchedulesToLocal(schedules.orEmpty().map { schedule ->
            com.coffeepeek.domain.model.ShopSchedule(
                dayOfWeek = parseDayOfWeek(schedule.dayOfWeek),
                isClosed = schedule.isClosed,
                intervals = schedule.intervals.orEmpty().map { interval ->
                    com.coffeepeek.domain.model.ScheduleInterval(
                        openTime = interval.openTime,
                        closeTime = interval.closeTime,
                    )
                },
            )
        }),
        menu = menu?.toDomain(),
    )

    private fun com.coffeepeek.api.model.response.shop.LocationDto.toDomain() =
        com.coffeepeek.domain.model.ShopLocation(
            address = address,
            latitude = latitude,
            longitude = longitude,
        )

    fun ReviewDto.toDomain(fileUrls: FileUrlResolver) = Review(
        drinkSlug = drinkSlug,
        customDrinkName = customDrinkName,
        drinkNameRu = drinkNameRu,
        drinkNameEn = drinkNameEn,
        id = id,
        moderationReviewId = moderationReviewId,
        shopId = shop?.slug.orEmpty(),
        userId = author?.slug.orEmpty(),
        username = username.orEmpty(),
        header = header.orEmpty(),
        comment = comment.orEmpty(),
        rating = ReviewRating(
            place = rating.place,
            service = rating.service,
            coffee = rating.coffee,
        ),
        createdAt = createdAtUtc,
        photoUrls = photos.mapNotNull { photo ->
            fileUrls.resolve(photo.storageKey, photo.fullUrl)
        },
        helpfulCount = helpfulCount,
        isHelpfulByCurrentUser = isHelpfulByCurrentUser,
    )

    fun ShopMenuDto.toDomain() = ShopMenu(
        capturedAtUtc = capturedAtUtc,
        updatedAtUtc = updatedAtUtc,
        currency = currency.ifBlank { "BYN" },
        items = items.map { it.toDomain() },
        photos = photos
            .mapNotNull { photo ->
                val url = photo.urls.variantOr(photo.fullUrl) { it.fullscreen } ?: return@mapNotNull null
                ShopMenuPhoto(
                    id = photo.id,
                    fullUrl = url,
                    previewUrl = photo.urls.variantOr(url) { it.detail } ?: url,
                    sortIndex = photo.sortIndex,
                )
            }
            .sortedBy { it.sortIndex },
    )

    private fun ShopMenuItemDto.toDomain() = ShopMenuItem(
        slug = slug,
        nameRu = nameRu,
        nameEn = nameEn,
        category = category,
        availability = availability,
        price = price.toDoubleOrNull(),
        currency = currency.ifBlank { "BYN" },
        volumeMl = volumeMl.toIntOrNull(),
    )

    fun parseShopType(type: JsonElement?, coffeeFocus: JsonElement? = null): String {
        val raw = type.takeUnless { it == null || it is JsonNull } ?: coffeeFocus
        return CoffeeShopType.fromApi((raw as? JsonPrimitive)?.contentOrNull)
    }

    private fun JsonElement?.toDoubleOrNull(): Double? {
        val primitive = this as? JsonPrimitive ?: return null
        primitive.doubleOrNull?.let { return it }
        return primitive.contentOrNull
            ?.trim()
            ?.replace(',', '.')
            ?.toDoubleOrNull()
    }

    private fun priceRangeLabel(range: JsonElement?): String? {
        val value = (range as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
        if (value.isBlank()) return null
        return when (value.lowercase()) {
            "1", "cheap", "$" -> "$"
            "2", "moderate", "$$" -> "$$"
            "3", "expensive", "$$$" -> "$$$"
            "4", "luxury", "$$$$" -> "$$$$"
            else -> null
        }
    }

    private fun parseDayOfWeek(raw: JsonElement?): Int {
        val token = (raw as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
        if (token.isBlank()) return 0
        return when (token.lowercase()) {
            "0", "sunday" -> 0
            "1", "monday" -> 1
            "2", "tuesday" -> 2
            "3", "wednesday" -> 3
            "4", "thursday" -> 4
            "5", "friday" -> 5
            "6", "saturday" -> 6
            else -> token.toIntOrNull()?.coerceIn(0, 6) ?: 0
        }
    }

    private fun JsonElement?.toIntOrNull(): Int? {
        val primitive = this as? JsonPrimitive ?: return null
        primitive.contentOrNull?.trim()?.toIntOrNull()?.let { return it }
        return primitive.doubleOrNull?.toInt()
    }

    private fun parseTagItems(raw: JsonElement?): List<CatalogItem> {
        val array = raw as? JsonArray ?: return emptyList()
        return array.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = obj.readString("slug") ?: return@mapNotNull null
            if (id.isBlank()) return@mapNotNull null
            CatalogItem(
                id = id,
                name = obj.readTagName().orEmpty(),
                slug = obj.readString("slug").orEmpty(),
            )
        }.distinctBy { it.id }
    }

    private fun JsonObject.readString(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun extractBackendTags(vararg rawCandidates: JsonElement?): List<String> {
        val parsed = rawCandidates
            .asSequence()
            .flatMap { parseTagNames(it).asSequence() }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        return parsed.distinct()
    }

    private fun parseTagNames(raw: JsonElement?): List<String> {
        val array = raw as? JsonArray ?: return emptyList()
        return array.mapNotNull { item ->
            when (item) {
                is JsonPrimitive -> item.contentOrNull
                is JsonObject -> item.readTagName()
                else -> null
            }
        }
    }

    private fun JsonObject.readTagName(): String? {
        val keys = listOf("name", "title", "label", "value")
        return keys.asSequence()
            .mapNotNull { key ->
                (this[key] as? JsonPrimitive)?.contentOrNull
            }
            .firstOrNull { it.isNotBlank() }
    }
}
