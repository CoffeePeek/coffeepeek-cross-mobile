package com.coffeepeek.admin.di

import com.coffeepeek.admin.di.checkinhistory.FeatureCheckInHistoryScreenRenderer
import com.coffeepeek.admin.di.homecheckin.FeatureCheckInCreateFlowRenderer
import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateFlowRenderer
import com.coffeepeek.admin.ui.screen.checkins.CheckInHistoryScreenRenderer
import com.coffeepeek.admin.ui.screen.checkins.LegacyCheckInCreateFlowRenderer
import com.coffeepeek.admin.ui.screen.checkins.LegacyCheckInHistoryScreenRenderer
import org.koin.core.scope.Scope

/** App-owned, lazy factories for the personal check-in destinations; resolving one does not construct the other. */
class CheckInRendererFactories(
    val history: Scope.() -> CheckInHistoryScreenRenderer = { LegacyCheckInHistoryScreenRenderer },
    val create: Scope.() -> CheckInCreateFlowRenderer = { LegacyCheckInCreateFlowRenderer },
)

internal fun featureCheckInRendererFactories() = CheckInRendererFactories(
    history = { get<FeatureCheckInHistoryScreenRenderer>() },
    create = { get<FeatureCheckInCreateFlowRenderer>() },
)
