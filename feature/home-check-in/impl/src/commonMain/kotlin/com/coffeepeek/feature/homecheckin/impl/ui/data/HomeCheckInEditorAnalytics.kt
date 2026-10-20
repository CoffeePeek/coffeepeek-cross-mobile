package com.coffeepeek.feature.homecheckin.impl.ui.data

import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationField
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationIssue
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationVisibility

/** Composition adapts the existing consent-aware flow; no IDs, query, note, dates or bytes cross this port. */
fun interface HomeCheckInEditorAnalytics {
    fun record(event: HomeCheckInEditorMetric)

    companion object { val None = HomeCheckInEditorAnalytics {} }
}

enum class HomeCheckInEditorStage { Bean, Photo, Save }

sealed interface HomeCheckInEditorMetric {
    data object Opened : HomeCheckInEditorMetric
    data object BeanPickerOpened : HomeCheckInEditorMetric
    data class FieldChanged(val field: HomePreparationField) : HomeCheckInEditorMetric
    data object Submitted : HomeCheckInEditorMetric
    data class Validation(val fields: Set<HomePreparationField>, val server: Boolean) : HomeCheckInEditorMetric
    data class Failed(val stage: HomeCheckInEditorStage, val issue: HomePreparationIssue, val statusCode: Int? = null) : HomeCheckInEditorMetric
    data class Retry(val stage: HomeCheckInEditorStage) : HomeCheckInEditorMetric
    data class Saved(val visibility: HomePreparationVisibility?, val hasPhotos: Boolean) : HomeCheckInEditorMetric
    data object Cancelled : HomeCheckInEditorMetric
}
