package com.coffeepeek.admin.map

private val hexZoneColor = Regex("^#?([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$")

internal fun zoneColorForMap(color: String?, isDarkTheme: Boolean): String {
    val fallback = if (isDarkTheme) "#D4A84B" else "#FACC15"
    val value = color?.trim()?.takeIf(hexZoneColor::matches) ?: return fallback
    val digits = value.removePrefix("#")
    return if (digits.length == 3) {
        "#${digits[0]}${digits[0]}${digits[1]}${digits[1]}${digits[2]}${digits[2]}".uppercase()
    } else {
        "#$digits".uppercase()
    }
}
