package com.coffeepeek.feature.shop.impl.ui.compose.model

import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopConsumedDrinkOption
import com.coffeepeek.feature.shop.domain.usecase.ShopCheckInValidationError

internal data class ShopCheckInFormState(
    val input: ShopCheckInCreateInput,
    val isLoading: Boolean = true,
    val draftFailed: Boolean = false,
    val hasDraft: Boolean = false,
    val confirmDiscard: Boolean = false,
    val isDiscarding: Boolean = false,
    val drinks: List<ShopConsumedDrinkOption> = emptyList(),
    val drinksLoading: Boolean = false,
    val drinksFailed: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitted: Boolean = false,
    val deliveryUnconfirmed: Boolean = false,
    val submitFailure: ShopCheckInSubmitFailure? = null,
    val validationError: ShopCheckInValidationError? = null,
) {
    val submitFailed: Boolean get() = submitFailure != null
}

/** Why a submit failed: the HTTP [status] and the server's own [serverMessage] when it sent one; both null for a local failure. */
data class ShopCheckInSubmitFailure(val status: Int? = null, val serverMessage: String? = null)
