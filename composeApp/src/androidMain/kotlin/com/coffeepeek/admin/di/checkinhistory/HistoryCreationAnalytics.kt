package com.coffeepeek.admin.di.checkinhistory

import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsEvent
import com.coffeepeek.admin.analytics.CheckInKind
import com.coffeepeek.admin.analytics.CheckInSource
import com.coffeepeek.admin.analytics.CheckInStep
import com.coffeepeek.admin.analytics.CheckInTracker

/** App-owned handoff of the existing flow. No history records, IDs or text enter analytics. */
internal class HistoryCreationAnalytics(private val isCurrent: () -> Boolean) {
    private var picker: CheckInTracker? = null

    fun opened() {
        if (!isCurrent() || picker != null) return
        Analytics.track(AnalyticsEvent.CHECK_IN_START_ATTEMPTED, "source" to "global_create")
        picker = CheckInTracker(CheckInKind.Undecided, CheckInSource.Global).also { it.step(CheckInStep.Type) }
    }

    fun home(): CheckInTracker? = select(CheckInKind.Home)
    fun shop(): CheckInTracker? = select(CheckInKind.Visit)

    private fun select(kind: CheckInKind): CheckInTracker? {
        val tracker = picker
        picker = null
        return tracker?.takeIf { isCurrent() }?.also { it.kind(kind) }
    }

    fun cancelled() {
        val tracker = picker
        picker = null
        if (isCurrent()) tracker?.cancel("close")
    }
}
