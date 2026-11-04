package com.coffeepeek.admin.di.shelf

import com.coffeepeek.feature.shelf.api.ShelfTimeZone
import java.time.Instant
import java.time.ZoneId

/** Read on every call, so a time-zone change while the app runs is picked up. */
internal val AndroidShelfTimeZone = ShelfTimeZone { epochSeconds ->
    ZoneId.systemDefault().rules.getOffset(Instant.ofEpochSecond(epochSeconds)).totalSeconds
}
