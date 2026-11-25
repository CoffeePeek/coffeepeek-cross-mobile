package com.coffeepeek.admin.analytics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** Native lifecycle/geometry inputs use the existing consent, visit and exposure ledger. */
internal class NativeScreenAnalytics(private val scope: CoroutineScope, private val page: String?,
    private val visible: Flow<Boolean>, private val service: AnalyticsService? = Analytics.service) {
    fun feature(feature: String, placement: String, measured: Flow<Boolean>) {
        val analytics = service ?: return
        scope.launch {
            eligibility(analytics, measured).collectLatest { visit ->
                if (visit == null) return@collectLatest
                delay(500)
                reportNativeFeatureExposure(visit.key, feature, placement) {
                    analytics.track(AnalyticsEvent.FEATURE_EXPOSED, mapOf("feature" to feature, "placement" to placement))
                }
            }
        }
    }

    fun entity(entity: Entity, loaded: Flow<Boolean>) {
        val analytics = service ?: return
        val entry = DetailEntryMemory.consume(entity)
        scope.launch {
            var sent: String? = null
            eligibility(analytics, loaded).collect { visit ->
                if (visit != null && visit.key != sent && analytics.track(AnalyticsEvent.ENTITY_DETAIL_VIEWED, entry)) sent = visit.key
            }
        }
    }

    fun feedViewed(firstPage: Flow<Pair<Int?, Boolean>>) {
        val analytics = service ?: return
        scope.launch {
            var sent: String? = null
            combine(analytics.state, visible, firstPage) { state, shown, page -> Triple(state, shown, page) }
                .collect { (state, shown, page) ->
                    val count = page.first
                    val key = "${state.generation}:${state.visit}:${state.visitSequence}"
                    if (shown && page.second && state.ready && state.foreground && state.page == "feed" && count != null && sent != key &&
                        analytics.track(AnalyticsEvent.FEED_VIEWED, mapOf("result_count" to count, "is_empty" to (count == 0)))) sent = key
                }
        }
    }

    private fun eligibility(analytics: AnalyticsService, measured: Flow<Boolean>) =
        combine(analytics.state, visible, measured) { state, shown, enough ->
            if (shown && enough && state.ready && state.foreground && (page == null || state.page == page) && state.overlays.isEmpty())
                NativeAnalyticsVisit("${state.generation}:${state.visit}:${state.visitSequence}") else null
        }.distinctUntilChanged()
}

private data class NativeAnalyticsVisit(val key: String)
