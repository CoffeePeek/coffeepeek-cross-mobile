package com.coffeepeek.admin.di

import com.coffeepeek.admin.di.checkinhistory.FeatureCheckInHistoryScreenRenderer
import com.coffeepeek.admin.di.homecheckin.FeatureCheckInCreateFlowRenderer
import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateFlowRenderer
import com.coffeepeek.admin.ui.screen.checkins.CheckInHistoryScreenRenderer
import com.coffeepeek.admin.ui.screen.checkins.LegacyCheckInCreateFlowRenderer
import com.coffeepeek.admin.ui.screen.checkins.LegacyCheckInHistoryScreenRenderer
import org.koin.core.scope.Scope
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.admin.di.discovery.FeatureCheckInShopPickerRenderer
import com.coffeepeek.admin.ui.screen.checkins.CheckInShopPickerRenderer
import com.coffeepeek.admin.ui.screen.checkins.LegacyCheckInShopPickerRenderer

/** App-owned lazy factories for check-in destinations; resolving one does not construct the others. */
class CheckInRendererFactories(
    val history: Scope.() -> CheckInHistoryScreenRenderer = { LegacyCheckInHistoryScreenRenderer },
    val create: Scope.() -> CheckInCreateFlowRenderer = { LegacyCheckInCreateFlowRenderer },
    val shopPicker: Scope.() -> CheckInShopPickerRenderer = { LegacyCheckInShopPickerRenderer },
)

internal fun featureCheckInRendererFactories() = CheckInRendererFactories(
    history = { get<FeatureCheckInHistoryScreenRenderer>() },
    create = { get<FeatureCheckInCreateFlowRenderer>() },
    shopPicker = { FeatureCheckInShopPickerRenderer(get<CoffeePeekClient>().client, get(), get(), get(), get()) },
)
