package com.coffeepeek.feature.shopreport.impl.ui.compose.model

internal sealed interface ShopReportEvent {
    data object Back : ShopReportEvent
}
