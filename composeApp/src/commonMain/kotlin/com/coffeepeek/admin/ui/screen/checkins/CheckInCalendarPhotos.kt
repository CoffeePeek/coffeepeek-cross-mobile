package com.coffeepeek.admin.ui.screen.checkins

import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.feature.homecheckin.domain.HomeCheckIn

internal data class CalendarPhoto(val url: String)

/** Calendar photos belong to the whole month, independently of the selected timeline day. */
internal fun homeCalendarPhotos(entries: List<HomeCheckIn>): Map<String, List<CalendarPhoto>> =
    entries.groupBy(HomeCheckIn::preparedOn).mapValues { (_, checkIns) ->
        checkIns.mapNotNull { entry ->
            entry.photos.firstOrNull { it.calendarUrl.isNotBlank() }?.let { CalendarPhoto(it.calendarUrl) }
        }
    }

internal fun calendarPhotos(checkIns: List<CheckIn>, homePhotos: List<CalendarPhoto>): List<CalendarPhoto> =
    (checkIns.mapNotNull { diaryPhoto(it)?.let(::CalendarPhoto) } + homePhotos)
        .distinctBy(CalendarPhoto::url)
