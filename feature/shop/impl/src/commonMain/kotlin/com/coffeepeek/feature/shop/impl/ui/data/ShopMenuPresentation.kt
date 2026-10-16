package com.coffeepeek.feature.shop.impl.ui.data

import com.coffeepeek.feature.shop.domain.model.ShopMenuItem
import kotlin.math.abs
import kotlin.math.round

internal fun groupedPresentMenuItems(items: List<ShopMenuItem>): List<List<ShopMenuItem>> {
    val groups = items.filter { it.availability.equals("Present", ignoreCase = true) }
        .groupBy(ShopMenuItem::category)
    val known = listOf("Espresso", "Filter")
    return known.mapNotNull(groups::get) + groups.filterKeys { it !in known }.values
}

/** Match the existing menu's two-decimal rounding, including the decimal comma. */
internal fun formatShopMenuAmount(amount: Double): String {
    val cents = round(amount * 100).toLong()
    val sign = if (cents < 0) "-" else ""
    return "$sign${abs(cents / 100)},${abs(cents % 100).toString().padStart(2, '0')}"
}

/** Preview fallback only; runtime composition supplies the local-time date formatter. */
internal fun formatShopMenuDate(raw: String): String {
    val date = raw.substringBefore('T')
    val parts = date.split('-')
    return if (parts.size == 3 && parts[0].length == 4 && parts[1].length == 2 &&
        parts[2].length == 2 && parts.all { part -> part.all(Char::isDigit) }) {
        "${parts[2]}.${parts[1]}.${parts[0]}"
    } else date
}
