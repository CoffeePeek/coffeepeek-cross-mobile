package com.coffeepeek.admin.di

import com.coffeepeek.admin.di.checkinhistory.FeatureHomeCheckInDetailRenderer
import com.coffeepeek.admin.di.homecheckin.FeatureCheckInCreateFlowRenderer
import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateFlowRenderer
import com.coffeepeek.admin.ui.screen.checkins.HomeCheckInDetailRenderer
import org.koin.core.scope.Scope
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.admin.di.discovery.FeatureCheckInShopPickerRenderer
import com.coffeepeek.admin.ui.screen.checkins.CheckInShopPickerRenderer

/** App-owned lazy factories for check-in destinations; resolving one does not construct the others. */
class CheckInRendererFactories(
    val homeDetail: Scope.() -> HomeCheckInDetailRenderer,
    val create: Scope.() -> CheckInCreateFlowRenderer,
    val shopPicker: Scope.() -> CheckInShopPickerRenderer,
)

internal fun featureCheckInRendererFactories() = CheckInRendererFactories(
    homeDetail = { get<FeatureHomeCheckInDetailRenderer>() },
    create = { get<FeatureCheckInCreateFlowRenderer>() },
    shopPicker = {
        FeatureCheckInShopPickerRenderer(get<ApiClients>().client, get(), get(), get(), get(), get(), get(), get(), get())
    },
)
