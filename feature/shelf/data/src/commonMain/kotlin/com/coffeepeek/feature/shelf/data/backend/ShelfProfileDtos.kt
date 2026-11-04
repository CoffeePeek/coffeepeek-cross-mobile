package com.coffeepeek.feature.shelf.data.backend

import kotlinx.serialization.Serializable

@Serializable
internal data class ShelfProfileVisibilityDto(val isVisible: Boolean)

@Serializable
internal data class PublicShelfDto(val items: List<PublicShelfItemDto>, val totalCount: Int)

@Serializable
internal data class PublicShelfItemDto(
    val id: String,
    val name: String,
    val roasterName: String? = null,
    val coffeeSlug: String? = null,
    val photoUrl: String? = null,
)
