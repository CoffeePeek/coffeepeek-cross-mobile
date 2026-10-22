package com.coffeepeek.admin.utils

internal fun appReviewUrl(platform: String, channel: String?): String? = when {
    platform == "ios" -> "https://apps.apple.com/app/id6821851267?action=write-review"
    platform == "android" && channel == "play" -> "https://play.google.com/store/apps/details?id=com.coffeepeek"
    else -> null
}
