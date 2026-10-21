package com.coffeepeek.feature.homecheckin.domain.model

/** Issued month-wide preview, independent of the selected timeline page/day. */
data class HomePreparationCalendarPhoto(val url: String, val isCutout: Boolean, val preparedAtUtc: String? = null)
