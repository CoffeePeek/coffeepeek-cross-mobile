package com.coffeepeek.feature.shop.impl.ui.compose.model

internal enum class ShopReviewRatingKind { Place, Service, Coffee }

internal sealed interface ShopReviewFormAction {
    data class HeaderChanged(val value: String) : ShopReviewFormAction
    data class CommentChanged(val value: String) : ShopReviewFormAction
    data class RatingChanged(val kind: ShopReviewRatingKind, val value: Int) : ShopReviewFormAction
    data object Submit : ShopReviewFormAction
}
