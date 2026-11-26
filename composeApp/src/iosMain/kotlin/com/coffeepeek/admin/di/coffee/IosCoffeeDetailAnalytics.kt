package com.coffeepeek.admin.di.coffee

import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsEvent
import com.coffeepeek.admin.analytics.AnalyticsService
import com.coffeepeek.admin.analytics.AnalyticsState
import com.coffeepeek.admin.analytics.DetailEntryMemory
import com.coffeepeek.admin.analytics.Entity
import com.coffeepeek.admin.analytics.FeatureExposureLedger
import com.coffeepeek.feature.coffee.api.CoffeeExposure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Reuses the detail entry source and the app's view/exposure guards for the UIKit visit. */
internal class IosCoffeeDetailAnalytics(scope: CoroutineScope) {
    private val owner = MutableStateFlow<String?>(null)
    private val loaded = MutableStateFlow(false)
    private val fractions = MutableStateFlow<Map<CoffeeExposure, Float>>(emptyMap())
    private val exposureLedger = FeatureExposureLedger()

    init {
        Analytics.service?.let { service ->
            val entry = DetailEntryMemory.consume(Entity.Coffee)
            scope.launch { trackDetail(service, entry) }
            sections.forEach { section -> scope.launch { trackExposure(service, section) } }
        }
    }

    fun updateOwner(visit: String?) { owner.value = visit }

    fun updateLoaded(value: Boolean) {
        loaded.value = value
        if (!value) fractions.value = emptyMap()
    }

    fun updateFraction(name: String, fraction: Float) {
        val section = sections.firstOrNull { it.name == name } ?: return
        fractions.update { it + (section to fraction.coerceIn(0f, 1f)) }
    }

    private suspend fun trackDetail(service: AnalyticsService, entry: Map<String, Any>) {
        var sent: String? = null
        combine(service.state, owner, loaded) { state, visit, hasProduct ->
            coffeeDetailVisit(state, visit)?.takeIf { hasProduct }
        }.distinctUntilChanged().collect { eligibleVisit ->
            if (eligibleVisit != null && eligibleVisit != sent &&
                service.track(AnalyticsEvent.ENTITY_DETAIL_VIEWED, entry)) sent = eligibleVisit
        }
    }

    private suspend fun trackExposure(service: AnalyticsService, section: CoffeeExposure) {
        val (feature, placement) = section.tracking()
        combine(service.state, owner, fractions) { state, visit, measured ->
            coffeeDetailVisit(state, visit)?.takeIf {
                state.overlays.isEmpty() && measured.getOrElse(section) { 0f } >= .1f
            }
        }.distinctUntilChanged().collectLatest { visit ->
            if (visit == null) return@collectLatest
            delay(500)
            val state = service.state.value
            val fraction = fractions.value.getOrElse(section) { 0f }
            if (coffeeDetailVisit(state, owner.value) == visit) {
                exposureLedger.report(visit, feature, placement, fraction, state.foreground, state.overlays.isEmpty(), 500) {
                    service.track(AnalyticsEvent.FEATURE_EXPOSED, mapOf("feature" to feature, "placement" to placement))
                }
            }
        }
    }

    private companion object {
        val sections = listOf(CoffeeExposure.Photos, CoffeeExposure.Roaster, CoffeeExposure.Offers, CoffeeExposure.SimilarCoffees)
    }
}

internal fun coffeeDetailVisit(state: AnalyticsState, owner: String?): String? =
    owner?.takeIf { it == state.visit && state.page == "coffee_detail" && state.ready && state.foreground }
        ?.let { "${state.generation}:${state.visit}:${state.visitSequence}" }
