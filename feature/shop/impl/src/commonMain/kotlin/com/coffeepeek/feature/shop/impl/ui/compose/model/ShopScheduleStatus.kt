package com.coffeepeek.feature.shop.impl.ui.compose.model

internal sealed interface ShopScheduleStatus {
    data class Open(val closingTime: String?) : ShopScheduleStatus
    data class Opens(val dayOffset: Int, val dayOfWeek: Int, val time: String) : ShopScheduleStatus
    data object TodayClosed : ShopScheduleStatus
    data object Closed : ShopScheduleStatus
}
