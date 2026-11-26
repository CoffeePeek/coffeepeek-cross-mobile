package com.coffeepeek.feature.profile.impl.ui.data

import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileState
import kotlinx.datetime.TimeZone

internal fun PublicProfileState.filteredCheckIns(zone: TimeZone = PublicProfileTimeZone) = checkIns.filter { row ->
    checkInDate == null || publicProfileDate(row.occurredAt, zone) == checkInDate
}
