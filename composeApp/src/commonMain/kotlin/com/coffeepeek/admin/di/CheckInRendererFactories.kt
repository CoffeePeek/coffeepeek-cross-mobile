package com.coffeepeek.admin.di

import com.coffeepeek.admin.di.checkinhistory.FeatureCheckInHistoryScreenRenderer
import com.coffeepeek.admin.di.homecheckin.FeatureCheckInCreateFlowRenderer
import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateFlowRenderer
import com.coffeepeek.admin.ui.screen.checkins.CheckInHistoryScreenRenderer
import org.koin.core.scope.Scope
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.admin.di.discovery.FeatureCheckInShopPickerRenderer
import com.coffeepeek.admin.ui.screen.checkins.CheckInShopPickerRenderer

/** App-owned lazy factories for check-in destinations; resolving one does not construct the others. */
class CheckInRendererFactories(
    val history: Scope.() -> CheckInHistoryScreenRenderer,
    val create: Scope.() -> CheckInCreateFlowRenderer,
    val shopPicker: Scope.() -> CheckInShopPickerRenderer,
)

internal fun featureCheckInRendererFactories() = CheckInRendererFactories(
    history = { get<FeatureCheckInHistoryScreenRenderer>() },
    create = { get<FeatureCheckInCreateFlowRenderer>() },
    shopPicker = { FeatureCheckInShopPickerRenderer(get<CoffeePeekClient>().client, get(), get(), get(), get()) },
)
