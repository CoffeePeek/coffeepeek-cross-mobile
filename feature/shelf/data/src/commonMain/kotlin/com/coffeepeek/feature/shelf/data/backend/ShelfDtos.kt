package com.coffeepeek.feature.shelf.data.backend

import kotlinx.serialization.Serializable

@Serializable internal data class ShelfEnvelope<T>(val isSuccess: Boolean, val message: String? = null, val data: T? = null)

@Serializable internal data class ShelfPhotoDto(val id: String, val url: String)

@Serializable internal data class ShelfBagDto(
    val id: String,
    val status: String,
    val position: Int? = null,
    val photo: ShelfPhotoDto? = null,
    val coffeeProductId: String? = null,
    val coffeeProductSlug: String? = null,
    val name: String? = null,
    val roasterName: String? = null,
    val weightGrams: Int? = null,
    val grind: String? = null,
    val roastPurpose: String? = null,
    val roastedOn: String? = null,
    val addedAtUtc: String,
    val finishedAtUtc: String? = null,
    val rating: Int? = null,
    val buyAgain: Boolean? = null,
)

@Serializable internal data class ShelfArchiveDto(val items: List<ShelfBagDto>, val totalCount: Int)

/** Create and PUT body; PUT replaces every detail and keeps the card when [photo] is null. */
@Serializable internal data class ShelfBagWriteDto(
    val photo: UploadedCardDto? = null,
    val coffeeProductSlug: String? = null,
    val coffeeProductId: String? = null,
    val customName: String? = null,
    val roasterName: String? = null,
    val weightGrams: Int? = null,
    val grind: String? = null,
    val roastPurpose: String? = null,
    val roastedOn: String? = null,
)

@Serializable internal data class ShelfOrderDto(val bagIds: List<String>)
@Serializable internal data class ShelfFinishDto(val rating: Int? = null, val buyAgain: Boolean? = null)
@Serializable internal data class ShelfRestoreDto(val position: Int? = null)

@Serializable internal data class CardUploadRequestDto(val sizeBytes: Int, val fileName: String, val contentType: String)
@Serializable internal data class CardUploadUrlDto(val uploadUrl: String, val storageKey: String)
@Serializable internal data class UploadedCardDto(val fileName: String, val contentType: String, val storageKey: String, val size: Long)
