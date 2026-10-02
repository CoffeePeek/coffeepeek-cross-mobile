package com.coffeepeek.feature.shop.domain.model

/** One snapshot from shop details; feature actions are coordinated separately. */
data class ShopDetails(
    val overview: ShopOverview,
    val menu: ShopMenu?,
    val schedules: List<ShopSchedule>,
)

data class ShopMenu(
    val capturedAtUtc: String? = null,
    val updatedAtUtc: String? = null,
    val currency: String = "BYN",
    val items: List<ShopMenuItem> = emptyList(),
    val photos: List<MenuPhoto> = emptyList(),
)

data class ShopMenuItem(
    val slug: String,
    val nameRu: String,
    val nameEn: String,
    val category: String,
    val availability: String,
    val price: Double?,
    val currency: String,
    val volumeMl: Int?,
)

/** Day 0 is Sunday, matching the current API and legacy application model. */
data class ShopSchedule(
    val dayOfWeek: Int,
    val isClosed: Boolean,
    val intervals: List<ScheduleInterval> = emptyList(),
)

data class ScheduleInterval(
    val openTime: String,
    val closeTime: String,
)
