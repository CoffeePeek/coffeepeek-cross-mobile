package com.coffeepeek.feature.shop.domain.usecase

const val MIN_SHOP_REVIEW_HEADER_LENGTH = 3
const val MAX_SHOP_REVIEW_HEADER_LENGTH = 120
const val MIN_SHOP_REVIEW_COMMENT_LENGTH = 10
const val MAX_SHOP_REVIEW_COMMENT_LENGTH = 2000

enum class ShopReviewFieldError { Required, TooShort, TooLong }

data class ShopReviewTextValidation(
    val headerError: ShopReviewFieldError? = null,
    val commentError: ShopReviewFieldError? = null,
) {
    val isValid: Boolean get() = headerError == null && commentError == null
}

/** The same minimum lengths as the legacy editor, with explicit bounds for non-UI callers. */
fun validateShopReviewText(header: String, comment: String): ShopReviewTextValidation =
    ShopReviewTextValidation(
        headerError = validateLength(header, MIN_SHOP_REVIEW_HEADER_LENGTH, MAX_SHOP_REVIEW_HEADER_LENGTH),
        commentError = validateLength(comment, MIN_SHOP_REVIEW_COMMENT_LENGTH, MAX_SHOP_REVIEW_COMMENT_LENGTH),
    )

private fun validateLength(value: String, minimum: Int, maximum: Int): ShopReviewFieldError? {
    val length = value.trim().length
    return when {
        length == 0 -> ShopReviewFieldError.Required
        length < minimum -> ShopReviewFieldError.TooShort
        length > maximum -> ShopReviewFieldError.TooLong
        else -> null
    }
}
