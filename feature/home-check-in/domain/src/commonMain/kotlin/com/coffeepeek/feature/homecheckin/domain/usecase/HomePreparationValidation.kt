@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.coffeepeek.feature.homecheckin.domain.usecase

import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationField.*
import kotlin.time.Clock
import kotlin.time.Instant

/** Collect every invalid field so a save attempt can show all corrections together. */
fun homePreparationFieldErrors(input: HomePreparationInput, editing: Boolean = false,
    now: Instant = Clock.System.now()): Map<HomePreparationField, HomePreparationIssue> = buildMap {
    dateError(input, now)?.let { put(Date, it) }
    if (input.brewMethodCode != null && !Regex("^[a-z0-9][a-z0-9_-]{0,99}$").matches(input.brewMethodCode))
        put(Method, HomePreparationIssue.InvalidMethod)
    val selectors = listOf(input.coffeeProductSlug, input.coffeeProductId, input.customBeanName, input.shelfBagId).count { !it.isNullOrBlank() }
    if (selectors > 1 || (!editing && !input.coffeeProductId.isNullOrBlank()))
        put(Bean, HomePreparationIssue.ConflictingBeanSelection)
    else if ((input.customBeanName?.trim()?.length ?: 0) > 250)
        put(Bean, HomePreparationIssue.BeanNameTooLong)
    else if ((input.coffeeProductSlug?.trim()?.length ?: 0) > 250)
        put(Bean, HomePreparationIssue.InvalidBean)
    input.doseGrams?.let { value ->
        val dose = value.toDoubleOrNull()
        val message = when {
            dose == null || !dose.isFinite() -> HomePreparationIssue.InvalidDose
            dose <= 0 -> HomePreparationIssue.DoseTooSmall
            dose > 10000 -> HomePreparationIssue.DoseTooLarge
            dose < 0.01 || !Regex("^\\d+(?:\\.\\d{1,2})?$").matches(value) -> HomePreparationIssue.DosePrecision
            else -> null
        }
        message?.let { put(Dose, it) }
    }
    input.brewDurationSeconds?.let {
        if (it < 1) put(Duration, HomePreparationIssue.DurationTooSmall)
        else if (it > 604800) put(Duration, HomePreparationIssue.DurationTooLarge)
    }
    if (input.tasteScore != null && input.tasteScore !in 1..5) put(Rating, HomePreparationIssue.InvalidRating)
    if ((input.text?.length ?: 0) > 1000) put(Note, HomePreparationIssue.NoteTooLong)
    if (input.drinkSlug == "other" && input.customDrinkName.orEmpty().trim().length !in 1..100)
        put(Drink, HomePreparationIssue.InvalidDrinkName)
    else if (input.drinkSlug != "other" && !input.customDrinkName.isNullOrBlank())
        put(Drink, HomePreparationIssue.InvalidDrink)
    else if ((input.drinkSlug?.trim()?.length ?: 0) > 50) put(Drink, HomePreparationIssue.InvalidDrink)
    when {
        editing && input.photos.isNotEmpty() ->
            put(Photos, HomePreparationIssue.PhotoEditUnsupported)
        input.photos.size > 5 -> put(Photos, HomePreparationIssue.TooManyPhotos)
        input.photos.any {
            it.fileName.isBlank() || it.bytes.isEmpty() || !it.contentType.startsWith("image/")
        } -> put(Photos, HomePreparationIssue.InvalidPhotos)
        input.photos.indices.any { index -> input.photos.drop(index + 1).any { next ->
            input.photos[index].bytes.contentEquals(next.bytes)
        } } -> put(Photos, HomePreparationIssue.DuplicatePhotos)
    }
}

private fun dateError(input: HomePreparationInput, now: Instant): HomePreparationIssue? {
    if (!Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,7})?(?:Z|[+-]\\d{2}:\\d{2})$").matches(input.preparedAt))
        return HomePreparationIssue.MissingDateOffset
    if (!validDate(input.preparedAt.substringBefore('T')) || input.preparedOn?.let(::validDate) == false)
        return HomePreparationIssue.InvalidDate
    val time = input.preparedAt.substringAfter('T')
    val hour = time.substring(0, 2).toInt()
    val minute = time.substring(3, 5).toInt()
    val second = time.substring(6, 8).toInt()
    if (hour !in 0..23 || minute !in 0..59 || second !in 0..59) return HomePreparationIssue.InvalidTime
    val offset = if (time.endsWith('Z')) 0 else {
        val signIndex = maxOf(time.lastIndexOf('+'), time.lastIndexOf('-'))
        val hours = time.substring(signIndex + 1, signIndex + 3).toInt()
        val minutes = time.substring(signIndex + 4, signIndex + 6).toInt()
        if (hours !in 0..14 || minutes !in 0..59 || hours == 14 && minutes != 0) return HomePreparationIssue.InvalidDateOffset
        (hours * 60 + minutes) * if (time[signIndex] == '+') 1 else -1
    }
    val utcDate = when {
        hour * 60 + minute - offset < 0 -> shiftDate(input.preparedAt.substringBefore('T'), -1)
        hour * 60 + minute - offset >= 1440 -> shiftDate(input.preparedAt.substringBefore('T'), 1)
        else -> input.preparedAt.substringBefore('T')
    }
    if (input.preparedOn != null && input.preparedOn !in setOf(utcDate, shiftDate(utcDate, -1), shiftDate(utcDate, 1)))
        return HomePreparationIssue.CivilDateMismatch
    val instant = runCatching { Instant.parse(input.preparedAt) }.getOrNull()
        ?: return HomePreparationIssue.InvalidDate
    if (instant.toEpochMilliseconds() > now.toEpochMilliseconds() + 5 * 60 * 1000)
        return HomePreparationIssue.FutureDate
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
