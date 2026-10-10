package com.coffeepeek.feature.shop.impl.ui.data

/** The app owns timezone/date-picker conversion; shared Compose never uses platform APIs. */
interface ShopCheckInDateFormatter {
    fun label(visitedAtIso: String): String
    fun pickerMillis(visitedAtIso: String): Long?
    fun visitInstant(pickerMillis: Long): String
    fun nowMillis(): Long
}
