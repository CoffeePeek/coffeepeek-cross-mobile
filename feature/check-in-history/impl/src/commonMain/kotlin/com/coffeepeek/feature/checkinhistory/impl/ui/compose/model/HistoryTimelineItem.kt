package com.coffeepeek.feature.checkinhistory.impl.ui.compose.model

import com.coffeepeek.core.datetime.parseServerInstant
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryEntry
import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn

/**
 * Newest first: by the calendar day, then by the instant. Timestamps are compared as instants, not as text, because offsets and fractions change the
 * text order (12:00+03:00 is 09:00Z, earlier than 10:00Z, yet it sorts after it as text); one that cannot be read goes last, and the text breaks a tie.
 */
internal fun compareHistoryMoments(dateA: String, stampA: String, dateB: String, stampB: String): Int {
    dateB.compareTo(dateA).takeIf { it != 0 }?.let { return it }
    val a = parseServerInstant(stampA)
    val b = parseServerInstant(stampB)
    return when {
        a != null && b != null -> b.compareTo(a).takeIf { it != 0 } ?: stampB.compareTo(stampA)
        a != null -> -1
        b != null -> 1
        else -> stampB.compareTo(stampA)
    }
}

internal val HistoryTimelineOrder: Comparator<HistoryTimelineItem> = Comparator { a, b -> compareHistoryMoments(a.date, a.timestamp, b.date, b.timestamp) }

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
