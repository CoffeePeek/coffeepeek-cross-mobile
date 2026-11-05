package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.analytics.*
import com.coffeepeek.admin.feature.checkin.api.CheckInFlowHandoff
import com.coffeepeek.feature.shop.api.ShopCheckInAnalytics
import com.coffeepeek.feature.shop.api.ShopCheckInField
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInPhotoUploadFailure

internal class AppShopCheckInAnalytics(private val isCurrent: () -> Boolean) : ShopCheckInAnalytics {
    private var tracker: CheckInTracker? = null

    override fun opened() {
        if (!isCurrent() || tracker != null) return
        tracker = (CheckInFlowHandoff.take() ?: CheckInTracker(CheckInKind.Visit, CheckInSource.Shop))
            .also { it.step(CheckInStep.Editor) }
    }

    override fun field(field: ShopCheckInField) { current()?.field(field.wire()) }
    override fun submitted() { current()?.submit() }
    override fun validationFailed() { current()?.validation(listOf("form")) }

    override fun failed(error: Throwable) {
        val photoFailure = error is ShopCheckInPhotoUploadFailure
        val safe = safeAnalyticsError(if (photoFailure) error.cause ?: error else error)
        if (safe.status == 400 || safe.status == 422) current()?.validation(listOf("form"), server = true)
        else current()?.failed(if (photoFailure) CheckInStage.Photo else CheckInStage.Save, safe)
    }

    override fun saved(public: Boolean, hasPhotos: Boolean) {
        current()?.saved(if (public) "Public" else "Private", hasPhotos)
        tracker = null
    }

    override fun cancelled() {
        current()?.cancel("close")
        tracker = null
    }

    private fun current() = tracker?.takeIf { isCurrent() }
}

private fun ShopCheckInField.wire(): String = when (this) {
    ShopCheckInField.Note -> "note"
    ShopCheckInField.Date -> "date"
    ShopCheckInField.Drink -> "drink"
    ShopCheckInField.RatingCoffee -> "rating_coffee"
    ShopCheckInField.RatingService -> "rating_service"
    ShopCheckInField.RatingPlace -> "rating_place"
    ShopCheckInField.Photos -> "photos"
    ShopCheckInField.Visibility -> "visibility"
}
