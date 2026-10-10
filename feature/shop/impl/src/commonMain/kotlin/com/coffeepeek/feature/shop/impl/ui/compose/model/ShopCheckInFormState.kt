package com.coffeepeek.feature.shop.impl.ui.compose.model

import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopConsumedDrinkOption
import com.coffeepeek.feature.shop.domain.usecase.ShopCheckInValidationError

internal data class ShopCheckInFormState(
    val input: ShopCheckInCreateInput,
    val isLoading: Boolean = true,
    val draftFailed: Boolean = false,
    val drinks: List<ShopConsumedDrinkOption> = emptyList(),
    val drinksLoading: Boolean = false,
    val drinksFailed: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitted: Boolean = false,
    val deliveryUnconfirmed: Boolean = false,
    val submitFailed: Boolean = false,
    val validationError: ShopCheckInValidationError? = null,
)
