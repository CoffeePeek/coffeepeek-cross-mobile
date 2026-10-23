package com.coffeepeek.feature.contributions.domain.model

enum class ModerationStatus { Pending, Approved, Rejected }

/** A shop the user sent. [publishedShopSlug] is set once it is live in the catalog and may lag behind [ModerationStatus.Approved]. */
data class ShopSubmission(
    val id: String,
    val name: String,
    val address: String?,
    val status: ModerationStatus,
    val rejectedReason: String?,
    val publishedShopSlug: String?,
)

/** A roaster the user sent. The server gives no link to the published roaster. */
data class RoasterSubmission(
    val id: String,
    val name: String,
    val about: String?,
    val status: ModerationStatus,
    val rejectedReason: String?,
)

data class Paged<T>(val items: List<T>, val totalCount: Int, val totalPages: Int, val currentPage: Int) {
    val hasMore: Boolean get() = currentPage < totalPages
}
