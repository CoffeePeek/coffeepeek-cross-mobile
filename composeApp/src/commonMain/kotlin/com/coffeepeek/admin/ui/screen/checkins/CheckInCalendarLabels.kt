package com.coffeepeek.admin.ui.screen.checkins

private val monthNames = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
)

private val monthNamesGenitive = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)

internal fun monthTitle(month: CalendarMonth) = "${monthNames[month.month - 1]} ${month.year}"

internal fun selectedDateTitle(date: String, includeYear: Boolean = true): String {
    val parts = date.split('-')
    val year = parts.getOrNull(0)?.toIntOrNull() ?: return date
    val month = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..12 } ?: return date
    val day = parts.getOrNull(2)?.toIntOrNull() ?: return date
    return "$day ${monthNamesGenitive[month - 1]}" + if (includeYear) " $year" else ""
}

internal fun visitCount(count: Int) = "$count ${plural(count, "чекин", "чекина", "чекинов")}"
internal fun shopCount(count: Int) = "$count ${plural(count, "кофейня", "кофейни", "кофеен")}"

private fun plural(value: Int, one: String, few: String, many: String): String {
    val mod100 = value % 100
    if (mod100 in 11..14) return many
    return when (value % 10) {
        1 -> one
        2, 3, 4 -> few
        else -> many
    }
}
