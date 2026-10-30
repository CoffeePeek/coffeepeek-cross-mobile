package com.coffeepeek.admin.ui.screen.checkins

import com.coffeepeek.domain.model.CheckIn

internal data class CalendarPhoto(val url: String)

internal fun calendarPhotos(checkIns: List<CheckIn>, homePhotos: List<CalendarPhoto>): List<CalendarPhoto> =
    (checkIns.mapNotNull { diaryPhoto(it)?.let(::CalendarPhoto) } + homePhotos)
        .distinctBy(CalendarPhoto::url)
