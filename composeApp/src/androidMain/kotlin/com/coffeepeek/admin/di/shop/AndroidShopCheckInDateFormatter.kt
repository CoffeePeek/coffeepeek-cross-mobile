package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.utils.datePickerMillisToUtcIsoInstant
import com.coffeepeek.admin.utils.formatVisitDate
import com.coffeepeek.admin.utils.datePickerMillisWithLocalTime
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDateFormatter
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

internal class AndroidShopCheckInDateFormatter(
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val now: () -> Long = System::currentTimeMillis,
) : ShopCheckInDateFormatter {
    override fun label(visitedAtIso: String): String = pickerMillis(visitedAtIso)?.let(::formatVisitDate).orEmpty()

    override fun pickerMillis(visitedAtIso: String): Long? = try {
        Instant.parse(visitedAtIso).atZone(zone()).toLocalDate()
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    } catch (_: IllegalArgumentException) { null }
      catch (_: java.time.DateTimeException) { null }

    override fun visitInstant(pickerMillis: Long): String = datePickerMillisToUtcIsoInstant(pickerMillis, zone())
    override fun visitInstant(pickerMillis: Long, referenceIso: String): String =
        Instant.ofEpochMilli(datePickerMillisWithLocalTime(pickerMillis,
            Instant.parse(referenceIso).toEpochMilli(), zone()).coerceAtMost(now())).toString()
    /** Today's local calendar day as the UTC-midnight value the date picker works in, so "today" stays selectable after local midnight in any offset. */
    override fun nowMillis(): Long = Instant.ofEpochMilli(now()).atZone(zone()).toLocalDate()
        .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
