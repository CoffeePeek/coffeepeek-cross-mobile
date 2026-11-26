@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.coffeepeek.admin.ui.component

import coffeepeek.composeapp.generated.resources.*
import org.jetbrains.compose.resources.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

internal suspend fun nativeReviewDateLabels(): ReviewDateLabels {
    val environment = getSystemResourceEnvironment()
    return ReviewDateLabels(getString(environment, Res.string.review_just_now), getString(environment, Res.string.review_ago),
        getStringArray(environment, Res.array.minute_forms), getStringArray(environment, Res.array.hour_forms),
        getStringArray(environment, Res.array.review_months))
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class NativeReviewDateFormatter(scope: CoroutineScope, languages: Flow<String>) {
    private var labels: ReviewDateLabels? = null
    val languages = languages.mapLatest {
        labels = nativeReviewDateLabels()
        it
    }.shareIn(scope, SharingStarted.Eagerly, replay = 1)
    fun format(raw: String) = formatReviewDisplayDate(raw, checkNotNull(labels))
}
