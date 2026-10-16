package com.coffeepeek.admin.ui.screen.checkins

import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.feature.homecheckin.domain.HomeCheckIn

internal data class CalendarPhoto(val url: String, val isCutout: Boolean)

/** Calendar photos belong to the whole month, independently of the selected timeline day. */
internal fun homeCalendarPhotos(entries: List<HomeCheckIn>): Map<String, List<CalendarPhoto>> =
    entries.groupBy(HomeCheckIn::preparedOn).mapValues { (_, checkIns) ->
        checkIns.mapNotNull { entry ->
            entry.drinkPhotoUrl?.takeIf(String::isNotBlank)?.let { CalendarPhoto(it, isCutout = true) }
                ?: entry.photos.firstOrNull { it.url.isNotBlank() }?.let { CalendarPhoto(it.url, isCutout = false) }
        }
    }

internal fun calendarPhotos(checkIns: List<CheckIn>, homePhotos: List<CalendarPhoto>): List<CalendarPhoto> =
    (checkIns.mapNotNull { diaryPhoto(it)?.let { (url, cutout) -> CalendarPhoto(url, cutout) } } + homePhotos)
        .sortedByDescending(CalendarPhoto::isCutout)
        .distinctBy(CalendarPhoto::url)
        .take(3)
