package com.coffeepeek.feature.shop.api

/** Form lifecycle metadata only; no form values, photos or identity cross this port. */
interface ShopCheckInAnalytics {
    fun opened() {}
    fun field(field: ShopCheckInField) {}
    fun submitted() {}
    fun validationFailed() {}
    fun failed(error: Throwable) {}
    fun saved(public: Boolean, hasPhotos: Boolean) {}
    fun cancelled() {}

    companion object { val None: ShopCheckInAnalytics = object : ShopCheckInAnalytics {} }
}

enum class ShopCheckInField { Note, Date, Drink, RatingCoffee, RatingService, RatingPlace, Photos, Visibility }
