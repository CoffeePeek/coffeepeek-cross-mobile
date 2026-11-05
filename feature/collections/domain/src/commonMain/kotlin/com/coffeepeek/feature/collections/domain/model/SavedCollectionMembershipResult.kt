package com.coffeepeek.feature.collections.domain.model

/** Confirmed lists with optional server metadata; null preserves compatibility with older servers. */
data class SavedCollectionMembershipResult(
    val lists: List<SavedCollectionSummary>,
    val isFavorite: Boolean? = null,
    val membershipChanged: Boolean? = null,
)
