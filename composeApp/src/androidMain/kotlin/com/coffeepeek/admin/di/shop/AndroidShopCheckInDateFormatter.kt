package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.utils.datePickerMillisToUtcIsoInstant
import com.coffeepeek.admin.utils.formatVisitDate
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
    override fun nowMillis(): Long = now()
}
