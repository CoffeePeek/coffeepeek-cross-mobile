package com.coffeepeek.admin.di.homecheckin

import com.coffeepeek.admin.analytics.*
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationField
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationIssue
import com.coffeepeek.feature.homecheckin.impl.ui.data.*

/** One existing consent-aware tracker/handoff per form, never a second global analytics service. */
internal class AndroidHomeCheckInAnalytics(
    private val isCurrent: () -> Boolean,
    private val editing: Boolean,
    private val source: CheckInSource,
    private val inherited: CheckInTracker? = null,
) : HomeCheckInEditorAnalytics {
    private var tracker: CheckInTracker? = null
    private var completed = false

    override fun record(event: HomeCheckInEditorMetric) {
        if (!isCurrent() || completed) return
        if (event == HomeCheckInEditorMetric.Opened) {
            if (tracker == null) tracker = (inherited ?: CheckInTracker(CheckInKind.Home, source, edit = editing))
                .also { it.step(CheckInStep.Editor) }
            return
        }
        val current = tracker ?: return
        when (event) {
            HomeCheckInEditorMetric.Opened -> Unit
            HomeCheckInEditorMetric.BeanPickerOpened -> current.step(CheckInStep.Bean)
            is HomeCheckInEditorMetric.FieldChanged -> current.field(event.field.analyticsName())
            HomeCheckInEditorMetric.Submitted -> current.submit()
            is HomeCheckInEditorMetric.Validation -> current.validation(event.fields.map { it.analyticsName() }, event.server)
            is HomeCheckInEditorMetric.Failed -> current.failed(event.stage.analyticsStage(), event.issue.analyticsError())
            is HomeCheckInEditorMetric.Retry -> current.retry(event.stage.analyticsStage())
            is HomeCheckInEditorMetric.Saved -> {
                current.saved(event.visibility?.name ?: "unknown", event.hasPhotos)
                completed = true
            }
            HomeCheckInEditorMetric.Cancelled -> { current.cancel("close"); completed = true }
        }
    }
}

private fun HomePreparationField.analyticsName() = when (this) {
    HomePreparationField.Date -> "date"; HomePreparationField.Method -> "brew_method"; HomePreparationField.Bean -> "bean"
    HomePreparationField.Dose -> "dose"; HomePreparationField.Duration -> "brew_duration"; HomePreparationField.Rating -> "rating_taste"
    HomePreparationField.Note -> "note"; HomePreparationField.Drink -> "drink"; HomePreparationField.Photos -> "photos"
    HomePreparationField.Visibility -> "visibility"
}
private fun HomeCheckInEditorStage.analyticsStage() = when (this) {
    HomeCheckInEditorStage.Bean -> CheckInStage.Bean
    HomeCheckInEditorStage.Photo -> CheckInStage.Photo
    HomeCheckInEditorStage.Save -> CheckInStage.Save
}
private fun HomePreparationIssue.analyticsError() = SafeAnalyticsError(when (this) {
    HomePreparationIssue.Unauthorized -> "authentication"
    HomePreparationIssue.Forbidden -> "permission"
    HomePreparationIssue.NotFound -> "not_found"
    HomePreparationIssue.RateLimited -> "rate_limited"
    HomePreparationIssue.Unavailable -> "server"
    HomePreparationIssue.Offline, HomePreparationIssue.Timeout -> "network"
    HomePreparationIssue.InvalidResponse, HomePreparationIssue.RequestFailed, HomePreparationIssue.Conflict -> "unknown"
    else -> "validation"
})
