package com.coffeepeek.feature.profile.impl.ui.data

import com.coffeepeek.feature.profile.domain.PublicCheckInDrink
import com.coffeepeek.feature.profile.domain.PublicProfileCheckIn
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoStripSources

internal fun PublicCheckInDrink?.profileCheckInDrink(language: String): String? = this?.let {
    it.customName?.takeIf(String::isNotBlank) ?: if (language == "en") {
        it.nameEn?.takeIf(String::isNotBlank) ?: it.nameRu?.takeIf(String::isNotBlank)
    } else it.nameRu?.takeIf(String::isNotBlank) ?: it.nameEn?.takeIf(String::isNotBlank)
}

internal fun PublicProfileCheckIn.profileCheckInPhotos() = CoffeePeekPhotoStripSources(photoUrls, photoPreviewUrls, photoDetailUrls)
