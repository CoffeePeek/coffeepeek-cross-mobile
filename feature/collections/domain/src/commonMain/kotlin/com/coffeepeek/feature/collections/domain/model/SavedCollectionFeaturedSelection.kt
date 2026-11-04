package com.coffeepeek.feature.collections.domain.model

/** A verified owner preference; null collectionId means the user chose not to feature a list. */
data class SavedCollectionFeaturedSelection(val collectionId: String?)
