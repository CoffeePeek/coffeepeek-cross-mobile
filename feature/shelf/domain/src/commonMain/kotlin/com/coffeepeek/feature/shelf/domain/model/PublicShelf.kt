package com.coffeepeek.feature.shelf.domain.model

/** Deliberately excludes the owner's archive, notes, purchase details and private bag actions. */
data class PublicShelfItem(
    val id: String,
    val name: String,
    val roasterName: String?,
    val coffeeSlug: String?,
    val photoUrl: String?,
)

data class PublicShelf(val items: List<PublicShelfItem>, val totalCount: Int)
