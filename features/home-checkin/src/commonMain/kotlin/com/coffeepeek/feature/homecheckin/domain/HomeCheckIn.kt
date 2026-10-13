package com.coffeepeek.feature.homecheckin.domain

/** One preparation, including a batch that produces several servings. */
data class HomeCheckIn(
    val id: String,
    val preparedAtUtc: String,
    val preparedOn: String,
    val createdAtUtc: String,
    val text: String?,
    val brewMethodCode: String?,
    val coffeeProductId: String?,
    val beanName: String?,
    val doseGrams: String?,
    val brewDurationSeconds: Int?,
    val tasteScore: Int?,
    val drinkName: String?,
    val drinkSlug: String?,
    val customDrinkName: String?,
    val photos: List<HomeCheckInPhoto>,
    val drinkPhotoUrl: String?,
)

data class HomeCheckInPhoto(val id: String, val url: String)

data class HomePhotoUpload(val fileName: String, val contentType: String, val bytes: ByteArray)

data class HomeCheckInInput(
    val preparedAt: String,
    val preparedOn: String? = null,
    val brewMethodCode: String? = null,
    val coffeeProductSlug: String? = null,
    /** Only valid on an edit of the same entry; preserves its historical bean name. */
    val coffeeProductId: String? = null,
    val customBeanName: String? = null,
    val doseGrams: String? = null,
    val brewDurationSeconds: Int? = null,
    val tasteScore: Int? = null,
    val text: String? = null,
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
    val photos: List<HomePhotoUpload> = emptyList(),
    val drinkPhoto: HomePhotoUpload? = null,
)

data class HomeCheckInPage(val items: List<HomeCheckIn>, val totalCount: Int, val page: Int, val pageSize: Int) {
    val hasMore: Boolean get() = page.toLong() * pageSize < totalCount
}

data class HomeCalendarDay(val preparedOn: String, val brewCount: Int)

data class HomeBeanStats(
    val coffeeProductId: String?, val beanName: String?, val brewCount: Int,
    val dosedBrewCount: Int, val totalKnownDoseGrams: String,
    val ratedBrewCount: Int, val averageTasteScore: Double?,
)

data class HomeMethodStats(
    val brewMethodCode: String?, val brewCount: Int, val dosedBrewCount: Int,
    val totalKnownDoseGrams: String, val ratedBrewCount: Int, val averageTasteScore: Double?,
)

interface HomeCheckInRepository {
    suspend fun create(input: HomeCheckInInput): Result<HomeCheckIn>
    suspend fun history(page: Int = 1, pageSize: Int = 20, from: String? = null, to: String? = null): Result<HomeCheckInPage>
    suspend fun calendar(from: String, to: String): Result<List<HomeCalendarDay>>
    suspend fun detail(id: String): Result<HomeCheckIn>
    suspend fun update(id: String, input: HomeCheckInInput): Result<HomeCheckIn>
    suspend fun delete(id: String): Result<Unit>
    suspend fun beanStats(): Result<List<HomeBeanStats>>
    suspend fun methodStats(): Result<List<HomeMethodStats>>
    /** Downloads protected bytes through the same authenticated client as other owner routes. */
    suspend fun photo(url: String): Result<ByteArray>
}

val homeBrewMethods = listOf("espresso", "pour_over", "batch_drip", "moka_pot", "cezve", "aeropress", "french_press", "other")

fun validateHomeCheckIn(input: HomeCheckInInput, editing: Boolean = false): String? {
    if (!Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})$").matches(input.preparedAt))
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
        if (hours !in 0..23 || minutes !in 0..59) return "Укажите корректный часовой пояс"
        (hours * 60 + minutes) * if (time[signIndex] == '+') 1 else -1
    }
    val utcDate = when {
        hour * 60 + minute - offset < 0 -> shiftDate(input.preparedAt.substringBefore('T'), -1)
        hour * 60 + minute - offset >= 1440 -> shiftDate(input.preparedAt.substringBefore('T'), 1)
        else -> input.preparedAt.substringBefore('T')
    }
    if (input.preparedOn != null && input.preparedOn !in setOf(utcDate, shiftDate(utcDate, -1), shiftDate(utcDate, 1)))
        return "Дата в календаре должна быть рядом с датой приготовления"
    if (input.brewMethodCode != null && input.brewMethodCode !in homeBrewMethods)
        return "Неизвестный способ приготовления"
    val beanSelectors = listOf(input.coffeeProductSlug, input.coffeeProductId, input.customBeanName).count { !it.isNullOrBlank() }
    if (beanSelectors > 1 || (!editing && !input.coffeeProductId.isNullOrBlank()))
        return "Выберите один источник зерна"
    if ((input.customBeanName?.trim()?.length ?: 0) > 250) return "Название зерна слишком длинное"
    if ((input.text?.length ?: 0) > 1000) return "Заметка должна быть не длиннее 1000 символов"
    if (input.doseGrams != null) {
        val dose = input.doseGrams.toDoubleOrNull()
        if (!Regex("^\\d+(?:\\.\\d{1,2})?$").matches(input.doseGrams) || dose == null || dose !in 0.01..10000.0)
            return "Доза должна быть от 0,01 до 10000 г, не более двух знаков после запятой"
    }
    if (input.brewDurationSeconds != null && input.brewDurationSeconds !in 1..604800)
        return "Время приготовления должно быть от 1 до 604800 секунд"
    if (input.tasteScore != null && input.tasteScore !in 1..5) return "Оценка должна быть от 1 до 5"
    if (!input.drinkSlug.isNullOrBlank() && !input.customDrinkName.isNullOrBlank())
        return "Выберите напиток или укажите своё название"
    if (input.photos.size > 5) return "Можно добавить до пяти фотографий"
    if (input.photos.indices.any { index -> input.photos.drop(index + 1).any { next ->
            input.photos[index].bytes.contentEquals(next.bytes)
        } }) return "Добавьте разные фотографии"
    if (editing && (input.photos.isNotEmpty() || input.drinkPhoto != null))
        return "Фотографии нельзя изменить при редактировании"
    if (input.drinkPhoto != null && (input.photos.isEmpty() || input.drinkPhoto.contentType != "image/png"))
        return "Фото напитка должно быть PNG и требует основное фото"
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
