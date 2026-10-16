package com.coffeepeek.feature.checkinhistory.impl.ui.compose.model

import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryEntry
import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn

/** Stable source-prefixed identity; equal IDs from different sources are not duplicates. */
internal sealed interface HistoryTimelineItem {
    val key: String
    val date: String
    val timestamp: String

    data class Shop(val entry: PersonalCheckIn, override val date: String) : HistoryTimelineItem {
        override val key: String get() = "shop:${entry.id}"
        override val timestamp: String get() = entry.effectiveVisitedAt
    }

    data class Home(val entry: HomeHistoryEntry) : HistoryTimelineItem {
        override val key: String get() = "home:${entry.id}"
        override val date: String get() = entry.preparedOn
        override val timestamp: String get() = entry.preparedAtUtc
    }
}
