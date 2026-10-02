package com.coffeepeek.feature.shop.impl.ui.compose.model

import com.coffeepeek.feature.shop.domain.model.ShopReviewPhoto

internal enum class ShopReviewRatingKind { Place, Service, Coffee }

internal sealed interface ShopReviewFormAction {
    data class HeaderChanged(val value: String) : ShopReviewFormAction
    data class CommentChanged(val value: String) : ShopReviewFormAction
    data class RatingChanged(val kind: ShopReviewRatingKind, val value: Int) : ShopReviewFormAction
    data class PhotosAdded(val photos: List<ShopReviewPhoto>) : ShopReviewFormAction
    data class RemovePhoto(val index: Int) : ShopReviewFormAction
    data object DiscardDraft : ShopReviewFormAction
    data object Submit : ShopReviewFormAction
}
