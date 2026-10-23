@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.coffeepeek.legacy.homecheckin.domain

import com.coffeepeek.legacy.homecheckin.domain.HomeCheckInField.*
import kotlin.time.Clock
import kotlin.time.Instant

fun validateHomeCheckIn(input: HomeCheckInInput, editing: Boolean = false): String? =
    homeCheckInFieldErrors(input, editing).values.firstOrNull()

/** Collect every invalid field so a save attempt can show all corrections together. */
fun homeCheckInFieldErrors(input: HomeCheckInInput, editing: Boolean = false): Map<HomeCheckInField, String> = buildMap {
    dateError(input)?.let { put(Date, it) }
    if (input.brewMethodCode != null && !Regex("^[a-z0-9][a-z0-9_-]{0,99}$").matches(input.brewMethodCode))
        put(Method, "Выберите способ заваривания из списка")
    val selectors = listOf(input.coffeeProductSlug, input.coffeeProductId, input.customBeanName).count { !it.isNullOrBlank() }
    if (selectors > 1 || (!editing && !input.coffeeProductId.isNullOrBlank()))
        put(Bean, "Выберите зерно из каталога или укажите своё название")
    else if ((input.customBeanName?.trim()?.length ?: 0) > 250)
        put(Bean, "Название зерна должно быть не длиннее 250 символов")
    else if ((input.coffeeProductSlug?.trim()?.length ?: 0) > 250)
        put(Bean, "Выберите зерно из каталога заново")
    input.doseGrams?.let { value ->
        val dose = value.toDoubleOrNull()
        val message = when {
            dose == null || !dose.isFinite() -> "Введите количество кофе числом"
            dose <= 0 -> "Количество кофе должно быть больше 0"
            dose > 10000 -> "Количество кофе не должно превышать 10 000 г"
            dose < 0.01 || !Regex("^\\d+(?:\\.\\d{1,2})?$").matches(value) -> "Укажите не более двух знаков после запятой"
            else -> null
        }
        message?.let { put(Dose, it) }
    }
    input.brewDurationSeconds?.let {
        if (it < 1) put(Duration, "Время заваривания должно быть больше 0")
        else if (it > 604800) put(Duration, "Время заваривания не должно превышать 10 080 минут")
    }
    if (input.tasteScore != null && input.tasteScore !in 1..5) put(Rating, "Выберите оценку от 1 до 5")
    if ((input.text?.length ?: 0) > 1000) put(Note, "Заметка должна быть не длиннее 1000 символов")
    if (input.drinkSlug == "other" && input.customDrinkName.orEmpty().trim().length !in 1..100)
        put(Drink, "Укажите название напитка: от 1 до 100 символов")
    else if (input.drinkSlug != "other" && !input.customDrinkName.isNullOrBlank())
        put(Drink, "Укажите название напитка заново")
    else if ((input.drinkSlug?.trim()?.length ?: 0) > 50) put(Drink, "Укажите напиток заново")
    when {
        editing && input.photos.isNotEmpty() ->
            put(Photos, "Фотографии нельзя изменить при редактировании")
        input.photos.size > 5 -> put(Photos, "Можно добавить до пяти фотографий")
        input.photos.indices.any { index -> input.photos.drop(index + 1).any { next ->
            input.photos[index].bytes.contentEquals(next.bytes)
        } } -> put(Photos, "Добавьте разные фотографии")
    }
}

private fun dateError(input: HomeCheckInInput): String? {
    if (!Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,7})?(?:Z|[+-]\\d{2}:\\d{2})$").matches(input.preparedAt))
        return "Укажите дату и время с часовым поясом"
    if (!validDate(input.preparedAt.substringBefore('T')) || input.preparedOn?.let(::validDate) == false)
        return "Укажите корректную дату приготовления"
    val time = input.preparedAt.substringAfter('T')
    val hour = time.substring(0, 2).toInt()
    val minute = time.substring(3, 5).toInt()
    val second = time.substring(6, 8).toInt()
    if (hour !in 0..23 || minute !in 0..59 || second !in 0..59) return "Укажите корректное время приготовления"
    val offset = if (time.endsWith('Z')) 0 else {
        val signIndex = maxOf(time.lastIndexOf('+'), time.lastIndexOf('-'))
        val hours = time.substring(signIndex + 1, signIndex + 3).toInt()
        val minutes = time.substring(signIndex + 4, signIndex + 6).toInt()
        if (hours !in 0..14 || minutes !in 0..59 || hours == 14 && minutes != 0) return "Укажите корректный часовой пояс"
        (hours * 60 + minutes) * if (time[signIndex] == '+') 1 else -1
    }
    val utcDate = when {
        hour * 60 + minute - offset < 0 -> shiftDate(input.preparedAt.substringBefore('T'), -1)
        hour * 60 + minute - offset >= 1440 -> shiftDate(input.preparedAt.substringBefore('T'), 1)
        else -> input.preparedAt.substringBefore('T')
    }
    if (input.preparedOn != null && input.preparedOn !in setOf(utcDate, shiftDate(utcDate, -1), shiftDate(utcDate, 1)))
        return "Дата в календаре должна быть рядом с датой приготовления"
    val instant = runCatching { Instant.parse(input.preparedAt) }.getOrNull()
        ?: return "Укажите корректную дату и время приготовления"
    if (instant.toEpochMilliseconds() > Clock.System.now().toEpochMilliseconds() + 5 * 60 * 1000)
        return "Дата и время приготовления не могут быть в будущем"
    return null
}

private fun validDate(value: String): Boolean {
    if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(value)) return false
    val year = value.substring(0, 4).toInt()
    val month = value.substring(5, 7).toInt()
    val day = value.substring(8, 10).toInt()
    if (year == 0 || month !in 1..12) return false
    return day in 1..monthLength(year, month)
}

private fun shiftDate(value: String, delta: Int): String {
    var year = value.substring(0, 4).toInt()
    var month = value.substring(5, 7).toInt()
    var day = value.substring(8, 10).toInt() + delta
    if (day == 0) {
        month--
        if (month == 0) { month = 12; year-- }
        day = monthLength(year, month)
    } else if (day > monthLength(year, month)) {
        day = 1
        month++
        if (month == 13) { month = 1; year++ }
    }
    return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

private fun monthLength(year: Int, month: Int): Int = when (month) {
    2 -> if (year % 400 == 0 || year % 4 == 0 && year % 100 != 0) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}
