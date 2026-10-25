package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.utils.currentEpochMillis
import com.coffeepeek.admin.utils.datePickerMillisToUtcIsoInstant
import com.coffeepeek.admin.utils.datePickerMillisWithLocalTime
import com.coffeepeek.admin.utils.instantToDatePickerMillis
import com.coffeepeek.admin.utils.formatVisitDate
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDateFormatter
import kotlin.time.Instant

/** Reuses the app's platform calendar conversion, including DST and retained sub-second visit time. */
internal class AppShopCheckInDateFormatter(
    private val now: () -> Long = ::currentEpochMillis,
) : ShopCheckInDateFormatter {
    override fun label(visitedAtIso: String): String = pickerMillis(visitedAtIso)?.let(::formatVisitDate).orEmpty()

    override fun pickerMillis(visitedAtIso: String): Long? = try {
        instantToDatePickerMillis(Instant.parse(visitedAtIso).toEpochMilliseconds())
    } catch (_: IllegalArgumentException) {
        null
    }

    override fun visitInstant(pickerMillis: Long): String = datePickerMillisToUtcIsoInstant(pickerMillis)

    override fun visitInstant(pickerMillis: Long, referenceIso: String): String =
        Instant.fromEpochMilliseconds(datePickerMillisWithLocalTime(
            pickerMillis, Instant.parse(referenceIso).toEpochMilliseconds(),
        ).coerceAtMost(now())).toString()

    override fun nowMillis(): Long = instantToDatePickerMillis(now())
}
