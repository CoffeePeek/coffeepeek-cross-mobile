package com.coffeepeek.feature.collections.data.backend

import kotlinx.serialization.Serializable

@Serializable internal data class CollectionEnvelope<T>(val data: T? = null, val isSuccess: Boolean = true)
@Serializable internal data class CollectionAddressDto(val slug: String, val canonicalPath: String = "")
@Serializable internal data class CollectionPhotoUrlsDto(val card: String? = null)
@Serializable internal data class CollectionPhotoDto(val fullUrl: String? = null, val urls: CollectionPhotoUrlsDto? = null)
@Serializable internal data class CollectionPlaceDto(
    val kind: String, val address: CollectionAddressDto? = null, val name: String = "",
    val coverPhoto: CollectionPhotoDto? = null, val cityName: String? = null, val streetAddress: String? = null,
    val rating: Double? = null, val checkInCount: Int? = null,
)
@Serializable internal data class CollectionDto(
    val id: String, val title: String, val canonicalPath: String, val items: List<CollectionPlaceDto>,
)
@Serializable internal data class CollectionSummaryDto(val id: String, val title: String, val canonicalPath: String, val itemCount: Int)
@Serializable internal data class CollectionSelectionDto(val kind: String, val slug: String)
@Serializable internal data class CreateCollectionDto(val title: String, val items: List<CollectionSelectionDto>)
