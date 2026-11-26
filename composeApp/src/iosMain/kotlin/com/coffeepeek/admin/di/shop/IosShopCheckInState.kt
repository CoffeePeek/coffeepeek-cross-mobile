package com.coffeepeek.admin.di.shop

import platform.UIKit.UIImage

data class IosShopCheckInState(
    val shopName: String,
    val shopAddress: String?,
    val note: String,
    val placeRating: Int,
    val serviceRating: Int,
    val coffeeRating: Int,
    val visitDate: String,
    val today: String,
    val dateLabel: String,
    val isPublic: Boolean,
    val drinkSlug: String?,
    val customDrink: String,
    val drinks: List<IosShopCheckInDrink>,
    val photos: List<IosShopCheckInPhoto>,
    val active: Boolean,
    val loading: Boolean,
    val editable: Boolean,
    val photosPreparing: Boolean,
    val draftFailed: Boolean,
    val hasDraft: Boolean,
    val confirmDiscard: Boolean,
    val discarding: Boolean,
    val drinksLoading: Boolean,
    val drinksFailed: Boolean,
    val submitting: Boolean,
    val submitted: Boolean,
    val publication: String,
    val deliveryUnconfirmed: Boolean,
    val failureKey: String?,
    val serverMessage: String?,
    val validationKey: String?,
)

data class IosShopCheckInDrink(val slug: String, val name: String)
data class IosShopCheckInPhoto(val index: Int, val image: UIImage?, val name: String)
