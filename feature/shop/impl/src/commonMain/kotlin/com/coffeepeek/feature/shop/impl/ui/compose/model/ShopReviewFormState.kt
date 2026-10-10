package com.coffeepeek.feature.shop.impl.ui.compose.model

import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.model.ShopReviewPhoto
import com.coffeepeek.feature.shop.domain.usecase.ShopReviewFieldError

internal enum class ShopReviewFormMode { Create, Edit }

internal data class ShopReviewFormState(
    val mode: ShopReviewFormMode = ShopReviewFormMode.Create,
    val shopName: String? = null,
    val header: String = "",
    val comment: String = "",
    val rating: ShopRating = ShopRating(place = 4, service = 4, coffee = 4),
    val headerError: ShopReviewFieldError? = null,
    val commentError: ShopReviewFieldError? = null,
    val submitError: String? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val canEdit: Boolean = true,
    val existingPhotoUrls: List<String> = emptyList(),
    val newPhotos: List<ShopReviewPhoto> = emptyList(),
    val draftRestored: Boolean = false,
    val isPhotoLoading: Boolean = false,
    val draftError: Boolean = false,
    val loadError: Boolean = false,
    val ignoredDraftPhotos: Boolean = false,
)
